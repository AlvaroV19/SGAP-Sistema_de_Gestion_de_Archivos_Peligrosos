package backend.sgap.controller;

import backend.sgap.dto.LoginRequest;
import backend.sgap.dto.LoginResponse;
import backend.sgap.entity.User;
import backend.sgap.repository.UserRepository;
import backend.sgap.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import backend.sgap.dto.RegisterRequest;
import backend.sgap.security.Role;
import org.springframework.http.HttpStatus;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador de autenticación JWT.
 *
 * <p>Endpoint público: {@code POST /auth/login}.
 * Devuelve un Bearer token JWT en caso de credenciales correctas.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Autentica usuario con username/password y devuelve JWT.
     *
     * @param request credenciales del usuario
     * @return {@code 200 OK} con {@link LoginResponse} si las credenciales son válidas;
     *         {@code 401 Unauthorized} si son incorrectas o el usuario está deshabilitado.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("[AUTH] Intento de login para usuario '{}'", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();
            String token = jwtService.generateToken(user);

            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .role(user.getRole().name())
                    .authorities(
                            user.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .collect(Collectors.toList())
                    )
                    .expiresIn(jwtExpirationMs)
                    .build();

            log.info("[AUTH] Login exitoso para '{}' con rol '{}'", user.getUsername(), user.getRole());
            return ResponseEntity.ok(response);

        } catch (DisabledException e) {
            log.warn("[AUTH] Cuenta deshabilitada: '{}'", request.getUsername());
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Cuenta deshabilitada. Contacte al administrador."));
        } catch (BadCredentialsException e) {
            log.warn("[AUTH] Credenciales incorrectas para '{}'", request.getUsername());
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Usuario o contraseña incorrectos."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El usuario ya existe."));
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.valueOf(request.getRole()))
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("[AUTH] Usuario '{}' registrado con rol '{}'", user.getUsername(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Usuario creado correctamente."));
    }
}
