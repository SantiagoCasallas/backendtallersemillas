package com.tallersemillas.backend.domain;

import java.time.*;
import java.util.UUID;
public record Student(UUID id, String fullName, LocalDate birthDate, UUID guardianId, Instant createdAt) {
    public static Student register(String fullName, LocalDate birthDate, UUID guardianId, Clock clock) {
        if (fullName == null || fullName.isBlank() || fullName.strip().length() > 160)
            throw new IllegalArgumentException("El nombre del alumno es obligatorio y admite hasta 160 caracteres");
        if (birthDate == null || !birthDate.isBefore(LocalDate.now(clock)))
            throw new IllegalArgumentException("La fecha de nacimiento debe ser anterior a hoy");
        if (guardianId == null) throw new IllegalArgumentException("El acudiente es obligatorio");
        return new Student(UUID.randomUUID(), fullName.strip(), birthDate, guardianId, clock.instant());
    }
}
