package com.tallersemillas.backend.school.infrastructure.web;

import com.tallersemillas.backend.school.domain.SchoolConflict;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.annotation.Order;
import java.time.DateTimeException;
@RestControllerAdvice @Order(0)
public class SchoolErrors {
 @ExceptionHandler(SchoolConflict.class) ResponseEntity<ProblemDetail> conflict(SchoolConflict e) { return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,e.getMessage())); }
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ProblemDetail> integrity(DataIntegrityViolationException e) {
  return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Ya existe un registro con esos datos o hay registros relacionados"));
 }
 @ExceptionHandler(DateTimeException.class) ResponseEntity<ProblemDetail> date(DateTimeException e) { return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Fecha inválida")); }
}
