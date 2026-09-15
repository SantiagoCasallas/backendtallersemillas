package com.tallersemillas.backend.school.infrastructure.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.io.IOException;
public final class BearerFilter extends OncePerRequestFilter {
 private final ApiTokens tokens;
 public BearerFilter(ApiTokens tokens) { this.tokens=tokens; }
 protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  String header=request.getHeader("Authorization");
  if(header!=null) {
   var principal=header.startsWith("Bearer ") ? tokens.resolve(header.substring(7)) : java.util.Optional.<com.tallersemillas.backend.infrastructure.adapter.inbound.security.AccountPrincipal>empty();
   if(principal.isEmpty()) { response.setStatus(401); response.setContentType("application/json"); response.getWriter().write("{\"detail\":\"Token invalido o vencido\"}"); return; }
   var user=principal.get(); var context=SecurityContextHolder.createEmptyContext();
   context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities()));
   SecurityContextHolder.setContext(context);
  }
  chain.doFilter(request,response);
 }
}
