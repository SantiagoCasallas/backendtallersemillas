package com.tallersemillas.backend.school.domain;

import java.util.*;
import java.time.Clock;
import static com.tallersemillas.backend.school.domain.Fields.*;
public final class ProfileRules {
 private ProfileRules() {}
 public static void person(Map<String,Object> data,Clock clock) {
  required(data,"primer_nombre",120); required(data,"primer_apellido",120);
  required(data,"documento_identidad",40);
  if(!Set.of("RC","TI","CC","CE","PP","P").contains(required(data,"tipo_documento",10))) throw new IllegalArgumentException("Tipo de documento inválido");
  birthdate(data,"fecha_nacimiento",clock,false);
  String email=text(data,"correo_electronico"); if(!email.isEmpty()) email(email);
  for(var e:data.entrySet()) if(e.getValue() instanceof String s && s.length()>1000) throw new IllegalArgumentException("Campo demasiado largo: "+e.getKey());
 }
 public static void student(Map<String,Object> data,Clock clock) {
  person(data,clock);
  if(!Set.of("ASPIRANTE","ACTIVO","RETIRADO","EGRESADO").contains(text(data,"estado"))) throw new IllegalArgumentException("Estado inválido");
  long strata=number(data,"estrato"); if(strata<0 || strata>6) throw new IllegalArgumentException("Estrato debe estar entre 0 y 6");
  bool(data,"tiene_almuerzo",false); bool(data,"tiene_transporte_ruta",false);
 }
}
