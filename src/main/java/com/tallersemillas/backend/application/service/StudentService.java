package com.tallersemillas.backend.application.service;

import com.tallersemillas.backend.application.port.inbound.ManageStudents;
import com.tallersemillas.backend.application.port.outbound.Students;
import com.tallersemillas.backend.domain.*;
import java.time.*;
import java.util.*;
public final class StudentService implements ManageStudents {
    private final Students students;
    private final Clock clock;
    public StudentService(Students students, Clock clock) { this.students = students; this.clock = clock; }
    public Student register(Actor actor, String name, LocalDate birthDate) {
        return students.save(Student.register(name, birthDate, actor.id(), clock));
    }
    public Student get(Actor actor, UUID id) {
        Student student = students.findById(id).orElseThrow(NotFound::new);
        if (!actor.isAdmin() && !student.guardianId().equals(actor.id())) throw new NotFound();
        return student;
    }
    public List<Student> list(Actor actor, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) throw new IllegalArgumentException("Paginación inválida");
        return students.list(actor.isAdmin() ? null : actor.id(), page, size);
    }
}
