package Pw.Parcial;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.regex.Pattern;

@Controller
public class WebController {
    private static final Pattern PATIENT_USERNAME = Pattern.compile("[\\p{L}\\p{M}0-9 .'-]{1,40}");
    private final MedicoRepository medicos;
    private final ConsultaRepository consultas;
    private final AppUserRepository users;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;

    public WebController(MedicoRepository medicos, ConsultaRepository consultas, AppUserRepository users,
                         org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this.medicos = medicos; this.consultas = consultas; this.users = users; this.encoder = encoder;
    }

    @GetMapping("/login") String login() { return "login"; }
    @GetMapping("/acceso-denegado") String accesoDenegado() { return "acceso-denegado"; }

    @GetMapping("/") String home(Authentication auth) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"))) return "redirect:/admin";
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEDICO"))) return "redirect:/medico";
        return "redirect:/paciente";
    }

    @GetMapping("/admin") String admin(Model model) {
        model.addAttribute("medicos", medicos.findAll()); model.addAttribute("consulta", new Consulta());
        model.addAttribute("medico", new Medico()); model.addAttribute("consultas", consultas.findAll());
        return "admin";
    }

    @Transactional
    @PostMapping("/admin/medicos") String crearMedico(@Valid @ModelAttribute("medico") Medico form, BindingResult result,
                                                        Model model, RedirectAttributes flash) {
        if (form.getClave() == null || form.getClave().length() < 6 || form.getClave().length() > 100)
            result.rejectValue("clave", "clave.invalida", "La contraseña debe tener entre 6 y 100 caracteres");
        if (result.hasErrors() || users.findByUsername(form.getUsuario()).isPresent()) {
            if (users.findByUsername(form.getUsuario()).isPresent()) result.rejectValue("usuario", "duplicado", "Ese usuario ya existe");
            model.addAttribute("medicos", medicos.findAll()); model.addAttribute("consultas", consultas.findAll());
            model.addAttribute("consulta", new Consulta()); return "admin";
        }
        String clave = form.getClave(); form.setClave(null);
        Medico saved = medicos.save(form);
        users.save(new AppUser(saved.getUsuario(), encoder.encode(clave), Role.MEDICO));
        flash.addFlashAttribute("mensaje", "Médico creado correctamente"); return "redirect:/admin";
    }

    @Transactional
    @PostMapping("/admin/pacientes")
    String crearCuentaPaciente(@RequestParam String usuario, @RequestParam String clave, RedirectAttributes flash) {
        String nombrePaciente = usuario == null ? "" : usuario.trim();
        if (!PATIENT_USERNAME.matcher(nombrePaciente).matches() || clave == null || clave.length() < 6 || clave.length() > 100) {
            flash.addFlashAttribute("error", "Ingresa el nombre del paciente (máximo 40 caracteres) y una contraseña de 6 a 100 caracteres");
            return "redirect:/admin";
        }
        if (users.existsByUsername(nombrePaciente)) {
            flash.addFlashAttribute("error", "Ya existe una cuenta con ese usuario");
            return "redirect:/admin";
        }
        users.save(new AppUser(nombrePaciente, encoder.encode(clave), Role.PACIENTE));
        flash.addFlashAttribute("mensaje", "Cuenta de paciente creada. El usuario debe coincidir con el nombre al asignar una consulta.");
        return "redirect:/admin";
    }

    @GetMapping("/admin/medicos/{id}/editar") String editarMedico(@PathVariable Long id, Model model) {
        Medico medico = medicos.findById(id).orElseThrow(); medico.setClave(null);
        model.addAttribute("medico", medico); return "editar-medico";
    }

    @Transactional
    @PostMapping("/admin/medicos/{id}/editar") String guardarEdicion(@PathVariable Long id, @ModelAttribute Medico form,
                                                                       RedirectAttributes flash) {
        Medico medico = medicos.findById(id).orElseThrow();
        if (form.getNombre() == null || form.getNombre().isBlank() || form.getNombre().length() > 80 ||
            form.getEspecialidad() == null || form.getEspecialidad().isBlank() || form.getEspecialidad().length() > 80 ||
            form.getUsuario() == null || form.getUsuario().isBlank() || form.getUsuario().length() > 40) {
            flash.addFlashAttribute("error", "Completa los datos del médico con valores válidos"); return "redirect:/admin/medicos/" + id + "/editar";
        }
        if (!medico.getUsuario().equals(form.getUsuario()) && users.findByUsername(form.getUsuario()).isPresent()) {
            flash.addFlashAttribute("error", "Ese usuario ya existe"); return "redirect:/admin/medicos/" + id + "/editar";
        }
        String viejoUsuario = medico.getUsuario(); medico.setNombre(form.getNombre()); medico.setEspecialidad(form.getEspecialidad()); medico.setUsuario(form.getUsuario());
        medicos.save(medico); users.findByUsername(viejoUsuario).ifPresent(u -> { u.setUsername(form.getUsuario()); users.save(u); });
        flash.addFlashAttribute("mensaje", "Médico actualizado correctamente"); return "redirect:/admin";
    }

    @Transactional
    @PostMapping("/admin/medicos/{id}/eliminar") String eliminarMedico(@PathVariable Long id, RedirectAttributes flash) {
        if (consultas.existsByMedico_Id(id)) {
            flash.addFlashAttribute("error", "No se puede eliminar un médico con consultas asignadas");
        } else medicos.findById(id).ifPresent(m -> { users.findByUsername(m.getUsuario()).ifPresent(users::delete); medicos.delete(m); });
        return "redirect:/admin";
    }

    @Transactional
    @PostMapping("/admin/consultas") String crearConsulta(@Valid @ModelAttribute("consulta") Consulta form, BindingResult result,
                                                            @RequestParam Long medicoId, Model model, RedirectAttributes flash) {
        if (form.getInicio() != null && form.getFin() != null && !form.getFin().isAfter(form.getInicio()))
            result.rejectValue("fin", "horario", "La hora final debe ser posterior a la inicial");
        Medico doctor = medicos.findById(medicoId).orElse(null);
        if (doctor == null) result.reject("medico", "Seleccione un médico válido");
        if (result.hasErrors()) {
            model.addAttribute("medicos", medicos.findAll()); model.addAttribute("medico", new Medico());
            model.addAttribute("consultas", consultas.findAll()); return "admin";
        }
        form.setMedico(doctor); consultas.save(form);
        flash.addFlashAttribute("mensaje", "Consulta asignada correctamente"); return "redirect:/admin";
    }

    @GetMapping("/medico") String medico(Authentication auth, Model model) {
        model.addAttribute("consultas", consultas.findByMedicoUsuarioOrderByInicioAsc(auth.getName()));
        return "medico";
    }

    @GetMapping("/paciente") String paciente(Authentication auth, Model model) {
        model.addAttribute("consultas", consultas.findByPacienteNombreIgnoreCaseOrderByInicioAsc(auth.getName()));
        return "paciente";
    }

    @Transactional
    @PostMapping("/paciente/consultas/{id}/horario") String actualizarHorario(@PathVariable Long id,
            @RequestParam java.time.LocalDateTime inicio, @RequestParam java.time.LocalDateTime fin,
            Authentication auth, RedirectAttributes flash) {
        Consulta consulta = consultas.findById(id).orElseThrow();
        if (!consulta.getPacienteNombre().equalsIgnoreCase(auth.getName()))
            throw new org.springframework.security.access.AccessDeniedException("La cita no pertenece a este paciente");
        if (inicio == null || fin == null || !inicio.isAfter(java.time.LocalDateTime.now()) || !fin.isAfter(inicio)) {
            flash.addFlashAttribute("error", "Indica un horario futuro y una hora final posterior a la inicial");
            return "redirect:/paciente";
        }
        consulta.setInicio(inicio); consulta.setFin(fin); consultas.save(consulta);
        flash.addFlashAttribute("mensaje", "Horario actualizado correctamente"); return "redirect:/paciente";
    }
}
