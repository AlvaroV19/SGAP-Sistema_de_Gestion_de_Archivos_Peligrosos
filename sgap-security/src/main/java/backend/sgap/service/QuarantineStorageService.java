package backend.sgap.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Servicio de cuarentena local.
 *
 * <p>Todo archivo subido es almacenado primero en un directorio temporal de
 * cuarentena antes de ser analizado. Solo después del análisis se decide
 * si el archivo pasa al almacenamiento final (MinIO) o queda bloqueado.
 *
 * <p>Flujo:
 * <pre>
 *   Upload → storeInQuarantine() → análisis → (MinIO safe | MinIO blocked) → deleteFromQuarantine()
 * </pre>
 *
 * <p>La ruta de cuarentena se configura en {@code application.properties}:
 * <pre>
 *   storage.quarantine.path=./storage/quarantine
 * </pre>
 *
 * <p><strong>Seguridad:</strong> el nombre de archivo se sanitiza completamente
 * antes de escribir en disco. El nombre en disco es siempre {@code UUID_sanitized},
 * lo que evita path traversal y colisiones.
 */
@Service
@Slf4j
public class QuarantineStorageService {

    @Value("${storage.quarantine.path:./storage/quarantine}")
    private String quarantinePathConfig;

    private Path quarantineDir;

    @PostConstruct
    public void init() throws IOException {
        quarantineDir = Paths.get(quarantinePathConfig).toAbsolutePath().normalize();
        Files.createDirectories(quarantineDir);
        log.info("Quarantine directory ready: {}", quarantineDir);
    }

    /**
     * Escribe el archivo subido en el directorio de cuarentena y devuelve
     * la ruta resultante.
     *
     * <p>El nombre en disco es siempre {@code <UUID>_<nombre-sanitizado>} para
     * evitar colisiones y ataques de path traversal.
     *
     * @param file archivo recibido del cliente
     * @return ruta absoluta al archivo en cuarentena
     * @throws IOException si no se puede escribir el archivo
     */
    public Path storeInQuarantine(MultipartFile file) throws IOException {
        String safeFilename = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        Path destination = quarantineDir.resolve(safeFilename).normalize();

        // Doble verificación: la ruta resuelta debe estar dentro del directorio de cuarentena
        if (!destination.startsWith(quarantineDir)) {
            throw new SecurityException("Path traversal detectado en el nombre de archivo: "
                    + file.getOriginalFilename());
        }

        file.transferTo(destination);
        log.info("[QUARANTINE] Archivo almacenado en cuarentena: {} ({} bytes)",
                destination.getFileName(), file.getSize());
        return destination;
    }

    /**
     * Elimina el archivo de cuarentena una vez que el análisis y la copia al
     * almacenamiento final han concluido.
     *
     * <p>Esta operación se ejecuta en el bloque {@code finally} de
     * {@code ArchivoService} para garantizar la limpieza incluso ante errores.
     *
     * @param quarantinedFile ruta al archivo en cuarentena
     */
    public void deleteFromQuarantine(Path quarantinedFile) {
        try {
            boolean deleted = Files.deleteIfExists(quarantinedFile);
            if (deleted) {
                log.info("[QUARANTINE] Archivo eliminado de cuarentena: {}", quarantinedFile.getFileName());
            }
        } catch (IOException e) {
            // No lanzar excepción: la limpieza es best-effort.
            // Un job de mantenimiento puede limpiar el directorio periódicamente.
            log.warn("[QUARANTINE] No se pudo eliminar el archivo de cuarentena: {}. Error: {}",
                    quarantinedFile, e.getMessage());
        }
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    /**
     * Elimina cualquier carácter que no sea alfanumérico, punto, guión o guión bajo.
     * Protege contra path traversal, inyección de shell y nombres inválidos en disco.
     */
    private String sanitize(String originalName) {
        if (originalName == null || originalName.isBlank()) return "unknown";
        // Tomar solo el nombre de archivo (sin ruta)
        String baseName = Paths.get(originalName).getFileName().toString();
        return baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
