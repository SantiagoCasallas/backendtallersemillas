CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USUARIO', 'ADMINISTRADOR'))
);
CREATE TABLE students (
    id UUID PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    birth_date DATE NOT NULL,
    guardian_id UUID NOT NULL REFERENCES accounts(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_students_guardian ON students(guardian_id);
