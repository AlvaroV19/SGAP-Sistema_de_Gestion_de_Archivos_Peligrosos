package backend.sgap.service;

import backend.sgap.dto.UploadResponse;
import backend.sgap.entity.Archivo;
import backend.sgap.entity.Evento;
import backend.sgap.enums.EstadoArchivo;
import backend.sgap.repository.ArchivoRepository;
import backend.sgap.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de registro inmediato de archivos en SGAP.
 *
 * <p>Este servicio es responsable de la parte <em>síncrona</em> del pipeline:
 * recibir el archivo, almacenarlo temporalmente en cuarentena local,
 * persistir el registro en BD con estado {@link EstadoArchivo#PENDING}
 * y agendar el análisis asíncrono diferido.
 *
 * <h3>Flujo síncrono (en el hilo HTTP)</h3>
 * <pre>
 *   MultipartFile
 *     │
 *     ▼
 *   [1] QuarantineStorageService → archivo en /storage/quarantine/
 *     │
 *     ▼
 *   [2] Persistir metadatos básicos con estado PENDING
 *     │
 *     ▼
 *   [3] DelayedProcessingService.scheduleProcessing() → agenda worker asíncrono
 *     │
 *     ▼
 *   [4] Retorna UploadResponse (inmediato)
 * </pre>
 *
 * <p>El análisis de seguridad real (Tika, motor de reglas, MinIO, hash)
 * se ejecuta en {@link ArchivoProcessingService} tras la demora configurada.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchivoService {

    private final ArchivoRepository archivoRepository;
    private final EventoRepository eventoRepository;
    private final QuarantineStorageService quarantineStorageService;
    private final DelayedProcessingService delayedProcessingService;

    // ── API pública ────────────────────────────────────────────────────────

    /**
     * Registra el archivo en BD con estado {@code PENDING} y agenda el análisis
     * asíncrono. Responde de forma inmediata sin bloquear el hilo HTTP.
     *
     * @param file archivo recibido del cliente
     * @return {@link UploadResponse} con el ID del registro y estado PENDING
     * @throws Exception si el almacenamiento temporal o la persistencia inicial fallan
     */
    @Transactional
    public UploadResponse registrar(MultipartFile file) throws Exception {
        log.info("[SERVICE] Recibiendo archivo '{}' ({} bytes)",
                file.getOriginalFilename(), file.getSize());

        // ── Paso 1: Almacenar en cuarentena local ─────────────────────────
        Path quarantinePath = quarantineStorageService.storeInQuarantine(file);
        log.debug("[SERVICE] Archivo en cuarentena | path='{}'", quarantinePath);

        // ── Paso 2: Persistir registro inicial con estado PENDING ─────────
        LocalDateTime ahora = LocalDateTime.now();

        Archivo archivo = new Archivo();
        archivo.setNombre(file.getOriginalFilename());
        archivo.setTipoArchivo(file.getContentType()); // solo referencia, no confiable
        archivo.setTamano(file.getSize());
        archivo.setEstado(EstadoArchivo.PENDING);
        archivo.setFechaSubida(ahora);
        archivo.setRutaTemporal(quarantinePath.toAbsolutePath().toString());

        Archivo saved = archivoRepository.save(archivo);
        log.info("[SERVICE] Registro PENDING creado | id={} nombre='{}'",
                saved.getId(), saved.getNombre());

        registrarEvento("UPLOAD_RECEIVED",
                "Archivo recibido, pendiente de análisis: " + saved.getNombre());

        // ── Paso 3: Agendar análisis asíncrono diferido ───────────────────
        delayedProcessingService.scheduleProcessing(saved.getId(), quarantinePath);

        // ── Paso 4: Respuesta inmediata ───────────────────────────────────
        return UploadResponse.builder()
                .id(saved.getId())
                .nombre(saved.getNombre())
                .estado(EstadoArchivo.PENDING)
                .fechaSubida(ahora)
                .mensaje("Archivo recibido. El análisis de seguridad se ejecutará en segundo plano.")
                .build();
    }

    /**
     * Lista todos los archivos registrados en el sistema (todos los estados).
     */
    public List<Archivo> listar() {
        return archivoRepository.findAll();
    }

    // ── Helpers privados ───────────────────────────────────────────────────

    private void registrarEvento(String tipo, String descripcion) {
        Evento evento = new Evento();
        evento.setTipoEvento(tipo);
        evento.setDescripcion(descripcion);
        evento.setFecha(LocalDateTime.now());
        eventoRepository.save(evento);
    }

    /**
     * Busca un archivo por su ID.
     *
     * @param id identificador del archivo
     * @return Optional con el archivo si existe
     */
    public Optional<Archivo> buscarPorId(Long id) {
        return archivoRepository.findById(id);
    }

    /**
     * Elimina un archivo por ID. Solo accesible para ROLE_ADMIN.
     *
     * @param id identificador del archivo a eliminar
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("[SERVICE] Eliminando archivo id={}", id);
        archivoRepository.deleteById(id);
        registrarEvento("FILE_DELETED", "Archivo eliminado por administrador. id=" + id);
    }

}