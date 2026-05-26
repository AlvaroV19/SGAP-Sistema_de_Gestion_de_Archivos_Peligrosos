package backend.sgap.enums;

/**
 * Nivel de riesgo calculado por el motor de análisis.
 * El ordinal del enum refleja la jerarquía: LOW(0) < MEDIUM(1) < HIGH(2),
 * lo que permite comparar niveles directamente con {@code ordinal()}.
 */
public enum NivelRiesgo {
    LOW,
    MEDIUM,
    HIGH
}
