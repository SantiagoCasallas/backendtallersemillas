package com.tallersemillas.backend.infrastructure.adapter.outbound.persistence;

import com.tallersemillas.backend.application.port.outbound.Accounts;
import com.tallersemillas.backend.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;
@Repository
public class AccountPersistence implements Accounts {
    private final AccountJpaRepository repository;
    private final com.tallersemillas.backend.school.infrastructure.privacy.DataCipher cipher;
    public AccountPersistence(AccountJpaRepository repository,com.tallersemillas.backend.school.infrastructure.privacy.DataCipher cipher) { this.repository=repository; this.cipher=cipher; }
    public Optional<Account> findByEmail(String email) { return repository.findByEmailLookup(cipher.index(email)).map(AccountEntity::domain); }
    public Optional<Account> findById(UUID id) { return repository.findById(id).map(AccountEntity::domain); }
    public Account save(Account account) {
        try { var entity=new AccountEntity(account); entity.emailLookup=cipher.index(account.email()); return repository.saveAndFlush(entity).domain(); }
        catch (DataIntegrityViolationException ex) { throw new AccountConflict(); }
    }
}
