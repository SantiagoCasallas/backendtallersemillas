package com.tallersemillas.backend;

import com.tallersemillas.backend.application.port.inbound.RegisterAccount;
import com.tallersemillas.backend.application.port.outbound.Accounts;
import com.tallersemillas.backend.domain.*;
import com.tallersemillas.backend.school.application.port.SchoolManagement;
import com.tallersemillas.backend.school.domain.SchoolConflict;
import com.tallersemillas.backend.school.infrastructure.web.FrontendFields;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class FrontendApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired Accounts accounts;
 @Autowired RegisterAccount registration; @Autowired SchoolManagement school; @Autowired JdbcTemplate jdbc;
 private static final String PASSWORD="Contrasena vÃ¡lida de pruebas 2026";
 private String unique() { return UUID.randomUUID().toString(); }
 private String token(boolean admin) throws Exception {
  var a=registration.register("Nombre Apellido",unique()+"@example.test",PASSWORD);
  if(admin) accounts.save(new Account(a.id(),a.name(),a.email(),a.passwordHash(),Role.ADMINISTRADOR));
  var result=mvc.perform(post("/api/v1/auth/login").contentType("application/json")
   .content(json.writeValueAsString(Map.of("nombreUsuario",a.email(),"password",PASSWORD))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.user.persona.primerNombre").value("Nombre")).andReturn();
  return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
 }
 private Map<String,Object> studentPayload() {
  return new LinkedHashMap<>(Map.of("firstname","NiÃ±a","lastname","Prueba","documentoIdentidad",unique(),"tipoDocumento","RC",
   "fechaNacimiento","2022-01-01","homeAddress","DirecciÃ³n de prueba","strata","2","hasLunch",true,"hasTransport",false,"status","ASPIRANTE"));
 }
 private long createStudent(String token) throws Exception {
  var result=mvc.perform(post("/api/v1/students").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(studentPayload())))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.primer_nombre").value("NiÃ±a")).andExpect(jsonPath("$.estrato").value(2)).andReturn();
  return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
 }
 @Test void frontendLoginTokenRevocationAndRoleBoundary() throws Exception {
  String normal=token(false),admin=token(true);
  mvc.perform(get("/api/v1/students/all")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/students/all").header("Authorization","Bearer "+normal)).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/students/all").header("Authorization","Bearer dev-token-123")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/students/all").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.students").isArray());
  mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+admin)).andExpect(jsonPath("$.rol").value("administrador")).andExpect(jsonPath("$.passwordHash").doesNotExist());
  mvc.perform(post("/api/v1/auth/logout").header("Authorization","Bearer "+admin)).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+admin)).andExpect(status().isUnauthorized());
 }
 @Test void studentAliasesGeneratedIdAndRetirement() throws Exception {
  String admin=token(true); long id=createStudent(admin);
  mvc.perform(patch("/api/v1/students/"+id).header("Authorization","Bearer "+admin).contentType("application/json")
   .content("{\"status\":\"ACTIVO\",\"firstname\":\"Nombre actualizado\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACTIVO"));
  mvc.perform(patch("/api/v1/students/"+id).header("Authorization","Bearer "+admin).contentType("application/json")
   .content("{\"status\":\"EGRESADO\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EGRESADO"));
  mvc.perform(get("/api/v1/students/all?status=EGRESADO").header("Authorization","Bearer "+admin)).andExpect(status().isOk());
  mvc.perform(delete("/api/v1/students/"+id).header("Authorization","Bearer "+admin)).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/students/"+id).header("Authorization","Bearer "+admin)).andExpect(jsonPath("$.estado").value("RETIRADO"));
 }
 @Test void guardiansCannotMoveBetweenStudentsAndHistoryIsUnique() throws Exception {
  String admin=token(true); long sid=createStudent(admin),other=createStudent(admin);
  var guardian=Map.<String,Object>of("id_estudiante",sid,"primer_nombre","Acudiente","primer_apellido","Prueba","documento_identidad",unique(),"tipo_documento","CC","parentesco","MADRE","es_principal_estudiante",true);
  var result=mvc.perform(post("/api/v1/parent/").header("Authorization","Bearer "+admin).contentType("application/json").content(json.writeValueAsString(guardian)))
   .andExpect(status().isCreated()).andReturn();
  long gid=json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  mvc.perform(get("/api/v1/students/estudiante/"+sid).header("Authorization","Bearer "+admin)).andExpect(jsonPath("$.acudientes[0].id").value(gid));
  mvc.perform(patch("/api/v1/parent/"+gid).header("Authorization","Bearer "+admin).contentType("application/json").content("{\"id_estudiante\":"+other+"}"))
   .andExpect(status().isBadRequest());
  var h=school.history(sid,true); assertThat(school.history(sid,true).get("id")).isEqualTo(h.get("id"));
  var entry=school.saveMedicalEntry(null,Map.of("id_informacion_medica",h.get("id"),"tipo","ALERGIA","descripcion","Entrada de prueba"));
  assertThat(school.medicalEntries(((Number)h.get("id")).longValue())).hasSize(1);
  school.removeMedicalEntry(((Number)entry.get("id")).longValue());
 }
 @Test void schemaVersionsAreImmutableAndRequiredAnswersEnforced() {
  var prior=school.schema("entrevista");
  String name="field-"+unique();
  var schema=school.publishSchema("entrevista",List.of(Map.of("id",name,"type","text","label","Pregunta","required",true)));
  assertThat(((Number)schema.get("version")).longValue()).isGreaterThan(((Number)prior.get("version")).longValue());
  var slot=school.saveSlot(null,Map.of("franja",OffsetDateTime.now().plusDays(20).toString(),"cupos_totales",2));
  var payload=new LinkedHashMap<String,Object>(Map.of("id_franja_entrevista",slot.get("id"),"schema_version",schema.get("version"),"correo",unique()+"@example.test","nom_estudiante","Prueba"));
  assertThatThrownBy(()->school.bookInterview(payload)).isInstanceOf(IllegalArgumentException.class);
  payload.put("informacion_aspirante",Map.of(name,"Respuesta"));
  var interview=school.bookInterview(payload);
  school.publishSchema("entrevista",List.of());
  assertThat(json.writeValueAsString(interview.get("formSchema"))).isEqualTo(json.writeValueAsString(schema));
  assertThat(school.interviews().stream().filter(i->i.get("id").equals(interview.get("id"))).findFirst().orElseThrow().get("schema_version").toString()).isEqualTo(schema.get("version").toString());
 }
 @Test void lastSlotIsReservedOnlyOnceAndOccupiedSlotCannotBeRemoved() throws Exception {
  var schema=school.publishSchema("entrevista",List.of());
  var slot=school.saveSlot(null,Map.of("franja",OffsetDateTime.now().plusDays(30).toString(),"cupos_totales",1));
  long id=((Number)slot.get("id")).longValue();
  try(var executor=Executors.newFixedThreadPool(2)) {
   var start=new CountDownLatch(1);
   Callable<Boolean> attempt=()->{start.await(); try {
    school.bookInterview(Map.of("id_franja_entrevista",id,"schema_version",schema.get("version"),"correo",unique()+"@example.test","nom_estudiante","Prueba")); return true;
   } catch(SchoolConflict ex) { return false; }};
   var one=executor.submit(attempt); var two=executor.submit(attempt); start.countDown();
   assertThat(List.of(one.get(15,TimeUnit.SECONDS),two.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
  }
  assertThatThrownBy(()->school.removeSlot(id)).isInstanceOf(SchoolConflict.class);
 }
 @Test void enrollmentIsAtomicAndApprovalIsIdempotent() throws Exception {
  school.publishSchema("inscripcion",List.of());
  var student=studentPayload();
  var enrollment=new LinkedHashMap<String,Object>(Map.of("estudiante",student,"padre",Map.of("nombre_completo","Padre Prueba","tipo_documento","CC","documento_identidad",unique())));
  String admin=token(true);
  var result=mvc.perform(post("/api/v1/inscription/create").header("Authorization","Bearer "+admin).contentType("application/json").content(json.writeValueAsString(enrollment)))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("PENDIENTE")).andExpect(jsonPath("$.informacion_form").doesNotExist()).andReturn();
  long id=json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  mvc.perform(patch("/api/v1/inscription/"+id+"/approve").header("Authorization","Bearer "+admin))
   .andExpect(status().isOk()).andExpect(jsonPath("$.estudiante.estado").value("ASPIRANTE"));
  mvc.perform(patch("/api/v1/inscription/"+id+"/approve").header("Authorization","Bearer "+admin)).andExpect(status().isOk());
  mvc.perform(delete("/api/v1/inscription/"+id+"/reject").header("Authorization","Bearer "+admin)).andExpect(status().isConflict());
  int before=jdbc.queryForObject("SELECT COUNT(*) FROM school_students",Integer.class);
  assertThatThrownBy(()->school.enroll(FrontendFields.enrollment(Map.of("estudiante",studentPayload(),"padre",Map.of("nombre_completo","InvÃ¡lido")))))
   .isInstanceOf(IllegalArgumentException.class);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM school_students",Integer.class)).isEqualTo(before);
 }
 @Test void publicSchemaSlotsAndCorsMatchFrontend() throws Exception {
  mvc.perform(get("/api/v1/interview/latest-form-schema")).andExpect(status().isOk()).andExpect(jsonPath("$.form_schema.campos").isArray());
  mvc.perform(get("/api/v1/time_slot/available_time_slots")).andExpect(status().isOk()).andExpect(jsonPath("$.franjas").isArray());
  mvc.perform(post("/api/v1/interview/schema").contentType("application/json").content("[]")).andExpect(status().isUnauthorized());
  mvc.perform(options("/api/v1/students/1").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","PATCH").header("Access-Control-Request-Headers","Authorization,Content-Type"))
   .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
 }
}
