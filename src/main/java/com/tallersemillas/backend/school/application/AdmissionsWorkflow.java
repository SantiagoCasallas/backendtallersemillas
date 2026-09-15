package com.tallersemillas.backend.school.application;

import com.tallersemillas.backend.school.application.port.*;
import com.tallersemillas.backend.school.domain.*;
import com.tallersemillas.backend.domain.NotFound;
import java.time.*;
import java.util.*;
import static com.tallersemillas.backend.school.domain.RegistryKind.*;
import static com.tallersemillas.backend.school.domain.Fields.*;
public final class AdmissionsWorkflow {
 private final SchoolRepository repo; private final SchoolManagement school; private final Transactions tx; private final LinkTokens tokens; private final Clock clock;
 public AdmissionsWorkflow(SchoolRepository repo,SchoolManagement school,Transactions tx,LinkTokens tokens,Clock clock) { this.repo=repo;this.school=school;this.tx=tx;this.tokens=tokens;this.clock=clock; }
 private long rid(Map<String,Object> value) { return id(value,"id"); }
 public Map<String,Object> result(long interview,boolean attended,boolean proceed) { return tx.required(()->{
  repo.lock(INTERVIEW,interview); var prior=repo.get(INTERVIEW,interview);
  if(!text(prior,"estado_enum").equals("AGENDADA")) throw new SchoolConflict("La entrevista ya tiene resultado");
  var saved=school.attendance(interview,attended);
  saved=repo.update(INTERVIEW,interview,merge(saved,Map.of("continuar",attended && proceed)));
  var response=new LinkedHashMap<String,Object>(); response.put("interview",saved);
  if(attended && proceed) response.put("invitation",issue(interview));
  return response;
 }); }
 public Map<String,Object> issue(long interview) { return tx.required(()->{
  repo.lock(INTERVIEW,interview); var visit=repo.get(INTERVIEW,interview);
  if(!text(visit,"estado_enum").equals("REALIZADA") || !bool(visit,"continuar",false)) throw new SchoolConflict("La visita debe estar realizada con decisión de continuar");
  if(repo.list(INSCRIPTION).stream().anyMatch(i->i.containsKey("id_entrevista") && id(i,"id_entrevista")==interview)) throw new SchoolConflict("Esta visita ya tiene una preinscripción");
  for(var previous:repo.list(INVITATION)) if(id(previous,"id_entrevista")==interview) repo.update(INVITATION,rid(previous),merge(previous,Map.of("revoked",true)));
  String token=tokens.generate(); String expiry=clock.instant().plus(Duration.ofDays(7)).toString();
  repo.insert(INVITATION,Map.of("id_entrevista",interview,"token_hash",tokens.hash(token),"expires_at",expiry,"used",false,"revoked",false));
  return Map.of("token",token,"expires_at",expiry);
 }); }
 public Map<String,Object> invitation(String token) {
  if(token==null || token.length()!=43) throw new NotFound();
  var found=repo.list(INVITATION).stream().filter(i->text(i,"token_hash").equals(tokens.hash(token))).findFirst().orElseThrow(NotFound::new);
  if(bool(found,"used",false) || bool(found,"revoked",false) || !Instant.parse(text(found,"expires_at")).isAfter(clock.instant())) throw new SchoolConflict("El enlace venció o ya fue utilizado");
  return found;
 }
 public Map<String,Object> context(String token) {
  var invitation=invitation(token); var visit=repo.get(INTERVIEW,id(invitation,"id_entrevista"));
  return Map.of("expires_at",invitation.get("expires_at"),"informacion_aspirante",visit.get("informacion_aspirante"),"form_schema",school.schema("inscripcion"));
 }
 public Map<String,Object> enroll(String token,Map<String,Object> data,long schemaVersion,boolean consent) { return tx.required(()->{
  var invitation=invitation(token); repo.lock(INVITATION,rid(invitation)); invitation=invitation(token);
  if(!consent) throw new IllegalArgumentException("Debes aceptar el tratamiento de datos para enviar la solicitud");
  if(number(school.schema("inscripcion"),"version")!=schemaVersion) throw new SchoolConflict("El formulario cambió. Recarga el enlace");
  var inscription=school.enroll(data);
  var updated=repo.update(INSCRIPTION,rid(inscription),merge(inscription,Map.of("id_entrevista",id(invitation,"id_entrevista"),"admission_stage","PREINSCRITO","consent_at",clock.instant().toString())));
  for(var photo:repo.list(PHOTO)) if(photo.containsKey("invitation_id") && id(photo,"invitation_id")==rid(invitation)) repo.update(PHOTO,rid(photo),merge(photo,Map.of("id_estudiante",id(inscription,"id_estudiante"))));
  repo.update(INVITATION,rid(invitation),merge(invitation,Map.of("used",true,"inscription_id",rid(inscription))));
  return updated;
 }); }
 public Map<String,Object> reschedule(long interview,Long slotId) { return tx.required(()->{
  repo.lock(INTERVIEW,interview); var current=repo.get(INTERVIEW,interview);
  if(!text(current,"estado_enum").equals("AGENDADA")) throw new SchoolConflict("Solo se pueden modificar entrevistas agendadas");
  long old=id(current,"id_franja_entrevista");
  if(slotId==null) { repo.lock(SLOT,old); return repo.update(INTERVIEW,interview,merge(current,Map.of("estado_enum","CANCELADA"))); }
  repo.lock(SLOT,Math.min(old,slotId)); if(old!=slotId) repo.lock(SLOT,Math.max(old,slotId));
  if(old==slotId) return current;
  var slot=school.slot(slotId);
  if(number(slot,"cupos_disponibles")<1 || !OffsetDateTime.parse(text(slot,"franja")).toInstant().isAfter(clock.instant())) throw new SchoolConflict("La nueva franja no está disponible");
  return repo.update(INTERVIEW,interview,merge(current,Map.of("id_franja_entrevista",slotId)));
 }); }
 public List<Map<String,Object>> observations(long student) { repo.get(STUDENT,student); return repo.list(OBSERVATION).stream().filter(o->id(o,"id_estudiante")==student).toList(); }
 public Map<String,Object> observe(long student,String description,String actor) { return tx.required(()->{
  repo.get(STUDENT,student); required(Map.of("description",description),"description",5000);
  return repo.insert(OBSERVATION,Map.of("id_estudiante",student,"description",description.strip(),"author_id",actor));
 }); }
 public Map<String,Object> formalize(long inscription,int year,String actor) { return tx.required(()->{
  repo.lock(INSCRIPTION,inscription); var current=repo.get(INSCRIPTION,inscription);
  if(!text(current,"estado").equals("APROBADA")) throw new SchoolConflict("Primero debes aceptar la preinscripción");
  var enrollment=renew(id(current,"id_estudiante"),year,actor);
  repo.update(INSCRIPTION,inscription,merge(current,Map.of("admission_stage","MATRICULADO","school_year",year)));
  return enrollment;
 }); }
 public Map<String,Object> renew(long student,int year,String actor) { return tx.required(()->{
  repo.lock(STUDENT,student); var profile=repo.get(STUDENT,student);
  int current=LocalDate.now(clock).getYear(); if(year<current || year>current+1) throw new IllegalArgumentException("Selecciona el año actual o el siguiente");
  var existing=repo.list(YEAR_ENROLLMENT).stream().filter(e->id(e,"id_estudiante")==student && number(e,"school_year")==year).findFirst();
  if(existing.isPresent()) return existing.get();
  boolean approved=repo.list(INSCRIPTION).stream().anyMatch(i->id(i,"id_estudiante")==student && text(i,"estado").equals("APROBADA"));
  if(!text(profile,"estado").equals("ACTIVO") && !approved) throw new SchoolConflict("Se requiere estudiante activo o preinscripción aceptada");
  if(school.guardians(student).isEmpty()) throw new SchoolConflict("El estudiante debe tener un acudiente");
  var enrollment=repo.insert(YEAR_ENROLLMENT,Map.of("id_estudiante",student,"school_year",year,"author_id",actor));
  school.saveStudent(student,Map.of("estado","ACTIVO")); return enrollment;
 }); }
 public List<Map<String,Object>> years(long student) { repo.get(STUDENT,student); return repo.list(YEAR_ENROLLMENT).stream().filter(e->id(e,"id_estudiante")==student).toList(); }
 public Map<String,Object> document(long inscription) {
  var i=repo.get(INSCRIPTION,inscription); return merge(i,Map.of("estudiante",school.student(id(i,"id_estudiante")),"acudientes",school.guardians(id(i,"id_estudiante"))));
 }
}
