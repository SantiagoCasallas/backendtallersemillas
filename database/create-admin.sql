-- Crea la primera cuenta administradora en Supabase.
-- Sustituye los tres valores marcados con CAMBIA_ antes de ejecutar este script.
-- Ejecuta este archivo después de supabase-schema.sql.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    admin_name TEXT := 'CAMBIA_NOMBRE_ADMINISTRADOR';
    admin_email TEXT := 'CAMBIA_CORREO@ejemplo.com';
    admin_password TEXT := 'CAMBIA_ESTA_CONTRASENA_POR_UNA_DE_AL_MENOS_12_CARACTERES';
BEGIN
    IF admin_name LIKE 'CAMBIA_%'
       OR admin_email LIKE 'CAMBIA_%'
       OR admin_password LIKE 'CAMBIA_%' THEN
        RAISE EXCEPTION 'Reemplaza los valores CAMBIA_ antes de crear la cuenta administradora';
    END IF;

    IF length(admin_password) < 12 OR length(admin_password) > 128 THEN
        RAISE EXCEPTION 'La contraseña debe tener entre 12 y 128 caracteres';
    END IF;

    INSERT INTO accounts (id, name, email, email_lookup, password_hash, role, created_at, updated_at)
    VALUES (
        gen_random_uuid(),
        admin_name,
        lower(trim(admin_email)),
        NULL,
        '{bcrypt}' || crypt(admin_password, gen_salt('bf', 12)),
        'ADMINISTRADOR',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );
END $$;

-- Al iniciar, el backend cifra name/email y completa email_lookup automáticamente.
-- No vuelvas a ejecutar este script: crea una cuenta adicional en cada ejecución.
