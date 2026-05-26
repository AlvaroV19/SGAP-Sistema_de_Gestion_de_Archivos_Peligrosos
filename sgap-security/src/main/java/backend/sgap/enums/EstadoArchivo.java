package backend.sgap.enums;

/**
 * Estado del ciclo de vida de un archivo en el pipeline de seguridad.
 *
 * <ul>
 *   <li>{@link #PENDING}     – recibido, pendiente de análisis asíncrono.</li>
 *   <li>{@link #PROCESSING}  – análisis en curso (segunda plano).</li>
 *   <li>{@link #SAFE}        – riesgo LOW; movido al bucket definitivo.</li>
 *   <li>{@link #QUARANTINED} – riesgo MEDIUM; aislado para revisión.</li>
 *   <li>{@link #UNSAFE}      – riesgo HIGH; movido al bucket blocked.</li>
 *   <li>{@link #FAILED}      – el pipeline falló irrecuperablemente.</li>
 * </ul>
 */
public enum EstadoArchivo {
    PENDING,
    PROCESSING,
    SAFE,
    QUARANTINED,
    UNSAFE,
    FAILED
}
