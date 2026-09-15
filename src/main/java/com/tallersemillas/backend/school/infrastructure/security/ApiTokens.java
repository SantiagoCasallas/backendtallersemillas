package com.tallersemillas.backend.school.infrastructure.security;

import com.tallersemillas.backend.application.port.outbound.Accounts;
import com.tallersemillas.backend.infrastructure.adapter.inbound.security.AccountPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.security.*;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
@Component
public class ApiTokens {
 private final JdbcTemplate jdbc; private final Accounts accounts; private final SecureRandom random=new SecureRandom();
 public ApiTokens(JdbcTemplate jdbc,Accounts accounts) { this.jdbc=jdbc; this.accounts=accounts; }
 private String hash(String token) {
  try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
  catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
 }
 public String issue(UUID account) {
  byte[] bytes=new byte[32]; random.nextBytes(bytes); String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  jdbc.update("DELETE FROM api_tokens WHERE expires_at <= CURRENT_TIMESTAMP");
  jdbc.update("INSERT INTO api_tokens(token_hash,account_id,expires_at) VALUES (?,?,?)",hash(token),account,java.sql.Timestamp.from(Instant.now().plus(Duration.ofHours(2))));
  return token;
 }
 public Optional<AccountPrincipal> resolve(String token) {
  if(token==null || token.length()!=43) return Optional.empty();
  return jdbc.queryForList("SELECT a.id FROM api_tokens t JOIN accounts a ON a.id=t.account_id WHERE t.token_hash=? AND t.expires_at>CURRENT_TIMESTAMP",UUID.class,hash(token))
   .stream().findFirst().flatMap(accounts::findById).map(AccountPrincipal::new);
 }
 public Map<String,Object> timestamps(UUID id) { return jdbc.queryForMap("SELECT created_at,updated_at FROM accounts WHERE id=?",id); }
 public void revoke(String token) { jdbc.update("DELETE FROM api_tokens WHERE token_hash=?",hash(token)); }
}
