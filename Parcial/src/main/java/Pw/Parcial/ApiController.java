package Pw.Parcial;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Consultas médicas", description = "Operaciones protegidas por rol para médicos y consultas.")
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
@SecurityRequirement(name = "basicAuth")
public class ApiController {
    private final MedicoRepository medicos;
    private final ConsultaRepository consultas;
    private final AppUserRepository users;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;
    public ApiController(MedicoRepository medicos, ConsultaRepository consultas, AppUserRepository users,
                         org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this.medicos = medicos; this.consultas = consultas; this.users = users; this.encoder = encoder;
    }

    @Schema(description = "Datos del médico y las credenciales de su cuenta.")
    public record MedicoInput(
            @Schema(description = "Nombre completo, hasta 80 caracteres.", example = "Laura Gómez")
            @NotBlank @Size(max = 80) String nombre,
            @Schema(description = "Especialidad, hasta 80 caracteres.", example = "Medicina general")
            @NotBlank @Size(max = 80) String especialidad,
            @Schema(description = "Usuario único, entre 3 y 40 caracteres.", example = "dra.laura")
            @NotBlank @Size(min = 3, max = 40) String usuario,
            @Schema(description = "Contraseña inicial, entre 6 y 100 caracteres.", example = "Medico123!")
            @NotBlank @Size(min = 6, max = 100) String clave) {}

    @Schema(description = "Datos requeridos para asignar una consulta.")
    public record ConsultaInput(
            @Schema(description = "Nombre de usuario del paciente, hasta 40 caracteres.", example = "paciente")
            @NotBlank @Size(max = 40) @Pattern(regexp = "[\\p{L}\\p{M}0-9 .'-]+") String pacienteNombre,
            @Schema(description = "Motivo, hasta 100 caracteres.", example = "Consulta de control")
            @NotBlank @Size(max = 100) String motivo,
            @Schema(description = "Número de consultorio entre 101 y 999.", example = "101")
            @NotNull @Min(101) @Max(999) Integer consultorio,
            @Schema(description = "Fecha y hora de inicio ISO, actual o futura.", example = "2030-01-15T09:00:00")
            @NotNull @FutureOrPresent LocalDateTime inicio,
            @Schema(description = "Fecha y hora de finalización ISO, posterior al inicio.", example = "2030-01-15T09:30:00")
            @NotNull LocalDateTime fin,
            @Schema(description = "Identificador del médico registrado.", example = "1")
            @NotNull Long medicoId) {}

    @Operation(summary = "Listar médicos", description = "Devuelve el directorio de médicos. Requiere el rol ADMINISTRADOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de médicos devuelto."),
            @ApiResponse(responseCode = "401", description = "Falta autenticación HTTP Basic."),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es administrador.")})
    @GetMapping("/medicos") List<Medico> medicos() { return medicos.findAll(); }

