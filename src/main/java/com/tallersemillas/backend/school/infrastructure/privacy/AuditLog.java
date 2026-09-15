package com.tallersemillas.backend.school.infrastructure.privacy;

import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import com.tallersemillas.backend.infrastructure.adapter.inbound.security.AccountPrincipal;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
@Component
public class AuditLog {
 private final JdbcTemplate jdbc; private final ObjectMapper json;
 public AuditLog(JdbcTemplate jdbc,ObjectMapper json) { this.jdbc=jdbc;this.json=json; }
 public String actor() {
  var auth=SecurityContextHolder.getContext().getAuthentication();
  return auth!=null && auth.getPrincipal() instanceof AccountPrincipal p ? p.id().toString() : "PUBLIC";
 }
 public void record(String operation,String resource,Object id,Collection<String> fields) {
  jdbc.update("INSERT INTO audit_events(actor,operation,resource,record_id,changed_fields) VALUES (?,?,?,?,?)",actor(),operation,resource,String.valueOf(id),json.writeValueAsString(fields));
 }
 public List<Map<String,Object>> latest() { return jdbc.queryForList("SELECT * FROM audit_events ORDER BY id DESC LIMIT 500"); }
}
