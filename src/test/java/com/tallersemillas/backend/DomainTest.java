package com.tallersemillas.backend;
import com.tallersemillas.backend.domain.Student;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class DomainTest {
    private final Clock clock=Clock.fixed(Instant.parse("2026-09-04T12:00:00Z"),ZoneOffset.UTC);
    @Test void birthDateMustBeInPastWithoutSpring() {
        assertThatThrownBy(() -> Student.register("Alumno",LocalDate.of(2026,9,4),UUID.randomUUID(),clock))
            .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void studentHasNormalizedNameAndCreationDate() {
        var student=Student.register("  Alumno  ",LocalDate.of(2022,1,1),UUID.randomUUID(),clock);
        assertThat(student.fullName()).isEqualTo("Alumno");
        assertThat(student.createdAt()).isEqualTo(clock.instant());
    }
}