    @Operation(summary = "Crear médico y sus credenciales", description = "Crea el médico y una cuenta con rol MEDICO. La contraseña se almacena con BCrypt.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Médico y cuenta creados."),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o faltantes."),
            @ApiResponse(responseCode = "401", description = "Falta autenticación HTTP Basic."),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es administrador."),
            @ApiResponse(responseCode = "409", description = "El nombre de usuario ya está registrado.")})
    @Transactional
    @PostMapping("/medicos") @ResponseStatus(HttpStatus.CREATED)
    Medico crearMedico(@Valid @RequestBody MedicoInput input) {
        if (users.findByUsername(input.usuario()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        Medico m = new Medico(); m.setNombre(input.nombre()); m.setEspecialidad(input.especialidad()); m.setUsuario(input.usuario());
        Medico saved = medicos.save(m); users.save(new AppUser(input.usuario(), encoder.encode(input.clave()), Role.MEDICO)); return saved;
    }

    @Operation(summary = "Actualizar médico", description = "Actualiza los datos y las credenciales de acceso del médico identificado por id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Médico actualizado."),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o faltantes."),
            @ApiResponse(responseCode = "401", description = "Falta autenticación HTTP Basic."),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es administrador."),
            @ApiResponse(responseCode = "404", description = "No existe un médico con ese id."),
            @ApiResponse(responseCode = "409", description = "El nombre de usuario ya está registrado por otra cuenta.")})
    @Transactional
    @PutMapping("/medicos/{id}") Medico actualizarMedico(@PathVariable Long id, @Valid @RequestBody MedicoInput input) {
        Medico m = medicos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!m.getUsuario().equals(input.usuario()) && users.findByUsername(input.usuario()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        String usuarioAnterior = m.getUsuario(); m.setNombre(input.nombre()); m.setEspecialidad(input.especialidad()); m.setUsuario(input.usuario());
        Medico saved = medicos.save(m);
        AppUser user = users.findByUsername(usuarioAnterior).orElseThrow(); user.setUsername(input.usuario()); user.setPassword(encoder.encode(input.clave())); users.save(user);
        return saved;
    }

    @Operation(summary = "Eliminar médico", description = "Elimina la cuenta y el registro del médico si no tiene consultas asignadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Médico eliminado sin contenido de respuesta."),
            @ApiResponse(responseCode = "401", description = "Falta autenticación HTTP Basic."),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es administrador."),
            @ApiResponse(responseCode = "404", description = "No existe un médico con ese id."),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque tiene consultas asignadas.")})
    @Transactional
    @DeleteMapping("/medicos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void eliminarMedico(@PathVariable Long id) {
        Medico m = medicos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (consultas.existsByMedico_Id(id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "El médico tiene consultas asignadas");
        users.findByUsername(m.getUsuario()).ifPresent(users::delete); medicos.delete(m);
    }

    @Operation(summary = "Listar consultas visibles", description = "El administrador obtiene todas las consultas; el médico obtiene las propias; el paciente obtiene las asociadas a su usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consultas visibles para el perfil autenticado."),
            @ApiResponse(responseCode = "401", description = "Falta autenticación HTTP Basic."),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene un rol permitido.")})
    @GetMapping("/consultas") List<Consulta> misConsultas(Authentication auth) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"))) return consultas.findAll();
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEDICO"))) return consultas.findByMedicoUsuarioOrderByInicioAsc(auth.getName());
        return consultas.findByPacienteNombreIgnoreCaseOrderByInicioAsc(auth.getName());
    }

    @Schema(description = "Nuevo horario de una consulta asignada al paciente autenticado.")
    public record HorarioInput(
            @Schema(description = "Nueva fecha y hora de inicio, actual o futura.", example = "2030-01-15T10:00:00")
            @NotNull @FutureOrPresent LocalDateTime inicio,
            @Schema(description = "Nueva fecha y hora de fin, posterior al inicio.", example = "2030-01-15T10:30:00")
            @NotNull LocalDateTime fin) {}

    @Operation(summary = "Cambiar el horario de una consulta", description = "Solo un paciente puede cambiar el horario de una consulta que esté asociada a su usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horario actualizado."),
            @ApiResponse(responseCode = "400", description = "Fechas inválidas o la hora final no es posterior a la inicial."),
            @ApiResponse(responseCode = "401", description = "Falta autenticación HTTP Basic."),
            @ApiResponse(responseCode = "403", description = "El usuario no es paciente o la consulta pertenece a otro paciente."),
            @ApiResponse(responseCode = "404", description = "No existe una consulta con ese id.")})
    @Transactional
    @PutMapping("/consultas/{id}/horario") Consulta actualizarHorario(@PathVariable Long id, @Valid @RequestBody HorarioInput input, Authentication auth) {
        if (auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_PACIENTE")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el paciente puede actualizar horarios");
        Consulta c = consultas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!c.getPacienteNombre().equalsIgnoreCase(auth.getName())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cita no pertenece al paciente");
        if (!input.fin().isAfter(input.inicio())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora final debe ser posterior a la inicial");
        c.setInicio(input.inicio()); c.setFin(input.fin()); return consultas.save(c);
    }

    @Operation(summary = "Asignar consulta", description = "Crea una consulta y la asigna al médico indicado. El pacienteNombre debe coincidir con el usuario de acceso del paciente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Consulta creada y asignada."),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, consultorio fuera de 101 a 999 o horario incorrecto."),
            @ApiResponse(responseCode = "401", description = "Falta autenticación HTTP Basic."),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es administrador."),
            @ApiResponse(responseCode = "404", description = "No existe el médico indicado.")})
    @Transactional
    @PostMapping("/consultas") @ResponseStatus(HttpStatus.CREATED)
    Consulta crearConsulta(@Valid @RequestBody ConsultaInput input) {
        if (!input.fin().isAfter(input.inicio())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora final debe ser posterior a la inicial");
        Medico m = medicos.findById(input.medicoId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médico inexistente"));
        Consulta c = new Consulta(); c.setPacienteNombre(input.pacienteNombre()); c.setMotivo(input.motivo()); c.setConsultorio(input.consultorio());
        c.setInicio(input.inicio()); c.setFin(input.fin()); c.setMedico(m); return consultas.save(c);
    }
}
