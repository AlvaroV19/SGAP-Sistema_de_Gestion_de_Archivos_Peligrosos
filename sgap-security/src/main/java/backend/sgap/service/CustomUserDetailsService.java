package backend.sgap.service;

import backend.sgap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link UserDetailsService} que carga usuarios desde la base
 * de datos PostgreSQL a través de {@link UserRepository}.
 *
 * <p>Spring Security invoca este servicio durante la autenticación para obtener
 * el {@link UserDetails} y comparar credenciales.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("[USER_DETAILS] Cargando usuario '{}'", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[USER_DETAILS] Usuario '{}' no encontrado", username);
                    return new UsernameNotFoundException("Usuario no encontrado: " + username);
                });
    }
}
