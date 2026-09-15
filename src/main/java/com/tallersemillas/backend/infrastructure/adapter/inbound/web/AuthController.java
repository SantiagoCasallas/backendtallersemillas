package com.tallersemillas.backend.infrastructure.adapter.inbound.web;

import com.tallersemillas.backend.application.port.inbound.RegisterAccount;
import com.tallersemillas.backend.domain.*;
import com.tallersemillas.backend.infrastructure.adapter.inbound.security.AccountPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final RegisterAccount registerAccount;
    public AuthController(RegisterAccount registerAccount) { this.registerAccount=registerAccount; }
    public record Registration(@NotBlank @Size(max=120) String name, @NotBlank @Email @Size(max=254) String email,
        @NotNull @Size(min=12,max=128) String password) {}
    public record AccountResponse(UUID id,String name,String email,Role role) {}
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse register(@Valid @RequestBody Registration request) {
        Account a=registerAccount.register(request.name(),request.email(),request.password());
        return new AccountResponse(a.id(),a.name(),a.email(),a.role());
    }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @GetMapping("/me") public AccountResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
        return new AccountResponse(principal.id(),principal.name(),principal.email(),principal.role());
    }
}
