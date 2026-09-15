package com.tallersemillas.backend.infrastructure.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AccountJpaRepository extends JpaRepository<AccountEntity,UUID> { Optional<AccountEntity> findByEmailLookup(String emailLookup); }
