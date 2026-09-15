package com.tallersemillas.backend.school.infrastructure.web;

import com.tallersemillas.backend.application.port.inbound.RegisterAccount;
import com.tallersemillas.backend.application.port.outbound.Accounts;
import com.tallersemillas.backend.infrastructure.adapter.inbound.security.AccountPrincipal;
import com.tallersemillas.backend.school.infrastructure.security.ApiTokens;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/auth")
public class FrontendAuthController {
 private final Accounts accounts; private final RegisterAccount registration; private final PasswordEncoder passwords; private final ApiTokens tokens;
 private final String dummy;
 public FrontendAuthController(Accounts accounts,RegisterAccount registration,PasswordEncoder passwords,ApiTokens tokens) {
  this.accounts=accounts;this.registration=registration;this.passwords=passwords;this.tokens=tokens;
  dummy=passwords.encode(UUID.randomUUID().toString());
 }
 public record Credentials(@NotBlank @Size(max=254) String nombreUsuario,@NotNull @Size(min=1,max=128) String password) {}
 public record Registration(@NotBlank @Size(max=120) String name,@NotBlank @Email @Size(max=254) String email,@NotNull @Size(min=12,max=128) String password) {}
 private Map<String,Object> user(AccountPrincipal p) {
  String[] name=p.name().split(" ",2);
  return Map.of("id",p.id(),"nombreUsuario",p.email(),"rol",p.role().name().equals("ADMINISTRADOR") ? "administrador" : p.role().name().equals("DIRECTIVO") ? "directivo" : "usuario",
   "role",p.role(),"activo",true,"name",p.name(),"createdAt",tokens.timestamps(p.id()).get("created_at").toString(),"updatedAt",tokens.timestamps(p.id()).get("updated_at").toString(),"persona",Map.of("primerNombre",name[0],"primerApellido",name.length>1 ? name[1] : "","correoElectronico",p.email()));
 }
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Credentials credentials) {
  var account=accounts.findByEmail(credentials.nombreUsuario().strip().toLowerCase(Locale.ROOT));
  boolean matches=passwords.matches(credentials.password(),account.map(a->a.passwordHash()).orElse(dummy));
  if(account.isEmpty() || !matches) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales invÃ¡lidas");
  var principal=new AccountPrincipal(account.get());
  return Map.of("token",tokens.issue(principal.id()),"tokenType","Bearer","expiresIn",7200,"user",user(principal));
 }
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> register(@Valid @RequestBody Registration data) {
  return user(new AccountPrincipal(registration.register(data.name(),data.email(),data.password())));
 }
 @GetMapping("/me") public Map<String,Object> me(@AuthenticationPrincipal AccountPrincipal principal) { return user(principal); }
 @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) public void logout(@RequestHeader("Authorization") String authorization) { tokens.revoke(authorization.substring(7)); }
}
