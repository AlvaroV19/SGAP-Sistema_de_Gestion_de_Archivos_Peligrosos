package backend.sgap.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String fullName;
    private String role; // "ROLE_ADMIN", "ROLE_ANALYST", "ROLE_USER"
}
