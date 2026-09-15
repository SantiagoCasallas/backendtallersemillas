package com.tallersemillas.backend.application.port.inbound;

import com.tallersemillas.backend.domain.Account;
public interface RegisterAccount { Account register(String name, String email, String password); }
