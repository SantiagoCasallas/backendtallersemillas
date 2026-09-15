package com.tallersemillas.backend.school.application;

import com.tallersemillas.backend.school.application.port.*;
import com.tallersemillas.backend.school.domain.*;
import com.tallersemillas.backend.domain.NotFound;
import java.time.*;
import java.util.*;
import static com.tallersemillas.backend.school.domain.RegistryKind.*;
import static com.tallersemillas.backend.school.domain.Fields.*;

/** Business workflows. All multi-record mutations execute through a transaction port. */
public final class SchoolService implements SchoolManagement {
 private final SchoolRepository repo; private final Transactions tx; private final Clock clock;
 public SchoolService(SchoolRepository repo,Transactions tx,Clock clock) { this.repo=repo; this.tx=tx; this.clock=clock; }
 private long recordId(Map<String,Object> value) { return id(value,"id"); }
 private Map<String,Object> defaults(Map<String,Object> input) {
  var d=new LinkedHashMap<String,Object>(); d.put("estado","ASPIRANTE"); d.put("estrato",0);
  d.put("tiene_almuerzo",false); d.put("tiene_transporte_ruta",false); d.putAll(input); return d;
 }
 public List<Map<String,Object>> students(String status,String search) {
  String q=search==null ? "" : search.toLowerCase(Locale.ROOT);
  return repo.list(STUDENT).stream().map(this::withAge).filter(s->status==null || status.isBlank() || status.equals("all") || status.equals(s.get("estado")))
   .filter(s->(text(s,"primer_nombre")+" "+text(s,"primer_apellido")+" "+text(s,"documento_identidad")).toLowerCase(Locale.ROOT).contains(q)).toList();
 }
 private Map<String,Object> withAge(Map<String,Object> profile) {
  var result=new LinkedHashMap<>(profile); String birth=text(profile,"fecha_nacimiento");
  if(!birth.isEmpty()) result.put("edad",Period.between(LocalDate.parse(birth),LocalDate.now(clock)).getYears()); return result;
 }
 public Map<String,Object> student(long id) { return withAge(repo.get(STUDENT,id)); }
 public Map<String,Object> saveStudent(Long id,Map<String,Object> data) {
  return tx.required(()->{
   if(id!=null) repo.lock(STUDENT,id);
   var fields=id==null ? defaults(data) : merge(repo.get(STUDENT,id),data);
   ProfileRules.student(fields,clock);
   if("EGRESADO".equals(fields.get("estado")) && (id==null || !"EGRESADO".equals(repo.get(STUDENT,id).get("estado")))) {
    if(id==null || !"ACTIVO".equals(repo.get(STUDENT,id).get("estado"))) throw new SchoolConflict("Solo un estudiante activo puede egresar");
    fields.put("graduation_year",LocalDate.now(clock).getYear()); fields.put("graduated_at",clock.instant().toString());
   }
   return id==null ? repo.insert(STUDENT,fields) : repo.update(STUDENT,id,fields);
  });
 }
 public void retireStudent(long id) { saveStudent(id,Map.of("estado","RETIRADO")); }
 public List<Map<String,Object>> guardians(long student) {
  repo.get(STUDENT,student); return repo.list(GUARDIAN).stream().filter(g->id(g,"id_estudiante")==student && bool(g,"activo",true)).toList();
 }
 public Map<String,Object> saveGuardian(Long id,Map<String,Object> data) {
  return tx.required(()->{
   var fields=id==null ? new LinkedHashMap<>(data) : merge(repo.get(GUARDIAN,id),data);
   long student=id(fields,"id_estudiante"); repo.lock(STUDENT,student);
   if(id!=null && id(repo.get(GUARDIAN,id),"id_estudiante")!=student) throw new IllegalArgumentException("No se permite trasladar un acudiente entre alumnos");
   ProfileRules.person(fields,clock); required(fields,"parentesco",60);
   boolean principal=bool(fields,"es_principal_estudiante",false); bool(fields,"es_responsable_pago",false);
   if(principal) for(var guardian:guardians(student)) if(id==null || recordId(guardian)!=id)
    repo.update(GUARDIAN,recordId(guardian),merge(guardian,Map.of("es_principal_estudiante",false)));
   return id==null ? repo.insert(GUARDIAN,fields) : repo.update(GUARDIAN,id,fields);
  });
 }
 public void removeGuardian(long id,long student) { tx.required(()->{
  repo.lock(STUDENT,student); if(id(repo.get(GUARDIAN,id),"id_estudiante")!=student) throw new NotFound();
  var others=guardians(student).stream().filter(g->recordId(g)!=id).toList();
  if(others.isEmpty()) throw new SchoolConflict("El estudiante debe tener al menos un acudiente");
  var removed=repo.get(GUARDIAN,id);
  repo.update(GUARDIAN,id,merge(removed,Map.of("activo",false,"es_principal_estudiante",false)));
  if(bool(removed,"es_principal_estudiante",false)) repo.update(GUARDIAN,recordId(others.getFirst()),merge(others.getFirst(),Map.of("es_principal_estudiante",true)));
  return null;
 }); }
 public Map<String,Object> history(long student,boolean create) { return tx.required(()->{
  repo.lock(STUDENT,student);
  var found=repo.list(HISTORY).stream().filter(h->id(h,"id_estudiante")==student).findFirst();
  if(found.isPresent()) return found.get(); if(!create) throw new NotFound();
  return repo.insert(HISTORY,Map.of("id_estudiante",student));
 }); }
 public List<Map<String,Object>> medicalEntries(long history) {
  repo.get(HISTORY,history); return repo.list(MEDICAL_ENTRY).stream().filter(e->id(e,"id_informacion_medica")==history).toList();
 }
 public Map<String,Object> saveMedicalEntry(Long id,Map<String,Object> data) { return tx.required(()->{
  if(id!=null) repo.lock(MEDICAL_ENTRY,id);
  var fields=id==null ? new LinkedHashMap<>(data) : merge(repo.get(MEDICAL_ENTRY,id),data);
  long history=id(fields,"id_informacion_medica"); repo.get(HISTORY,history);
  if(id!=null && id(repo.get(MEDICAL_ENTRY,id),"id_informacion_medica")!=history) throw new IllegalArgumentException("No se permite trasladar una entrada mÃ©dica");
  if(!Set.of("ALERGIA","ENFERMEDAD","ASPECTO_RELEVANTE").contains(text(fields,"tipo"))) throw new IllegalArgumentException("Tipo de entrada mÃ©dica invÃ¡lido");
  required(fields,"descripcion",5000);
  return id==null ? repo.insert(MEDICAL_ENTRY,fields) : repo.update(MEDICAL_ENTRY,id,fields);
 }); }
 public void removeMedicalEntry(long id) { tx.required(()->{ repo.delete(MEDICAL_ENTRY,id); return null; }); }

