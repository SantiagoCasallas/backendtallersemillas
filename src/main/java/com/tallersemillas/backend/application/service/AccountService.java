package com.tallersemillas.backend.application.service;

import com.tallersemillas.backend.application.port.inbound.RegisterAccount;
import com.tallersemillas.backend.application.port.outbound.*;
import com.tallersemillas.backend.domain.*;
import java.util.*;
public final class AccountService implements RegisterAccount {
    private final Accounts accounts;
    private final Passwords passwords;
    public AccountService(Accounts accounts, Passwords passwords) { this.accounts = accounts; this.passwords = passwords; }
    public Account register(String name, String email, String password) {
        if (name == null || name.isBlank() || name.strip().length() > 120)
            throw new IllegalArgumentException("Nombre inválido");
        if (email == null || email.strip().length() > 254 || !email.strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Correo inválido");
        if (password == null || password.length() < 12 || password.length() > 128)
            throw new IllegalArgumentException("La contraseña debe tener entre 12 y 128 caracteres");
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (accounts.findByEmail(normalized).isPresent()) throw new AccountConflict();
        return accounts.save(new Account(UUID.randomUUID(), name.strip(), normalized, passwords.hash(password), Role.USUARIO));
    }
}
