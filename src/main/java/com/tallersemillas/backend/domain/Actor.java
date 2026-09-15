package com.tallersemillas.backend.domain;

import java.util.UUID;
public record Actor(UUID id, Role role) {
    public Actor { java.util.Objects.requireNonNull(id); java.util.Objects.requireNonNull(role); }
    public boolean isAdmin() { return role == Role.ADMINISTRADOR; }
}
