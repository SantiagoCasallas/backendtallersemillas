package com.tallersemillas.backend.infrastructure.adapter.inbound.security;

import com.tallersemillas.backend.domain.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;
public record AccountPrincipal(UUID id, String name, String email, String passwordHash, Role role) implements UserDetails {
    public AccountPrincipal(Account a) { this(a.id(),a.name(),a.email(),a.passwordHash(),a.role()); }
    public Actor actor() { return new Actor(id,role); }
    public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_"+role)); }
    public String getUsername() { return email; }
    public String getPassword() { return passwordHash; }
}
