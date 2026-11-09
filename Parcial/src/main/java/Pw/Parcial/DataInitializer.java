package Pw.Parcial;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean CommandLineRunner seed(AppUserRepository users, MedicoRepository medicos,
                                 ConsultaRepository consultas, PasswordEncoder encoder) {
        return args -> {
            if (users.findByUsername("admin").isEmpty())
                users.save(new AppUser("admin", encoder.encode("Admin123!"), Role.ADMINISTRADOR));
            if (users.findByUsername("paciente").isEmpty())
                users.save(new AppUser("paciente", encoder.encode("Paciente123!"), Role.PACIENTE));

            Medico medico = medicos.findByUsuario("dra.laura").orElseGet(() -> {
                Medico nuevo = new Medico();
                nuevo.setNombre("Laura Gómez");
                nuevo.setEspecialidad("Medicina general");
                nuevo.setUsuario("dra.laura");
                return medicos.save(nuevo);
            });
            if (users.findByUsername("dra.laura").isEmpty())
                users.save(new AppUser("dra.laura", encoder.encode("Medico123!"), Role.MEDICO));

            if (consultas.findByMedicoUsuarioOrderByInicioAsc("dra.laura").isEmpty()) {
                Consulta demo = new Consulta();
                demo.setPacienteNombre("paciente"); demo.setMotivo("Consulta de ejemplo"); demo.setConsultorio(101);
                demo.setInicio(java.time.LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0));
                demo.setFin(demo.getInicio().plusMinutes(30)); demo.setMedico(medico);
                consultas.save(demo);
            }
        };
    }
}
