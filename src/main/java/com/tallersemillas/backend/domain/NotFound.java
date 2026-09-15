package com.tallersemillas.backend.domain;

public class NotFound extends RuntimeException {
    public NotFound() { super("Recurso no encontrado"); }
}