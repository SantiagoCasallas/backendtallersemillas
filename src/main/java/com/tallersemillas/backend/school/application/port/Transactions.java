package com.tallersemillas.backend.school.application.port;

import java.util.function.Supplier;
public interface Transactions { <T> T required(Supplier<T> work); }
