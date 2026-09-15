package com.tallersemillas.backend.infrastructure.adapter.inbound.web;

import com.tallersemillas.backend.application.port.inbound.ManageStudents;
import com.tallersemillas.backend.domain.Student;
import com.tallersemillas.backend.infrastructure.adapter.inbound.security.AccountPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.net.URI;
import java.util.*;
@RestController @RequestMapping("/api/students")
public class StudentController {
    private final ManageStudents students;
    public StudentController(ManageStudents students) { this.students=students; }
    public record Registration(@NotBlank @Size(max=160) String fullName,@NotNull @Past LocalDate birthDate) {}
    public record StudentResponse(UUID id,String fullName,LocalDate birthDate,UUID guardianId,Instant createdAt) {
        static StudentResponse from(Student s) { return new StudentResponse(s.id(),s.fullName(),s.birthDate(),s.guardianId(),s.createdAt()); }
    }
    @PostMapping public ResponseEntity<StudentResponse> register(@AuthenticationPrincipal AccountPrincipal principal,@Valid @RequestBody Registration request) {
        var student=students.register(principal.actor(),request.fullName(),request.birthDate());
        return ResponseEntity.created(URI.create("/api/students/"+student.id())).body(StudentResponse.from(student));
    }
    @GetMapping public List<StudentResponse> list(@AuthenticationPrincipal AccountPrincipal principal,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return students.list(principal.actor(),page,size).stream().map(StudentResponse::from).toList();
    }
    @GetMapping("/{id}") public StudentResponse get(@AuthenticationPrincipal AccountPrincipal principal,@PathVariable UUID id) {
        return StudentResponse.from(students.get(principal.actor(),id));
    }
}
