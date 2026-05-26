package backend.sgap.security;

/**
 * Roles de autorización del sistema SGAP.
 *
 * <ul>
 *   <li>{@link #ROLE_ADMIN}    – Administrador: acceso total, puede eliminar archivos.</li>
 *   <li>{@link #ROLE_ANALYST}  – Analista: puede ver archivos, eventos y descargas.</li>
 *   <li>{@link #ROLE_USER}     – Usuario estándar: puede subir archivos.</li>
 * </ul>
 */
public enum Role {
    ROLE_ADMIN,
    ROLE_ANALYST,
    ROLE_USER
}