 private void formType(String type) { if(!Set.of("entrevista","inscripcion").contains(type)) throw new IllegalArgumentException("Tipo de formulario invÃ¡lido"); }
 public Map<String,Object> schema(String type) { formType(type); return tx.required(()->{
  repo.lockSchema(type);
  var latest=repo.list(SCHEMA).stream().filter(s->type.equals(s.get("formulario"))).max(Comparator.comparingLong(s->number(s,"version")));
  return latest.orElseGet(()->repo.insert(SCHEMA,Map.of("formulario",type,"version",1,"activo",true,"campos",List.of())));
 }); }
 public Map<String,Object> publishSchema(String type,List<Map<String,Object>> fields) { formType(type); return tx.required(()->{
  if(fields.size()>100) throw new IllegalArgumentException("MÃ¡ximo 100 campos");
  var normalized=new ArrayList<Map<String,Object>>(); var names=new HashSet<String>();
  for(var field:fields) {
   String name=required(field,"id",120); if(!names.add(name)) throw new IllegalArgumentException("Identificador de campo duplicado");
   String fieldType=required(field,"type",20);
   if(!Set.of("text","number","date","select","boolean","textarea").contains(fieldType)) throw new IllegalArgumentException("Tipo de campo invÃ¡lido");
   var entry=new LinkedHashMap<String,Object>(); entry.put("id",name); entry.put("label",required(field,"label",200)); entry.put("type",fieldType);
   entry.put("required",bool(field,"required",false)); entry.put("visible",bool(field,"visible",true)); entry.put("order",normalized.size()+1);
   Object options=field.get("options"); if(options!=null) {
    if(!(options instanceof List<?> list) || list.size()>100 || list.stream().anyMatch(v->!(v instanceof String s) || s.length()>200)) throw new IllegalArgumentException("Opciones invÃ¡lidas");
    entry.put("options",options);
   }
   normalized.add(entry);
  }
  repo.lockSchema(type); var previous=schema(type);
  repo.update(SCHEMA,recordId(previous),merge(previous,Map.of("activo",false)));
  return repo.insert(SCHEMA,Map.of("formulario",type,"version",number(previous,"version")+1,"activo",true,"campos",normalized));
 }); }
 private void validateAnswers(Map<String,Object> schema,Map<String,Object> answers) {
  limit(answers,0);
  for(Object raw:(List<?>)schema.get("campos")) {
   var field=object(raw); String key=text(field,"id"); Object value=answers.get(key);
   if(value==null || value instanceof String s && s.isBlank()) {
    if(bool(field,"required",false) && bool(field,"visible",true)) throw new IllegalArgumentException("Campo requerido: "+text(field,"label"));
    continue;
   }
   switch(text(field,"type")) {
    case "number" -> { try { double n=Double.parseDouble(value.toString()); if(!Double.isFinite(n)) throw new NumberFormatException(); } catch(NumberFormatException ex) { throw new IllegalArgumentException("Campo numÃ©rico invÃ¡lido: "+key); } }
    case "boolean" -> { if(!(value instanceof Boolean)) throw new IllegalArgumentException("Campo booleano invÃ¡lido: "+key); }
    case "date" -> { try { LocalDate.parse(value.toString()); } catch(RuntimeException ex) { throw new IllegalArgumentException("Fecha invÃ¡lida: "+key); } }
    default -> { if(!(value instanceof String)) throw new IllegalArgumentException("Texto invÃ¡lido: "+key); }
   }
   if("select".equals(field.get("type")) && field.get("options") instanceof List<?> options && !options.isEmpty() && !options.contains(value)) throw new IllegalArgumentException("OpciÃ³n invÃ¡lida: "+key);
  }
 }
 private long reservations(long slot) { return repo.list(INTERVIEW).stream().filter(i->id(i,"id_franja_entrevista")==slot && Set.of("AGENDADA","REALIZADA").contains(text(i,"estado_enum"))).count(); }
 private Map<String,Object> withCapacity(Map<String,Object> slot) { return merge(slot,Map.of("cupos_disponibles",number(slot,"cupos_totales")-reservations(recordId(slot)))); }
 public List<Map<String,Object>> slots(boolean availableOnly) {
  return repo.list(SLOT).stream().map(this::withCapacity)
   .filter(s->!availableOnly || number(s,"cupos_disponibles")>0 && OffsetDateTime.parse(text(s,"franja")).toInstant().isAfter(clock.instant()))
   .sorted(Comparator.comparing(s->OffsetDateTime.parse(text(s,"franja")).toInstant())).toList();
 }
 public Map<String,Object> slot(long id) { return withCapacity(repo.get(SLOT,id)); }
 public Map<String,Object> saveSlot(Long id,Map<String,Object> data) { return tx.required(()->{
  if(id!=null) repo.lock(SLOT,id);
  var fields=id==null ? new LinkedHashMap<>(data) : merge(repo.get(SLOT,id),data);
  OffsetDateTime date; try { date=OffsetDateTime.parse(required(fields,"franja",50)); } catch(java.time.format.DateTimeParseException ex) { throw new IllegalArgumentException("La franja debe incluir fecha, hora y zona horaria"); }
  if(!date.toInstant().isAfter(clock.instant())) throw new IllegalArgumentException("La franja debe estar en el futuro");
  long capacity=number(fields,"cupos_totales"); if(capacity<1 || capacity>1000) throw new IllegalArgumentException("Cupos entre 1 y 1000");
  if(id!=null) {
   long reserved=reservations(id); if(capacity<reserved) throw new SchoolConflict("Hay mÃ¡s reservas que los cupos solicitados");
   if(reserved>0 && !text(repo.get(SLOT,id),"franja").equals(text(fields,"franja"))) throw new SchoolConflict("No se puede mover una franja con reservas");
  }
  return withCapacity(id==null ? repo.insert(SLOT,fields) : repo.update(SLOT,id,fields));
 }); }
 public void removeSlot(long id) { tx.required(()->{
  repo.lock(SLOT,id); if(reservations(id)>0) throw new SchoolConflict("La franja tiene entrevistas agendadas"); repo.delete(SLOT,id); return null;
 }); }
 public Map<String,Object> bookInterview(Map<String,Object> data) { return tx.required(()->{
  long slotId=id(data,"id_franja_entrevista"); repo.lock(SLOT,slotId); var slot=slot(slotId);
  if(number(slot,"cupos_disponibles")<1) throw new SchoolConflict("La franja ya no tiene cupos");
  if(!OffsetDateTime.parse(text(slot,"franja")).toInstant().isAfter(clock.instant())) throw new SchoolConflict("La franja ya pasÃ³");
  var schema=schema("entrevista");
  if(number(data,"schema_version")!=number(schema,"version")) throw new SchoolConflict("El formulario cambiÃ³; recarga antes de enviar");
  var info=optionalObject(data.get("informacion_aspirante"));
  for(String key:List.of("nom_padre","nom_madre","correo","celular","nom_estudiante","edad_estudiante","como_conocimiento")) if(data.containsKey(key)) info.put(key,data.get(key));
  String email=required(info,"correo",254).toLowerCase(Locale.ROOT); email(email); info.put("correo",email);
  required(info,"nom_estudiante",200); validateAnswers(schema,info);
  if(info.get("edad_estudiante")!=null) { long age=number(info,"edad_estudiante"); if(age<0 || age>18) throw new IllegalArgumentException("Edad invÃ¡lida"); }
  return repo.insert(INTERVIEW,Map.of("id_franja_entrevista",slotId,"schema_id",recordId(schema),"schema_version",number(schema,"version"),
   "informacion_aspirante",info,"correo_form_aspirante",email,"estado_enum","AGENDADA","formSchema",schema));
 }); }
 public List<Map<String,Object>> interviews() { return repo.list(INTERVIEW); }
 public Map<String,Object> attendance(long id,boolean attended) { return tx.required(()->{
  repo.lock(INTERVIEW,id); var current=repo.get(INTERVIEW,id);
  repo.lock(SLOT,id(current,"id_franja_entrevista"));
  if(!text(current,"estado_enum").equals("AGENDADA")) throw new SchoolConflict("La entrevista ya tiene resultado");
  return repo.update(INTERVIEW,id,merge(current,Map.of("estado_enum",attended ? "REALIZADA" : "INASISTENCIA")));
 }); }
 public Map<String,Object> enroll(Map<String,Object> data) { return tx.required(()->{
  var profile=object(data.get("estudiante")); birthdate(profile,"fecha_nacimiento",clock,true);
  var schema=schema("inscripcion"); var answers=optionalObject(data.get("informacion_form"));
  validateAnswers(schema,answers);
  var student=saveStudent(null,merge(profile,Map.of("estado","ASPIRANTE"))); long sid=recordId(student);
  var contacts=new LinkedHashMap<String,Map<String,Object>>();
  for(String key:List.of("padre","madre","acudiente_adicional","responsable_pago")) {
   var person=optionalObject(data.get(key)); if(text(person,"documento_identidad").isEmpty() && text(person,"primer_nombre").isEmpty()) continue;
   String identity=text(person,"tipo_documento")+":"+text(person,"documento_identidad");
   person.put("id_estudiante",sid); person.putIfAbsent("parentesco",key.equals("padre") ? "PADRE" : key.equals("madre") ? "MADRE" : "OTRO");
   person.put("es_responsable_pago",key.equals("responsable_pago"));
   if(contacts.containsKey(identity)) {
    var existing=contacts.get(identity); if(key.equals("responsable_pago")) existing.put("es_responsable_pago",true);
   } else contacts.put(identity,person);
  }
  if(contacts.isEmpty()) throw new IllegalArgumentException("Debes proporcionar al menos un acudiente");
  Long guardian=null;
  for(var contact:contacts.values()) {
   contact.put("es_principal_estudiante",guardian==null);
   var saved=saveGuardian(null,contact); if(guardian==null) guardian=recordId(saved);
  }
  var snapshot=new LinkedHashMap<>(data); snapshot.putAll(answers);
  if(profile.containsKey("observaciones_relevantes")) snapshot.put("observaciones_relevantes",profile.get("observaciones_relevantes"));
  return repo.insert(INSCRIPTION,Map.of("id_estudiante",sid,"id_acudiente",guardian,"schema_id",recordId(schema),"schema_version",number(schema,"version"),
   "estado","PENDIENTE","status","PENDIENTE","fecha_inscripcion",clock.instant().toString(),"informacion_form",snapshot,"formSchema",schema));
 }); }
 private Map<String,Object> inscriptionDetails(Map<String,Object> data) {
  return merge(data,Map.of("estudiante",student(id(data,"id_estudiante")),"acudiente",repo.get(GUARDIAN,id(data,"id_acudiente"))));
 }
 public Map<String,Object> inscriptions(Map<String,String> filters) {
  int page,perPage;
  try { page=Integer.parseInt(filters.getOrDefault("page","1")); perPage=Integer.parseInt(filters.getOrDefault("perPage","10")); }
  catch(NumberFormatException e) { throw new IllegalArgumentException("PaginaciÃ³n invÃ¡lida"); }
  if(page<1 || page>100000 || perPage<1 || perPage>100) throw new IllegalArgumentException("PaginaciÃ³n invÃ¡lida");
  String status=filters.getOrDefault("estado",""); String query=filters.getOrDefault("search","").toLowerCase(Locale.ROOT);
  String from=filters.getOrDefault("fechaDesde",""); String to=filters.getOrDefault("fechaHasta","");
  if(!from.isEmpty()) LocalDate.parse(from); if(!to.isEmpty()) LocalDate.parse(to);
  var all=repo.list(INSCRIPTION).stream().filter(i->status.isBlank() || status.equals("all") || status.equals(i.get("estado")))
   .filter(i->from.isEmpty() || text(i,"fecha_inscripcion").substring(0,10).compareTo(from)>=0)
   .filter(i->to.isEmpty() || text(i,"fecha_inscripcion").substring(0,10).compareTo(to)<=0)
   .map(this::inscriptionDetails).filter(i->{var s=object(i.get("estudiante")); return (text(s,"primer_nombre")+" "+text(s,"primer_apellido")+" "+text(s,"documento_identidad")).toLowerCase(Locale.ROOT).contains(query);})
   .sorted(Comparator.comparingLong(this::recordId).reversed()).toList();
  return Map.of("items",all.stream().skip((long)(page-1)*perPage).limit(perPage).toList(),"pagination",Map.of("page",page,"perPage",perPage,"total",all.size()));
 }
 public Map<String,Object> decide(long id,boolean approve) { return tx.required(()->{
  repo.lock(INSCRIPTION,id); var record=repo.get(INSCRIPTION,id); String state=text(record,"estado"); String desired=approve ? "APROBADA" : "RECHAZADA";
  if(state.equals(desired)) return inscriptionDetails(record);
  if(!state.equals("PENDIENTE")) throw new SchoolConflict("La inscripciÃ³n ya tiene una decisiÃ³n");
  if(!approve) saveStudent(id(record,"id_estudiante"),Map.of("estado","RETIRADO"));
  return inscriptionDetails(repo.update(INSCRIPTION,id,merge(record,Map.of("estado",desired,"status",desired,"admission_stage",approve ? "ACEPTADO" : "RECHAZADO"))));
 }); }
}
