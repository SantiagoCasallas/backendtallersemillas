package com.tallersemillas.backend.application.port.outbound;

import com.tallersemillas.backend.domain.Account;
import java.util.*;
public interface Accounts {
    Optional<Account> findByEmail(String email);
    Account save(Account account);
    Optional<Account> findById(UUID id);
}
