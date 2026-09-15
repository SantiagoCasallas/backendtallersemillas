package com.tallersemillas.backend.application.port.outbound;

import com.tallersemillas.backend.domain.Student;
import java.util.*;
public interface Students {
    Student save(Student student);
    Optional<Student> findById(UUID id);
    List<Student> list(UUID guardianId, int page, int size);
}
