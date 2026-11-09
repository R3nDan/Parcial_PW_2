# Parcial — gestión de consultas médicas

Aplicación web Spring Boot para registrar médicos, asignar consultas y mostrar la agenda según el rol de cada usuario.

## Ejecutar localmente

Requiere Java 21 y MySQL Server. Ejecuta `database/create-database.sql` para crear `pw_parcial2`; JPA crea las tablas al iniciar y `DataInitializer` inserta datos de demostración si no existen usuarios. Configura la contraseña local de MySQL en `DB_PASSWORD` en la configuración de ejecución de Spring Boot de Eclipse. El usuario predeterminado es `root`; se puede cambiar con `DB_USERNAME`. El proyecto no guarda la contraseña en sus archivos.

Desde esta carpeta ejecuta (definiendo `DB_PASSWORD` en el entorno antes):

```powershell
.\mvnw.cmd spring-boot:run
```

Abre `http://localhost:8080`. La primera ejecución carga cuentas de demostración en `pw_parcial2`:

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | `admin` | `Admin123!` |
| Médico | `dra.laura` | `Medico123!` |
| Paciente | `paciente` | `Paciente123!` |

Las cuentas iniciales que falten se insertan al iniciar. Desde el panel de administración se crean médicos con su usuario y contraseña, y también accesos de pacientes. Para asociar un acceso de paciente a sus consultas, usa como usuario el mismo nombre que ingreses al registrar la consulta.

## Funcionalidad por rol

- **Administrador:** crea, edita y elimina médicos; registra consultas y las asigna a un médico; consulta el directorio y las citas.
- **Médico:** visualiza exclusivamente las consultas asignadas a su cuenta.
- **Paciente:** visualiza y actualiza el horario de sus propias citas, asociadas al nombre de usuario con el que inicia sesión. El servidor comprueba que la cita pertenezca a esa cuenta.

El formulario valida el nombre del paciente (máximo 40 caracteres alfanuméricos), motivo (máximo 100), consultorio entre 101 y 999 y que el fin de la cita sea posterior al inicio.

## API y documentación

La documentación interactiva está en `http://localhost:8080/swagger-ui.html` y el esquema OpenAPI en `http://localhost:8080/v3/api-docs`.

- `GET /api/medicos`, `POST /api/medicos`, `PUT /api/medicos/{id}`, `DELETE /api/medicos/{id}`: administrador.
- `GET /api/consultas`: citas visibles al usuario autenticado según su rol.
- `PUT /api/consultas/{id}/horario`: paciente; requiere `inicio` y `fin`, y solo modifica una cita asociada al usuario autenticado.
- `POST /api/consultas`: administrador; requiere `pacienteNombre`, `motivo`, `consultorio`, `inicio`, `fin` y `medicoId`.

Los endpoints documentan respuestas de éxito, `401 Unauthorized` y `403 Forbidden`. Las contraseñas se guardan con BCrypt.

## Pruebas

Ejecuta `.\mvnw.cmd test`. Las pruebas usan una base H2 temporal y comprueban acceso no autenticado, restricción por rol y acceso autorizado al endpoint de médicos. No necesitan conectarse a MySQL ni modifican la base local.
