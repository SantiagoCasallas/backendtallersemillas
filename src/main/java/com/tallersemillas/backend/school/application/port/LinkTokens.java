package com.tallersemillas.backend.school.application.port;

public interface LinkTokens { String generate(); String hash(String value); }