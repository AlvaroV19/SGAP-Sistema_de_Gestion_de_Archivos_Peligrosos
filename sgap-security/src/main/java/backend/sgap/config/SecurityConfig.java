package backend.sgap.config;

import backend.sgap.security.JwtAccessDeniedHandler;
import backend.sgap.security.JwtAuthenticationEntryPoint;
import backend.sgap.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración principal de Spring Security para SGAP.
 *
 * <ul>
 *   <li>Stateless: sin sesiones HTTP (JWT only).</li>
 *   <li>CSRF deshabilitado: API REST consumida desde Vue.js con JWT.</li>
 *   <li>CORS gestionado por {@link CorsConfig} integrado aquí.</li>
 *   <li>Endpoints protegidos con roles granulares por HTTP method.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final UserDetailsService userDetailsService;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CSRF: deshabilitado para API REST stateless ───────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS: delegado a CorsConfig (WebMvcConfigurer) ────────────
            .cors(cors -> cors.configure(http))

            // ── Sesiones: STATELESS (JWT) ─────────────────────────────────
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Manejo de errores 401 / 403 ───────────────────────────────
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .accessDeniedHandler(jwtAccessDeniedHandler))

            // ── Reglas de autorización ────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Preflight CORS: siempre permitido sin autenticación
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Endpoints público de autenticación
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/error").permitAll()
                // Actuator (health checks) - público
                .requestMatchers("/actuator/health").permitAll()

                // ── Archivos ─────────────────────────────────────────────

                // Subir archivos → USER, ANALYST, ADMIN
                .requestMatchers(HttpMethod.POST, "/api/archivos/**")
                        .hasAnyAuthority("ROLE_USER", "ROLE_ANALYST", "ROLE_ADMIN")

                // Listar y ver archivos → ANALYST, ADMIN
                .requestMatchers(HttpMethod.GET, "/api/archivos/**")
                        .hasAnyAuthority("ROLE_ANALYST", "ROLE_ADMIN")

                // Eliminar archivos → solo ADMIN
                .requestMatchers(HttpMethod.DELETE, "/api/archivos/**")
                        .hasAuthority("ROLE_ADMIN")

                // ── Eventos ──────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/eventos/**")
                        .hasAnyAuthority("ROLE_ANALYST", "ROLE_ADMIN")

                // ── Resto de /api/** → autenticado ───────────────────────
                .requestMatchers("/api/**").authenticated()

                // Cualquier otra ruta → denegada por defecto
                .anyRequest().denyAll()
            )

            // ── Proveedor de autenticación ────────────────────────────────
            .authenticationProvider(authenticationProvider())

            // ── Insertar filtro JWT antes de UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
