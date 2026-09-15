package com.tallersemillas.backend.infrastructure.adapter.inbound.web;

import com.tallersemillas.backend.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(NotFound.class) ResponseEntity<ProblemDetail> notFound(NotFound ex) { return problem(404,ex.getMessage()); }
    @ExceptionHandler(AccountConflict.class) ResponseEntity<ProblemDetail> conflict(AccountConflict ex) { return problem(409,ex.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<ProblemDetail> invalid(IllegalArgumentException ex) { return problem(400,ex.getMessage()); }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> invalidRequest(Exception ex) { return problem(400,"Solicitud inválida: revisa los campos y sus formatos"); }
    private ResponseEntity<ProblemDetail> problem(int status,String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status),detail));
    }
}
