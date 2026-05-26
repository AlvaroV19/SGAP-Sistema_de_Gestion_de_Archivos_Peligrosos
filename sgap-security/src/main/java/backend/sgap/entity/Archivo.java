package backend.sgap.entity;

import backend.sgap.enums.EstadoArchivo;
import backend.sgap.enums.NivelRiesgo;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Representa un archivo gestionado por SGAP.
 *
 * <p><b>Campos de ciclo de vida asíncrono:</b>
 * <ul>
 *   <li>{@link #fechaSubida}   – marca de tiempo del momento de recepción HTTP.</li>
 *   <li>{@link #rutaTemporal}  – ruta del archivo en cuarentena local; usada por
 *       el worker asíncrono para retomar el procesamiento tras la demora.</li>
 * </ul>
 *
 * <p><b>IMPORTANTE:</b> {@link #tipoArchivo} es el Content-Type declarado por el
 * cliente y <em>no se debe usar para decisiones de seguridad</em>. Usar siempre
 * {@link #mimeDetectado}.
 */
@Entity
@Table(name = "archivos")
@Data
public class Archivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre original del archivo enviado por el cliente (no confiable). */
    private String nombre;

    /**
     * Content-Type declarado por el cliente. <strong>No confiable.</strong>
     * Usar {@link #mimeDetectado} para decisiones de seguridad.
     */
    @Column(name = "tipo_archivo")
    private String tipoArchivo;

    /** MIME detectado por Apache Tika sobre el contenido real del archivo. */
    @Column(name = "mime_detectado")
    private String mimeDetectado;

    /** Extensión declarada en el nombre original (minúsculas, sin punto). */
    @Column(name = "extension_declarada")
    private String extensionDeclarada;

    /** SHA-256 del contenido binario del archivo. */
    private String hash;

    /** Tamaño en bytes. */
    private Long tamano;

    /** URL de acceso al archivo en MinIO (pre-firmada o pública). */
    @Column(columnDefinition = "TEXT")
    private String ruta;

    /**
     * Ruta absoluta del archivo en cuarentena local.
     * Persiste para que el worker asíncrono pueda retomar el procesamiento
     * incluso tras un reinicio controlado de la aplicación.
     * Se borra (null) una vez que el archivo fue procesado o el temp eliminado.
     */
    @Column(name = "ruta_temporal", columnDefinition = "TEXT")
    private String rutaTemporal;

    /** Marca de tiempo de recepción del archivo (momento del POST). */
    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;

    /**
     * Flags de seguridad detectados, separados por coma.
     * Ejemplo: {@code "MIME_MISMATCH,DOUBLE_EXTENSION"}
     */
    @Column(name = "flags_seguridad")
    private String flagsSeguridad;

    /** Descripción legible de los hallazgos del análisis. */
    @Column(name = "descripcion_analisis", columnDefinition = "TEXT")
    private String descripcionAnalisis;

    /** Nivel de riesgo calculado por el motor de reglas. */
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo")
    private NivelRiesgo nivelRiesgo;

    /** Estado del ciclo de vida del archivo en el pipeline asíncrono. */
    @Enumerated(EnumType.STRING)
    private EstadoArchivo estado;

    /**
     * Amenaza asociada manualmente tras revisión (null si no aplica
     * o si aún está pendiente de clasificación manual).
     */
    @ManyToOne
    @JoinColumn(name = "amenaza_id", nullable = true)
    private Amenaza amenaza;
}
