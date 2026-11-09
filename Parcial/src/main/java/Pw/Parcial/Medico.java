package Pw.Parcial;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Medico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 80) @Column(nullable = false, length = 80)
    private String nombre;
    @NotBlank @Size(max = 80) @Column(nullable = false, length = 80)
    private String especialidad;
    @NotBlank @Size(min = 3, max = 40) @Column(nullable = false, unique = true, length = 40)
    private String usuario;
    @Transient
    private String clave;
}
