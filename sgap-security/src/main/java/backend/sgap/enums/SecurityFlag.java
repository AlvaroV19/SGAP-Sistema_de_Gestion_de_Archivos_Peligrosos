package backend.sgap.enums;

/**
 * Indicadores de seguridad individuales detectados durante el análisis.
 * Un archivo puede acumular múltiples flags; el nivel de riesgo final
 * refleja la regla más grave aplicada.
 *
 * <p>Preparado para futuros motores:
 * <ul>
 *   <li>YARA_MATCH        – regla YARA disparada</li>
 *   <li>ANTIVIRUS_HIT     – firma de antivirus encontrada</li>
 *   <li>VIRUSTOTAL_MATCH  – positivo en VirusTotal</li>
 *   <li>HEURISTIC_ANOMALY – comportamiento anómalo heurístico</li>
 * </ul>
 */
public enum SecurityFlag {

    // ── Reglas activas ────────────────────────────────────────────────────
    /** La extensión declarada pertenece a la lista de ejecutables/scripts. */
    SUSPICIOUS_EXTENSION,

    /** El MIME real detectado por Tika no es compatible con la extensión declarada. */
    MIME_MISMATCH,

    /** El nombre contiene más de una extensión real (ej: factura.pdf.exe). */
    DOUBLE_EXTENSION,

    /** El MIME detectado por Tika es inherentemente peligroso (ejecutable, script, etc.). */
    DANGEROUS_MIME,

    // ── Marcadores de placeholder para motores futuros ────────────────────
    /** Reservado: coincidencia con regla YARA. */
    YARA_MATCH,

    /** Reservado: firma de antivirus encontrada. */
    ANTIVIRUS_HIT,

    /** Reservado: positivo en VirusTotal. */
    VIRUSTOTAL_MATCH,

    /** Reservado: anomalía detectada por análisis heurístico. */
    HEURISTIC_ANOMALY
}
