package com.tallersemillas.backend.school.infrastructure.web;

import com.tallersemillas.backend.school.application.port.SchoolManagement;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import java.util.*;
import static com.tallersemillas.backend.school.domain.Fields.*;
@RestController @RequestMapping("/api/v1")
public class SchoolController {
 private final SchoolManagement school;
 public SchoolController(SchoolManagement school) { this.school=school; }
 @GetMapping("/students/all") public Map<String,Object> students(@RequestParam(required=false) String status,@RequestParam(defaultValue="") String search) { return Map.of("students",school.students(status,search)); }
 @GetMapping("/students/{id}") public Map<String,Object> student(@PathVariable long id) { return school.student(id); }
 @PostMapping("/students") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createStudent(@RequestBody Map<String,Object> data) { return school.saveStudent(null,FrontendFields.profile(data)); }
 @PatchMapping("/students/{id}") public Map<String,Object> patchStudent(@PathVariable long id,@RequestBody Map<String,Object> data) { return school.saveStudent(id,FrontendFields.profile(data)); }
 @DeleteMapping("/students/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void removeStudent(@PathVariable long id) { school.retireStudent(id); }
 @GetMapping("/students/estudiante/{id}") public Map<String,Object> guardians(@PathVariable long id) { return Map.of("acudientes",school.guardians(id)); }
 @PostMapping({"/parent","/parent/"}) @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createGuardian(@RequestBody Map<String,Object> data) { return school.saveGuardian(null,FrontendFields.profile(data)); }
 @PatchMapping("/parent/{id}") public Map<String,Object> patchGuardian(@PathVariable long id,@RequestBody Map<String,Object> data) { return school.saveGuardian(id,FrontendFields.profile(data)); }
 @DeleteMapping("/parent/{id}/estudiante/{student}") @ResponseStatus(HttpStatus.NO_CONTENT) public void removeGuardian(@PathVariable long id,@PathVariable long student) { school.removeGuardian(id,student); }
 @PostMapping("/medical_information") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> history(@RequestBody Map<String,Object> data) { return school.history(id(data,"id_estudiante"),true); }
 @GetMapping("/medical_information/estudiante/{student}") public Map<String,Object> history(@PathVariable long student) { return school.history(student,false); }
 @PostMapping({"/medical_entry","/medical_entry/"}) @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> entry(@RequestBody Map<String,Object> data) { limit(data,0); return school.saveMedicalEntry(null,data); }
 @GetMapping("/medical_entry/por_informacion/medica/{history}") public List<Map<String,Object>> entries(@PathVariable long history) { return school.medicalEntries(history); }
 @PutMapping("/medical_entry/{id}") public Map<String,Object> entry(@PathVariable long id,@RequestBody Map<String,Object> data) { limit(data,0); return school.saveMedicalEntry(id,data); }
 @DeleteMapping("/medical_entry/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void removeEntry(@PathVariable long id) { school.removeMedicalEntry(id); }
 @GetMapping("/interview/latest-form-schema") public Map<String,Object> interviewSchema() { return Map.of("form_schema",school.schema("entrevista")); }
 @GetMapping("/inscription/latest-form-schema") public Map<String,Object> inscriptionSchema() { return Map.of("form_schema",school.schema("inscripcion")); }
 @PostMapping("/interview/schema") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> interviewSchema(@RequestBody List<Map<String,Object>> fields) { return school.publishSchema("entrevista",fields.stream().map(FrontendFields::schemaField).toList()); }
 @PostMapping("/inscription/schema") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> inscriptionSchema(@RequestBody List<Map<String,Object>> fields) { return school.publishSchema("inscripcion",fields.stream().map(FrontendFields::schemaField).toList()); }
 @GetMapping("/time_slot/available_time_slots") public Map<String,Object> slots(Authentication auth) {
  boolean admin=auth!=null && auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMINISTRADOR"));
  return Map.of("franjas",school.slots(!admin));
 }
 @GetMapping("/time_slot/{id}") public Map<String,Object> slot(@PathVariable long id) { return Map.of("franja",school.slot(id)); }
 @PostMapping({"/time_slot","/time_slot/"}) @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createSlot(@RequestBody Map<String,Object> data) { limit(data,0); return school.saveSlot(null,data); }
 @PutMapping("/time_slot/{id}") public Map<String,Object> patchSlot(@PathVariable long id,@RequestBody Map<String,Object> data) { limit(data,0); return school.saveSlot(id,data); }
 @DeleteMapping("/time_slot/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void removeSlot(@PathVariable long id) { school.removeSlot(id); }
 @PostMapping("/interview/create") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> book(@RequestBody Map<String,Object> data) {
  limit(data,0); var saved=school.bookInterview(data); return Map.of("id",saved.get("id"),"estado_enum",saved.get("estado_enum"));
 }
 @GetMapping("/interview") public Map<String,Object> interviews() { return Map.of("interviews",school.interviews()); }
 @PatchMapping("/interview/{id}/assisted") public Map<String,Object> attended(@PathVariable long id) { return school.attendance(id,true); }
 @PatchMapping("/interview/{id}/unassisted") public Map<String,Object> missed(@PathVariable long id) { return school.attendance(id,false); }
 @PostMapping("/inscription/create") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> enroll(@RequestBody Map<String,Object> data) {
  var saved=school.enroll(FrontendFields.enrollment(data)); return Map.of("id",saved.get("id"),"estado",saved.get("estado"));
 }
 @GetMapping("/inscription") public Map<String,Object> inscriptions(@RequestParam Map<String,String> filters) { return school.inscriptions(filters); }
 @PatchMapping("/inscription/{id}/approve") public Map<String,Object> approve(@PathVariable long id) { return school.decide(id,true); }
 @DeleteMapping("/inscription/{id}/reject") public Map<String,Object> reject(@PathVariable long id) { return school.decide(id,false); }
}
