package com.tallersemillas.backend.school.infrastructure.persistence;

import com.tallersemillas.backend.school.application.port.SchoolRepository;
import com.tallersemillas.backend.school.domain.*;
import com.tallersemillas.backend.domain.NotFound;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import tools.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.*;
@Repository
public class JdbcSchoolRepository implements SchoolRepository {
 private final JdbcTemplate jdbc; private final ObjectMapper json;
 private final com.tallersemillas.backend.school.infrastructure.privacy.DataCipher cipher;
 private final com.tallersemillas.backend.school.infrastructure.privacy.AuditLog audit;
 public JdbcSchoolRepository(JdbcTemplate jdbc,ObjectMapper json,com.tallersemillas.backend.school.infrastructure.privacy.DataCipher cipher,com.tallersemillas.backend.school.infrastructure.privacy.AuditLog audit) { this.jdbc=jdbc; this.json=json;this.cipher=cipher;this.audit=audit; }
 private String table(RegistryKind kind) { return switch(kind) {
  case STUDENT -> "school_students"; case GUARDIAN -> "school_guardians";
  case HISTORY -> "school_medical_histories"; case MEDICAL_ENTRY -> "school_medical_entries";
  case SCHEMA -> "school_schemas"; case SLOT -> "school_slots";
  case INTERVIEW -> "school_interviews"; case INSCRIPTION -> "school_inscriptions";
  case INVITATION -> "school_invitations"; case OBSERVATION -> "school_observations"; case YEAR_ENROLLMENT -> "school_year_enrollments"; case SCHEDULE -> "school_schedules"; case PHOTO -> "school_photos";
 }; }
 @SuppressWarnings("unchecked") private Map<String,Object> row(ResultSet rs,int index) throws SQLException {
  var result=new LinkedHashMap<String,Object>(json.readValue(cipher.decrypt(rs.getString("data")),Map.class));
  result.put("id",rs.getLong("id"));
  result.put("created_at",rs.getTimestamp("created_at").toInstant().toString());
  result.put("updated_at",rs.getTimestamp("updated_at").toInstant().toString()); return result;
 }
 public Map<String,Object> get(RegistryKind kind,long id) {
  return jdbc.query("SELECT * FROM "+table(kind)+" WHERE id=?",this::row,id).stream().findFirst().orElseThrow(NotFound::new);
 }
 public List<Map<String,Object>> list(RegistryKind kind) { return jdbc.query("SELECT * FROM "+table(kind)+" ORDER BY id",this::row); }
 private LinkedHashMap<String,Object> columns(RegistryKind kind,Map<String,Object> data) {
  var columns=new LinkedHashMap<String,Object>();
  var copy=new LinkedHashMap<>(data); copy.remove("id"); copy.remove("created_at"); copy.remove("updated_at");
  columns.put("data",cipher.encrypt(json.writeValueAsString(copy)));
  switch(kind) {
   case STUDENT -> { columns.put("document_type",data.get("tipo_documento")); columns.put("document_number",cipher.index(String.valueOf(data.get("documento_identidad")))); columns.put("status",data.get("estado")); }
   case GUARDIAN -> { columns.put("student_id",data.get("id_estudiante")); columns.put("document_type",data.get("tipo_documento")); columns.put("document_number",cipher.index(String.valueOf(data.get("documento_identidad")))); }
   case HISTORY -> columns.put("student_id",data.get("id_estudiante"));
   case MEDICAL_ENTRY -> columns.put("history_id",data.get("id_informacion_medica"));
   case SCHEMA -> { columns.put("form_type",data.get("formulario")); columns.put("version",data.get("version")); }
   case INTERVIEW -> { columns.put("slot_id",data.get("id_franja_entrevista")); columns.put("schema_id",data.get("schema_id")); columns.put("contact_email",cipher.index(String.valueOf(data.get("correo_form_aspirante")))); }
   case INSCRIPTION -> { columns.put("student_id",data.get("id_estudiante")); columns.put("guardian_id",data.get("id_acudiente")); columns.put("schema_id",data.get("schema_id")); }
   case INVITATION -> { columns.put("interview_id",data.get("id_entrevista")); columns.put("token_hash",data.get("token_hash")); }
   case OBSERVATION -> columns.put("student_id",data.get("id_estudiante"));
   case YEAR_ENROLLMENT -> { columns.put("student_id",data.get("id_estudiante")); columns.put("school_year",data.get("school_year")); }
   case PHOTO -> { columns.put("student_id",data.get("id_estudiante")); columns.put("invitation_id",data.get("invitation_id")); }
   default -> {}
  }
  return columns;
 }
 public Map<String,Object> insert(RegistryKind kind,Map<String,Object> fields) {
  var columns=columns(kind,fields); var keys=new GeneratedKeyHolder();
  String sql="INSERT INTO "+table(kind)+" ("+String.join(",",columns.keySet())+") VALUES ("+String.join(",",Collections.nCopies(columns.size(),"?"))+")";
  jdbc.update(connection->{ var statement=connection.prepareStatement(sql,new String[]{"id"}); int i=1; for(Object value:columns.values()) statement.setObject(i++,value); return statement; },keys);
  long id=Objects.requireNonNull(keys.getKey()).longValue(); audit.record("CREATE",kind.name(),id,fields.keySet()); return get(kind,id);
 }
 public Map<String,Object> update(RegistryKind kind,long id,Map<String,Object> fields) {
  var previous=get(kind,id); var changed=fields.keySet().stream().filter(k->!Objects.equals(fields.get(k),previous.get(k))).toList();
  var columns=columns(kind,fields); var args=new ArrayList<>(columns.values()); args.add(id);
  String assignments=String.join(",",columns.keySet().stream().map(k->k+"=?").toList());
  if(jdbc.update("UPDATE "+table(kind)+" SET "+assignments+",updated_at=CURRENT_TIMESTAMP WHERE id=?",args.toArray())!=1) throw new NotFound();
  audit.record("UPDATE",kind.name(),id,changed); return get(kind,id);
 }
 public void delete(RegistryKind kind,long id) { if(jdbc.update("DELETE FROM "+table(kind)+" WHERE id=?",id)!=1) throw new NotFound(); audit.record("DELETE",kind.name(),id,List.of()); }
 public void lock(RegistryKind kind,long id) {
  if(jdbc.queryForList("SELECT id FROM "+table(kind)+" WHERE id=? FOR UPDATE",id).isEmpty()) throw new NotFound();
 }
 public void lockSchema(String type) { jdbc.queryForList("SELECT lock_key FROM school_locks WHERE lock_key=? FOR UPDATE","schema:"+type); }
}
