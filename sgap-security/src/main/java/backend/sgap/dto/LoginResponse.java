package backend.sgap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta para el endpoint {@code POST /auth/login}.
 *
 * <p>Incluye el token JWT y metadatos del usuario autenticado
 * para que el frontend Vue.js pueda inicializar el estado de sesión.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** Bearer token JWT. El cliente debe enviarlo en {@code Authorization: Bearer <token>}. */
    private String token;

    /** Tipo de token; siempre {@code "Bearer"}. */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Username del usuario autenticado. */
    private String username;

    /** Nombre completo del usuario. */
    private String fullName;

    /** Rol asignado (e.g. {@code "ROLE_ADMIN"}). */
    private String role;

    /** Lista de authorities (roles) del usuario. */
    private List<String> authorities;

    /** Milisegundos hasta la expiración del token (para que el frontend programe el refresh). */
    private long expiresIn;
}
