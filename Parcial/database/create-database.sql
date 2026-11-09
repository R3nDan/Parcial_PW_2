-- Ejecutar en MySQL antes de iniciar la aplicación.
CREATE DATABASE IF NOT EXISTS pw_parcial2
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Hibernate crea/actualiza las tablas al iniciar.
-- DataInitializer inserta las cuentas de demostración que no existan.
