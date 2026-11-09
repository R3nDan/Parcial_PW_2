package Pw.Parcial;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParcialApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private MedicoRepository medicos;

    @Autowired
    private ConsultaRepository consultas;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void medicoDeDemostracionSeCreaAunqueYaExistanOtrosUsuarios() {
        var medico = medicos.findByUsuario("dra.laura").orElseThrow();
        var cuenta = users.findByUsername("dra.laura").orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals("Laura Gómez", medico.getNombre());
        org.junit.jupiter.api.Assertions.assertEquals(Role.MEDICO, cuenta.getRole());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("Medico123!", cuenta.getPassword()));
        org.junit.jupiter.api.Assertions.assertTrue(consultas.existsByMedico_Id(medico.getId()));
    }

    @Test
    void loginEstaDisponibleSinAutenticacion() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void apiRequiereAutenticacion() throws Exception {
        mvc.perform(get("/api/consultas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openApiDocumentaRutasYCodigosDeRespuesta() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/medicos'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/medicos'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/medicos'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/medicos'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/consultas/{id}/horario'].put.responses['403']").exists());
    }

    @Test
    void swaggerUiEstaDisponible() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerPuedeAutenticarUnAdministradorConHttpBasic() throws Exception {
        mvc.perform(get("/api/medicos")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "Admin123!")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PACIENTE")
    void apiRestringeMedicosAlAdministrador() throws Exception {
        mvc.perform(get("/api/medicos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeConsultarMedicos() throws Exception {
        mvc.perform(get("/api/medicos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void apiRechazaConsultoriosMenoresQue101() throws Exception {
        mvc.perform(post("/api/consultas")
                        .contentType("application/json")
                        .content("""
                                {"pacienteNombre":"Paciente","motivo":"Control","consultorio":3,
                                 "inicio":"2030-01-01T09:00:00","fin":"2030-01-01T09:30:00","medicoId":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    void paginaAdministrativaCarga() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    void administradorPuedeCrearAccesoParaPaciente() throws Exception {
        mvc.perform(post("/admin/pacientes")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("usuario", "Paciente de prueba")
                        .param("clave", "Clave123!"))
                .andExpect(status().is3xxRedirection());

        var cuenta = users.findByUsername("Paciente de prueba").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Role.PACIENTE, cuenta.getRole());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("Clave123!", cuenta.getPassword()));
    }

    @Test
    @WithMockUser(username = "dra.laura", roles = "MEDICO")
    void paginaDeMedicoCarga() throws Exception {
        mvc.perform(get("/medico"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "paciente", roles = "PACIENTE")
    void paginaDePacienteCarga() throws Exception {
        mvc.perform(get("/paciente"))
                .andExpect(status().isOk());
    }
}
