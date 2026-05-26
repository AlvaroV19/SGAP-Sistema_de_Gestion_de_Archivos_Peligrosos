package backend.sgap.dto;

import lombok.Data;

/**
 * DTO de entrada para el endpoint {@code POST /auth/login}.
 */
@Data
public class LoginRequest {

    private String username;
    private String password;
}
