package backend.sgap.entity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "amenazas")
@Data
public class Amenaza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo;
    private String descripcion;
    private String severidad;
}