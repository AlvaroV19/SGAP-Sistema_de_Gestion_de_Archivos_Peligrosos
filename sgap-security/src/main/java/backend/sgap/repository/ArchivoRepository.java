package backend.sgap.repository;

import backend.sgap.entity.Archivo;
import backend.sgap.enums.EstadoArchivo;
import backend.sgap.enums.NivelRiesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link Archivo}.
 *
 * <p>Se añaden queries útiles para el dashboard de seguridad y la revisión
 * manual de archivos en cuarentena.
 */
public interface ArchivoRepository extends JpaRepository<Archivo, Long> {

    /** Archivos por estado (SAFE / QUARANTINED / UNSAFE). */
    List<Archivo> findByEstado(EstadoArchivo estado);

    /** Archivos por nivel de riesgo. */
    List<Archivo> findByNivelRiesgo(NivelRiesgo nivelRiesgo);

    /** Archivos QUARANTINED o UNSAFE pendientes de revisión. */
    List<Archivo> findByEstadoIn(List<EstadoArchivo> estados);

    /** Buscar por hash SHA-256 (detección de duplicados). */
    List<Archivo> findByHash(String hash);

    /** Archivos que contienen un flag de seguridad específico. */
    @Query("SELECT a FROM Archivo a WHERE a.flagsSeguridad LIKE %:flag%")
    List<Archivo> findByFlag(String flag);

    /** Conteo de archivos por estado para el dashboard. */
    long countByEstado(EstadoArchivo estado);
}
