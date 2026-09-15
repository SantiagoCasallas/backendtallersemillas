package com.tallersemillas.backend.application.port.inbound;

import com.tallersemillas.backend.domain.*;
import java.time.LocalDate;
import java.util.*;
public interface ManageStudents {
    Student register(Actor actor, String fullName, LocalDate birthDate);
    Student get(Actor actor, UUID id);
    List<Student> list(Actor actor, int page, int size);
}
