package com.tallersemillas.backend.domain;

import java.util.UUID;
public record Account(UUID id, String name, String email, String passwordHash, Role role) {}
