package backend.sgap.config;

import backend.sgap.entity.User;
import backend.sgap.repository.UserRepository;
import backend.sgap.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seed de datos inicial para la base de datos SGAP.
 *
 * <p>Crea los usuarios por defecto si no existen al arrancar la aplicación.
 * Activo en todos los perfiles excepto {@code prod} (donde el admin
 * debe crearse manualmente o via migración controlada).
 *
 * <p><strong>IMPORTANTE:</strong> Cambiar las contraseñas de seed en producción.
 * Gestionar vía variables de entorno {@code SEED_ADMIN_PASSWORD}, etc.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Profile("!prod")
    public CommandLineRunner seedUsers() {
        return args -> {
            createUserIfNotExists(
                    "admin",
                    System.getenv().getOrDefault("SEED_ADMIN_PASSWORD", "Admin@2025!"),
                    "Administrador SGAP",
                    Role.ROLE_ADMIN
            );
            createUserIfNotExists(
                    "analyst",
                    System.getenv().getOrDefault("SEED_ANALYST_PASSWORD", "Analyst@2025!"),
                    "Analista SGAP",
                    Role.ROLE_ANALYST
            );
            createUserIfNotExists(
                    "user",
                    System.getenv().getOrDefault("SEED_USER_PASSWORD", "User@2025!"),
                    "Usuario SGAP",
                    Role.ROLE_USER
            );
            log.info("[SEED] Verificación de usuarios seed completada.");
        };
    }

    private void createUserIfNotExists(String username, String rawPassword, String fullName, Role role) {
        if (userRepository.existsByUsername(username)) {
            log.debug("[SEED] Usuario '{}' ya existe, omitiendo.", username);
            return;
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("[SEED] Usuario '{}' creado con rol '{}'.", username, role);
    }
}
