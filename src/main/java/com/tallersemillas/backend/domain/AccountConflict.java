package com.tallersemillas.backend.domain;

public class AccountConflict extends RuntimeException {
    public AccountConflict() { super("No se pudo crear la cuenta con los datos proporcionados"); }
}