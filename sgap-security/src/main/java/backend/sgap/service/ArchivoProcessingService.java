package backend.sgap.service;

import backend.sgap.dto.AnalisisResultado;
import backend.sgap.entity.Archivo;
import backend.sgap.entity.Evento;
import backend.sgap.enums.EstadoArchivo;
import backend.sgap.enums.NivelRiesgo;
import backend.sgap.enums.SecurityFlag;
import backend.sgap.repository.ArchivoRepository;
import backend.sgap.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Worker asíncrono del pipeline de seguridad de SGAP.
 *
 * <p>Este servicio ejecuta el análisis completo de un archivo en segundo plano,
 * después de la demora configurada. El método {@link #procesarAsync} corre en el
 * thread pool {@code fileProcessingExecutor} definido en {@code AsyncConfig}.
 *
 * <h3>Pasos del pipeline</h3>
 * <pre>
 *   [1] Actualizar estado → PROCESSING
 *   [2] TikaService       → detectar MIME real
 *   [3] SecurityAnalysis  → generar AnalisisResultado
 *   [4] Calcular SHA-256
 *   [5] Routing MinIO     → archivos/ | quarantine/ | blocked/
 *   [6] Persistir metadatos
 *   [7] Registrar evento de auditoría
 *   [8] Cleanup archivo temporal
 * </pre>
 *
 * <p>El bloque {@code finally} garantiza la limpieza del archivo temporal
 * incluso ante errores en cualquier paso intermedio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchivoProcessingService {

    private final ArchivoRepository archivoRepository;
    private final EventoRepository eventoRepository;
    private final StorageService storageService;
    private final QuarantineStorageService quarantineStorageService;
    private final TikaService tikaService;
    private final SecurityAnalysisService securityAnalysisService;

    // ── API pública ────────────────────────────────────────────────────────

    /**
     * Ejecuta el pipeline de seguridad completo de forma asíncrona.
     *
     * <p>Este método es invocado por {@link DelayedProcessingService} tras la
     * demora configurada y corre en el pool {@code fileProcessingExecutor}.
     *
     * @param archivoId      ID del registro {@link Archivo} en base de datos (estado PENDING)
     * @param quarantinePath ruta del archivo en cuarentena local
     */
    @Async("fileProcessingExecutor")
    @Transactional
    public void procesarAsync(Long archivoId, Path quarantinePath) {
        log.info("[ASYNC-PIPELINE] Iniciando procesamiento | archivoId={} path='{}'",
                archivoId, quarantinePath);

        Archivo archivo = null;

        try {
            // ── Paso 1: Cargar entidad y transicionar a PROCESSING ─────────
            archivo = archivoRepository.findById(archivoId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Archivo no encontrado en BD: id=" + archivoId));

            archivo.setEstado(EstadoArchivo.PROCESSING);
            archivoRepository.save(archivo);
            log.debug("[ASYNC-PIPELINE] Estado actualizado a PROCESSING | archivoId={}", archivoId);

            // ── Paso 2: Detección MIME real con Tika ──────────────────────
            String mimeDetectado = tikaService.detectMime(quarantinePath);
            log.debug("[ASYNC-PIPELINE] MIME detectado='{}' | archivoId={}", mimeDetectado, archivoId);

            // ── Paso 3: Motor de análisis de seguridad ────────────────────
            AnalisisResultado resultado = securityAnalysisService.analyze(
                    archivo.getNombre(), mimeDetectado);

            log.info("[ASYNC-PIPELINE] Risk assessment | archivoId={} risk={} flags={} mime='{}'",
                    archivoId,
                    resultado.getNivelRiesgo(),
                    resultado.getFlags(),
                    mimeDetectado);

            // ── Paso 4: Calcular hash SHA-256 ─────────────────────────────
            String hash = calcularHash(quarantinePath);

            // ── Paso 5: Routing y subida a MinIO ──────────────────────────
            RoutingResult routing = routeToStorage(
                    archivo.getNombre(), quarantinePath, mimeDetectado, resultado.getNivelRiesgo());

            // ── Paso 6: Persistir metadatos de seguridad ──────────────────
            archivo.setMimeDetectado(mimeDetectado);
            archivo.setExtensionDeclarada(resultado.getExtensionDeclarada());
            archivo.setHash(hash);
            archivo.setNivelRiesgo(resultado.getNivelRiesgo());
            archivo.setEstado(routing.estado());
            archivo.setFlagsSeguridad(flagsToString(resultado.getFlags()));
            archivo.setDescripcionAnalisis(resultado.getDescripcion());
            archivo.setRuta(routing.url());
            archivo.setRutaTemporal(null); // limpieza lógica

            archivoRepository.save(archivo);

            // ── Paso 7: Evento de auditoría ───────────────────────────────
            registrarEvento(
                    eventoTipo(routing.estado()),
                    resultado.getDescripcion() + " | Archivo: " + archivo.getNombre());

            log.info("[ASYNC-PIPELINE] Finalizado exitosamente | archivoId={} estado={} ruta='{}'",
                    archivoId, routing.estado(), routing.url());

        } catch (Exception e) {
            log.error("[ASYNC-PIPELINE] Error irrecuperable | archivoId={} error='{}'",
                    archivoId, e.getMessage(), e);
            marcarComoFallido(archivo, archivoId, e.getMessage());

        } finally {
            // ── Paso 8: Cleanup del archivo temporal (siempre) ────────────
            if (quarantinePath != null) {
                quarantineStorageService.deleteFromQuarantine(quarantinePath);
                log.debug("[ASYNC-PIPELINE] Archivo temporal eliminado | path='{}'", quarantinePath);
            }
        }
    }

    // ── Helpers privados ───────────────────────────────────────────────────

    /** Resultado del routing: object key en MinIO, URL de acceso y estado final del archivo. */
    private record RoutingResult(String objectKey, String url, EstadoArchivo estado) {}

    /**
     * Decide a qué prefijo MinIO va el archivo según el nivel de riesgo,
     * sube el contenido y devuelve el resultado del routing.
     */
    private RoutingResult routeToStorage(String filename, Path quarantinePath,
                                          String mimeDetectado, NivelRiesgo nivelRiesgo)
            throws Exception {

        String prefix;
        EstadoArchivo estado;

        switch (nivelRiesgo) {
            case LOW -> {
                prefix = "archivos";
                estado = EstadoArchivo.SAFE;
            }
            case MEDIUM -> {
                prefix = "quarantine";
                estado = EstadoArchivo.QUARANTINED;
                log.warn("[ASYNC-PIPELINE] Archivo en cuarentena MinIO (MEDIUM): '{}'", filename);
            }
            default -> { // HIGH
                prefix = "blocked";
                estado = EstadoArchivo.UNSAFE;
                log.error("[ASYNC-PIPELINE] Archivo BLOQUEADO (HIGH): '{}'", filename);
            }
        }

        String objectKey = storageService.buildObjectKey(prefix, filename);
        storageService.uploadFromPath(objectKey, quarantinePath, mimeDetectado);
        String url = storageService.getPresignedUrl(objectKey);

        return new RoutingResult(objectKey, url, estado);
    }

    /** Marca el archivo como FAILED si se produce una excepción en el pipeline. */
    private void marcarComoFallido(Archivo archivo, Long archivoId, String errorMsg) {
        try {
            Archivo target = (archivo != null)
                    ? archivo
                    : archivoRepository.findById(archivoId).orElse(null);

            if (target != null) {
                target.setEstado(EstadoArchivo.FAILED);
                target.setDescripcionAnalisis("Error en pipeline asíncrono: " + errorMsg);
                target.setRutaTemporal(null);
                archivoRepository.save(target);
                registrarEvento("UPLOAD_FAILED",
                        "Pipeline fallido para archivo id=" + archivoId + " | " + errorMsg);
            }
        } catch (Exception ex) {
            log.error("[ASYNC-PIPELINE] No se pudo persistir estado FAILED | archivoId={}: {}",
                    archivoId, ex.getMessage());
        }
    }

    /** Serializa el conjunto de flags a cadena separada por comas. */
    private String flagsToString(Set<SecurityFlag> flags) {
        if (flags == null || flags.isEmpty()) return null;
        return flags.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    /** Calcula el hash SHA-256 del contenido del archivo. */
    private String calcularHash(Path file) throws Exception {
        byte[] data = Files.readAllBytes(file);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /** Determina el tipo de evento de auditoría según el estado final. */
    private String eventoTipo(EstadoArchivo estado) {
        return switch (estado) {
            case SAFE        -> "UPLOAD_SAFE";
            case QUARANTINED -> "UPLOAD_QUARANTINED";
            case UNSAFE      -> "UPLOAD_BLOCKED";
            default          -> "UPLOAD_UNKNOWN";
        };
    }

    private void registrarEvento(String tipo, String descripcion) {
        Evento evento = new Evento();
        evento.setTipoEvento(tipo);
        evento.setDescripcion(descripcion);
        evento.setFecha(LocalDateTime.now());
        eventoRepository.save(evento);
    }
}
