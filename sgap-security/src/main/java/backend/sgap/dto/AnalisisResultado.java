package backend.sgap.dto;

import backend.sgap.enums.NivelRiesgo;
import backend.sgap.enums.SecurityFlag;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Resultado inmutable del análisis de seguridad sobre un archivo.
 * Producido por {@code SecurityAnalysisService} y consumido por {@code ArchivoService}
 * para decidir el destino final del archivo y los metadatos a persistir.
 */
@Data
@Builder
public class AnalisisResultado {

    /** MIME real detectado por Apache Tika (fuente de verdad). */
    private final String mimeDetectado;

    /** Extensión en minúsculas extraída del nombre original del archivo. */
    private final String extensionDeclarada;

    /** Nivel de riesgo calculado por el motor de reglas. */
    private final NivelRiesgo nivelRiesgo;

    /**
     * Conjunto de flags disparados durante el análisis.
     * Nunca es {@code null}; puede estar vacío si el archivo es limpio.
     */
    private final Set<SecurityFlag> flags;

    /**
     * Descripción textual consolidada de todos los motivos de riesgo.
     * Ejemplo: "MIME incompatible: .pdf pero Tika detectó application/x-dosexec;
     *           Extensión sospechosa: .exe"
     */
    private final String descripcion;
}
