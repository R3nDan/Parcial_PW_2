package Pw.Parcial;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Consulta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 40) @Pattern(regexp = "[\\p{L}\\p{M}0-9 .'-]+", message = "Use solo letras y números")
    @Column(nullable = false, length = 40)
    private String pacienteNombre;
    @NotBlank @Size(max = 100) @Column(nullable = false, length = 100)
    private String motivo;
    @NotNull @Min(101) @Max(999) @Column(nullable = false)
    private Integer consultorio;
    @NotNull @FutureOrPresent @Column(nullable = false)
    private LocalDateTime inicio;
    @NotNull @Column(nullable = false)
    private LocalDateTime fin;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;
}
