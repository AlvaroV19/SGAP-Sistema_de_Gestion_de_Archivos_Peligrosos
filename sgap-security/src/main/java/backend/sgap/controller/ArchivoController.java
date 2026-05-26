package backend.sgap.controller;

import backend.sgap.config.CorsConfig;
import backend.sgap.dto.UploadResponse;
import backend.sgap.entity.Archivo;
import backend.sgap.service.ArchivoService;
import backend.sgap.service.ReporteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controlador REST para la gestión de archivos en SGAP.
 *
 * <h3>Seguridad por rol</h3>
 * <ul>
 *   <li>POST   – USER, ANALYST, ADMIN pueden subir archivos.</li>
 *   <li>GET    – ANALYST y ADMIN pueden listar/ver archivos.</li>
 *   <li>GET /{id}/reporte – ANALYST y ADMIN pueden descargar el PDF de un archivo.</li>
 *   <li>GET /reporte      – ANALYST y ADMIN pueden descargar el PDF consolidado.</li>
 *   <li>DELETE – solo ADMIN puede eliminar archivos.</li>
 * </ul>
 *
 * <p>CORS gestionado globalmente por {@link CorsConfig}.
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

    private final ArchivoService  service;
    private final ReporteService  reporteService;

    // ── Subida ────────────────────────────────────────────────────────────────

    /**
     * Sube un archivo al sistema SGAP.
     * Roles permitidos: ROLE_USER, ROLE_ANALYST, ROLE_ADMIN.
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

    // ── Consulta ──────────────────────────────────────────────────────────────

    /**
     * Lista todos los archivos del sistema.
     * Roles permitidos: ROLE_ANALYST, ROLE_ADMIN.
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
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("[CONTROLLER] Eliminando archivo id={}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ── Reportes PDF ──────────────────────────────────────────────────────────

    /**
     * Genera y descarga el reporte PDF de análisis de un archivo específico.
     * Roles permitidos: ROLE_ANALYST, ROLE_ADMIN.
     *
     * @param id identificador del archivo
     * @return PDF con los resultados del análisis de seguridad
     */
    @GetMapping(value = "/{id}/reporte", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ANALYST', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> reporteArchivo(@PathVariable Long id) {
        log.info("[CONTROLLER] Generando reporte PDF para archivo id={}", id);

        return service.buscarPorId(id)
                .<ResponseEntity<byte[]>>map(archivo -> {
                    try {
                        byte[] pdf = reporteService.generarReporteArchivo(archivo);
                        String filename = "reporte_sgap_" + id + "_" + archivo.getNombre() + ".pdf";
                        return ResponseEntity.<byte[]>ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                        "attachment; filename=\"" + filename + "\"")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(pdf);
                    } catch (Exception e) {
                        log.error("[CONTROLLER] Error generando reporte PDF para id={}: {}", id, e.getMessage(), e);
                        return ResponseEntity.<byte[]>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElseGet(() -> {
                    log.warn("[CONTROLLER] Archivo id={} no encontrado para reporte", id);
                    return ResponseEntity.<byte[]>notFound().build();
                });
    }

    /**
     * Genera y descarga el reporte PDF consolidado de todos los archivos.
     * Roles permitidos: ROLE_ANALYST, ROLE_ADMIN.
     *
     * @return PDF con el resumen completo del repositorio
     */
    @GetMapping(value = "/reporte", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ANALYST', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> reporteConsolidado() {
        log.info("[CONTROLLER] Generando reporte PDF consolidado");
        try {
            List<Archivo> archivos = service.listar();
            byte[] pdf = reporteService.generarReporteConsolidado(archivos);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"reporte_sgap_consolidado.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("[CONTROLLER] Error generando reporte consolidado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
