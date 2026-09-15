package com.tallersemillas.backend.school.infrastructure.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.*;
import java.util.*;
/** Single-instance limit. Configure shared rate limiting at the gateway when scaling. */
public final class SubmissionLimitFilter extends OncePerRequestFilter {
 private record Window(Instant until,int count) {}
 private final Map<String,Window> windows=new HashMap<>();
 private synchronized boolean allow(String key) {
  Instant now=Instant.now(); windows.entrySet().removeIf(e->!e.getValue().until().isAfter(now));
  Window w=windows.get(key); if(w==null && windows.size()>=10000) return false;
  if(w==null) w=new Window(now.plusSeconds(900),0);
  if(w.count()>=30) return false;
  windows.put(key,new Window(w.until(),w.count()+1)); return true;
 }
 protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  if(request.getMethod().equals("POST") && Set.of("/api/v1/auth/login","/api/v1/auth/register","/api/v1/interview/create","/api/v1/inscription/create").contains(request.getServletPath())) {
   if(!allow(request.getRemoteAddr()+":"+request.getServletPath())) {
    response.setStatus(429); response.setHeader("Retry-After","900"); response.setContentType("application/json"); response.getWriter().write("{\"detail\":\"Demasiados intentos. Intenta mas tarde.\"}"); return;
   }
  }
  chain.doFilter(request,response);
 }
}
