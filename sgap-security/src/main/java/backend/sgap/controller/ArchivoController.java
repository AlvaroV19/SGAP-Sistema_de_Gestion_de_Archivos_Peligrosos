package backend.sgap.controller;

import backend.sgap.config.CorsConfig;
import backend.sgap.dto.UploadResponse;
import backend.sgap.entity.Archivo;
import backend.sgap.service.ArchivoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de archivos en SGAP.
 *
 * <h3>Seguridad por rol</h3>
 * <ul>
 *   <li>POST   – USER, ANALYST, ADMIN pueden subir archivos.</li>
 *   <li>GET    – ANALYST y ADMIN pueden listar/ver archivos.</li>
 *   <li>DELETE – solo ADMIN puede eliminar archivos.</li>
 * </ul>
 *
 * <p>CORS gestionado globalmente por {@link CorsConfig}.
 * El {@code @CrossOrigin("*")} ha sido eliminado para usar la configuración central.
 *
 * <h3>Flujo asíncrono</h3>
 * <ul>
 *   <li>POST /api/archivos devuelve {@code 202 Accepted} inmediatamente con
 *       {@link UploadResponse} (id + estado PENDING). El análisis ocurre en segundo plano.</li>
 *   <li>El cliente puede consultar el estado final mediante GET /api/archivos/{id}
 *       o GET /api/archivos (lista completa).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
@Slf4j
public class ArchivoController {

    private final ArchivoService service;

    /**
     * Sube un archivo al sistema SGAP.
     * Roles permitidos: ROLE_USER, ROLE_ANALYST, ROLE_ADMIN.
     *
     * @param file archivo multipart (campo "file")
     * @return 202 Accepted con UploadResponse; 400 si vacío; 500 en error interno.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ANALYST', 'ROLE_ADMIN')")
    public ResponseEntity<UploadResponse> subir(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            log.warn("[CONTROLLER] Intento de subida con archivo vacío");
            return ResponseEntity.badRequest().build();
        }
        try {
            UploadResponse response = service.registrar(file);
            log.info("[CONTROLLER] Archivo aceptado | id={} nombre='{}'",
                    response.getId(), response.getNombre());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            log.error("[CONTROLLER] Error registrando archivo '{}': {}",
                    file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lista todos los archivos del sistema.
     * Roles permitidos: ROLE_ANALYST, ROLE_ADMIN.
     *
     * @return lista completa de archivos
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ANALYST', 'ROLE_ADMIN')")
    public ResponseEntity<List<Archivo>> listar() {
        log.debug("[CONTROLLER] Listando archivos");
        return ResponseEntity.ok(service.listar());
    }

    /**
     * Obtiene un archivo por ID.
     * Roles permitidos: ROLE_ANALYST, ROLE_ADMIN.
     *
     * @param id identificador del archivo
     * @return 200 con el archivo; 404 si no existe.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ANALYST', 'ROLE_ADMIN')")
    public ResponseEntity<Archivo> obtener(@PathVariable Long id) {
        log.debug("[CONTROLLER] Solicitando archivo id={}", id);
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("[CONTROLLER] Archivo id={} no encontrado", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Elimina un archivo del sistema.
     * Roles permitidos: solo ROLE_ADMIN.
     *
     * @param id identificador del archivo a eliminar
     * @return 204 No Content si se eliminó correctamente.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("[CONTROLLER] Eliminando archivo id={}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manejo local de errores de tamaño máximo de archivo.
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        log.warn("[CONTROLLER] Archivo demasiado grande: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of("error", "El archivo supera el tamaño máximo permitido"));
    }
}
