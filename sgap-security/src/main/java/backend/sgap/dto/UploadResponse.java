package backend.sgap.dto;

import backend.sgap.enums.EstadoArchivo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Respuesta inmediata devuelta por el endpoint POST /api/archivos.
 *
 * <p>El cliente recibe este DTO con HTTP 202 Accepted en cuanto el archivo
 * es almacenado en cuarentena local. El análisis de seguridad real se
 * ejecutará de forma asíncrona tras la demora configurada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    /** Identificador del registro creado en base de datos. */
    private Long id;

    /** Nombre original del archivo recibido. */
    private String nombre;

    /** Estado inicial: siempre {@link EstadoArchivo#PENDING} en esta respuesta. */
    private EstadoArchivo estado;

    /** Marca de tiempo de recepción. */
    private LocalDateTime fechaSubida;

    /** Mensaje informativo para el cliente. */
    private String mensaje;
}
