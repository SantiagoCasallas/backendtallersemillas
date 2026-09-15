package com.tallersemillas.backend.infrastructure.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.UUID;
public interface StudentJpaRepository extends JpaRepository<StudentEntity,UUID> {
    Page<StudentEntity> findByGuardianId(UUID guardianId, Pageable pageable);
}
