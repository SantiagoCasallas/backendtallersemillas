package com.tallersemillas.backend.infrastructure.adapter.outbound.persistence;

import com.tallersemillas.backend.domain.Student;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity @Table(name="students")
public class StudentEntity {
    @Id UUID id;
    @Convert(converter=com.tallersemillas.backend.school.infrastructure.privacy.EncryptedText.class) @Column(name="full_name",nullable=false,columnDefinition="text") String fullName;
    @Column(name="birth_date",nullable=false) LocalDate birthDate;
    @Column(name="guardian_id",nullable=false) UUID guardianId;
    @Column(name="created_at",nullable=false) Instant createdAt;
    protected StudentEntity() {}
    StudentEntity(Student s) { id=s.id(); fullName=s.fullName(); birthDate=s.birthDate(); guardianId=s.guardianId(); createdAt=s.createdAt(); }
    Student domain() { return new Student(id,fullName,birthDate,guardianId,createdAt); }
}
