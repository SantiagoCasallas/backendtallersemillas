package com.tallersemillas.backend.school.domain;

import java.time.*;
import java.util.*;
public final class Fields {
 private Fields() {}
 public static String text(Map<String,Object> data,String key) {
  Object value=data.get(key); if(value==null) return "";
  if(!(value instanceof String)) throw new IllegalArgumentException("Campo de texto inválido: "+key);
  return ((String)value).strip();
 }
 public static String required(Map<String,Object> data,String key,int max) {
  String value=text(data,key);
  if(value.isBlank() || value.length()>max) throw new IllegalArgumentException("Campo obligatorio o demasiado largo: "+key);
  return value;
 }
 public static long number(Map<String,Object> data,String key) {
  Object value=data.get(key);
  try { return Long.parseLong(String.valueOf(value)); }
  catch(RuntimeException e) { throw new IllegalArgumentException("Número entero inválido: "+key); }
 }
 public static long id(Map<String,Object> data,String key) {
  long id=number(data,key); if(id<1) throw new IllegalArgumentException("Identificador inválido: "+key); return id;
 }
 public static boolean bool(Map<String,Object> data,String key,boolean fallback) {
  Object value=data.get(key); if(value==null) return fallback;
  if(value instanceof Boolean b) return b;
  throw new IllegalArgumentException("Valor booleano inválido: "+key);
 }
 @SuppressWarnings("unchecked") public static Map<String,Object> object(Object value) {
  if(!(value instanceof Map<?,?> map)) throw new IllegalArgumentException("Se esperaba un objeto");
  for(Object key:map.keySet()) if(!(key instanceof String)) throw new IllegalArgumentException("Clave inválida");
  return new LinkedHashMap<>((Map<String,Object>)map);
 }
 public static Map<String,Object> optionalObject(Object value) { return value==null ? new LinkedHashMap<>() : object(value); }
 public static void limit(Object value,int depth) {
  if(depth>6) throw new IllegalArgumentException("Datos demasiado anidados");
  if(value instanceof String s && s.length()>10000) throw new IllegalArgumentException("Texto demasiado largo");
  if(value instanceof Map<?,?> m) { if(m.size()>150) throw new IllegalArgumentException("Demasiados campos"); m.values().forEach(v->limit(v,depth+1)); }
  if(value instanceof List<?> l) { if(l.size()>100) throw new IllegalArgumentException("Demasiados elementos"); l.forEach(v->limit(v,depth+1)); }
 }
 public static void birthdate(Map<String,Object> data,String key,Clock clock,boolean required) {
  String date=text(data,key); if(date.isEmpty() && !required) return;
  try { if(!LocalDate.parse(date).isBefore(LocalDate.now(clock))) throw new IllegalArgumentException("Fecha de nacimiento inválida"); }
  catch(java.time.format.DateTimeParseException ex) { throw new IllegalArgumentException("Fecha inválida: "+key); }
 }
 public static void email(String email) {
  if(email.length()>254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Correo inválido");
 }
 public static Map<String,Object> merge(Map<String,Object> before,Map<String,Object> after) {
  var result=new LinkedHashMap<>(before); result.putAll(after); return result;
 }
}
