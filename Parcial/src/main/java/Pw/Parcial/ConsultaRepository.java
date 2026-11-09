package Pw.Parcial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    @Override
    @EntityGraph(attributePaths = "medico")
    List<Consulta> findAll();

    @Override
    @EntityGraph(attributePaths = "medico")
    Optional<Consulta> findById(Long id);

    boolean existsByMedico_Id(Long medicoId);

    @EntityGraph(attributePaths = "medico")
    List<Consulta> findByMedicoUsuarioOrderByInicioAsc(String usuario);

    @EntityGraph(attributePaths = "medico")
    List<Consulta> findByPacienteNombreIgnoreCaseOrderByInicioAsc(String pacienteNombre);
}
