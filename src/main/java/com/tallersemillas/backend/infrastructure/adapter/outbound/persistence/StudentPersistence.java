package com.tallersemillas.backend.infrastructure.adapter.outbound.persistence;

import com.tallersemillas.backend.application.port.outbound.Students;
import com.tallersemillas.backend.domain.Student;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.*;
import java.util.*;
@Repository
public class StudentPersistence implements Students {
    private final StudentJpaRepository repository;
    public StudentPersistence(StudentJpaRepository repository) { this.repository=repository; }
    public Student save(Student s) { return repository.saveAndFlush(new StudentEntity(s)).domain(); }
    public Optional<Student> findById(UUID id) { return repository.findById(id).map(StudentEntity::domain); }
    public List<Student> list(UUID guardianId,int page,int size) {
        Pageable pageable=PageRequest.of(page,size,Sort.by("createdAt").descending().and(Sort.by("id")));
        return (guardianId==null ? repository.findAll(pageable) : repository.findByGuardianId(guardianId,pageable))
            .stream().map(StudentEntity::domain).toList();
    }
}
