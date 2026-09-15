package com.tallersemillas.backend.school.infrastructure.web;

import java.util.*;
import static com.tallersemillas.backend.school.domain.Fields.*;
/** Explicit compatibility mapping. Client-generated ids and audit fields are never accepted. */
public final class FrontendFields {
 private FrontendFields() {}
 private static void alias(Map<String,Object> out,Map<String,Object> in,String target,String... names) {
  for(String name:names) if(in.containsKey(name) && in.get(name)!=null) {
   Object value=in.get(name); out.put(target,value instanceof String s ? s.strip() : value); return;
  }
 }
 public static Map<String,Object> profile(Map<String,Object> input) {
  limit(input,0); var result=new LinkedHashMap<String,Object>();
  alias(result,input,"primer_nombre","firstname","firstName","primerNombre","primer_nombre","nombres");
  alias(result,input,"segundo_nombre","secondName","segundoNombre","segundo_nombre");
  alias(result,input,"primer_apellido","lastname","lastName","primerApellido","primer_apellido","apellidos");
  alias(result,input,"segundo_apellido","secondLastName","segundoApellido","segundo_apellido");
  alias(result,input,"tipo_documento","documentType","tipoDocumento","tipo_documento");
  alias(result,input,"documento_identidad","documentNumber","documentoIdentidad","documento_identidad");
  alias(result,input,"fecha_nacimiento","fechaNacimiento","birthdate","fecha_nacimiento");
  alias(result,input,"correo_electronico","email","correoElectronico","correo_electronico");
  alias(result,input,"telefono","phone","telefono","celular");
  alias(result,input,"pais_nacimiento","paisNacimiento","pais_nacimiento");
  alias(result,input,"departamento_nacimiento","departamentoNacimiento","departamento_nacimiento");
  alias(result,input,"ciudad_nacimiento","ciudadNacimiento","ciudad_nacimiento");
  alias(result,input,"direccion_residencia","homeAddress","direccionResidencia","direccion_residencia");
  alias(result,input,"ciudad_residencia","city","ciudadResidencia","ciudad_residencia");
  alias(result,input,"departamento_residencia","department","departamentoResidencia","departamento_residencia");
  alias(result,input,"estrato","strata","estrato");
  alias(result,input,"tiene_almuerzo","hasLunch","tieneAlmuerzo","tiene_almuerzo");
  alias(result,input,"tiene_transporte_ruta","hasTransport","tieneTranporteRuta","tiene_transporte_ruta");
  alias(result,input,"foto_url","photoURL","fotoUrl","foto_url");
  alias(result,input,"estado","status","estado");
  alias(result,input,"parentesco","kinship","parentesco");
  alias(result,input,"ocupacion","occupation","ocupacion");
  alias(result,input,"nombre_empresa_trabajo","workplace","nombreEmpresaTrabajo","nombre_empresa_trabajo");
  alias(result,input,"direccion_casa","homeAddress","direccionCasa","direccion_casa");
  alias(result,input,"ciudad_casa","city","ciudadCasa","ciudad_casa");
  alias(result,input,"departamento_casa","department","departamentoCasa","departamento_casa");
  alias(result,input,"es_principal_estudiante","isMainGuardian","esPrincipal","es_principal_estudiante","es_principal");
  alias(result,input,"es_responsable_pago","isPaymentResponsible","esResponsablePago","es_responsable_pago");
  alias(result,input,"id_estudiante","id_estudiante","idEstudiante");
  alias(result,input,"barrio","barrio");
  alias(result,input,"observaciones_relevantes","observaciones_relevantes");
  alias(result,input,"lugar_nacimiento","lugar_nacimiento");
  alias(result,input,"telefono_fijo","telefono_fijo");
  if(!result.containsKey("primer_nombre")) {
   String name=text(input,"nombre_completo"); if(name.isEmpty()) name=text(input,"nombre_acudiente");
   if(!name.isEmpty()) { String[] parts=name.split(" ",2); result.put("primer_nombre",parts[0]); result.put("primer_apellido",parts.length>1 ? parts[1] : ""); }
  }
  if(result.containsKey("estrato")) { Object v=result.get("estrato"); result.put("estrato",v instanceof String s && s.isBlank() ? 0 : number(result,"estrato")); }
  if(result.containsKey("fecha_nacimiento")) { String date=text(result,"fecha_nacimiento"); if(date.length()>10) result.put("fecha_nacimiento",date.substring(0,10)); }
  if(result.containsKey("foto_url")) {
   String url=text(result,"foto_url");
   if(url.equals("pendiente") || url.startsWith("blob:")) result.remove("foto_url");
   else if(!url.isEmpty() && !url.startsWith("https://")) throw new IllegalArgumentException("La foto debe ser una URL HTTPS persistente");
  }
  return result;
 }
 public static Map<String,Object> enrollment(Map<String,Object> input) {
  limit(input,0); var result=new LinkedHashMap<String,Object>();
  result.put("estudiante",profile(object(input.get("estudiante"))));
  for(String key:List.of("padre","madre","acudiente_adicional","responsable_pago")) result.put(key,profile(optionalObject(input.get(key))));
  if(input.containsKey("informacion_form")) result.put("informacion_form",object(input.get("informacion_form")));
  return result;
 }
 public static Map<String,Object> schemaField(Map<String,Object> in) {
  var out=new LinkedHashMap<String,Object>(); alias(out,in,"id","id","nombre"); alias(out,in,"type","type","tipo");
  alias(out,in,"label","label","etiqueta"); alias(out,in,"required","required","requerido");
  alias(out,in,"visible","visible"); alias(out,in,"options","options","opciones"); return out;
 }
}
