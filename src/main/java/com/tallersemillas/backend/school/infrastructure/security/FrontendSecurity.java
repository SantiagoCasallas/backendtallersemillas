package com.tallersemillas.backend.school.infrastructure.security;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.http.HttpMethod;
@Configuration
public class FrontendSecurity {
 @Bean @Order(1) SecurityFilterChain frontendChain(HttpSecurity http,ApiTokens tokens) throws Exception {
  return http.securityMatcher("/api/v1/**").cors(cors->{})
   .csrf(csrf->csrf.disable())
   .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .requestCache(cache->cache.disable())
   .authorizeHttpRequests(auth->auth
    .requestMatchers(HttpMethod.POST,"/api/v1/auth/login","/api/v1/auth/register","/api/v1/interview/create","/api/v1/preinscription/*").permitAll()
    .requestMatchers(HttpMethod.GET,"/api/v1/interview/latest-form-schema","/api/v1/time_slot/available_time_slots","/api/v1/time_slot/*","/api/v1/preinscription/*").permitAll()
    .requestMatchers("/api/v1/auth/me","/api/v1/auth/logout").authenticated()
    .anyRequest().hasAnyRole("ADMINISTRADOR", "DIRECTIVO"))
   .addFilterBefore(new BearerFilter(tokens),UsernamePasswordAuthenticationFilter.class)
   .addFilterBefore(new SubmissionLimitFilter(),AuthorizationFilter.class)
   .exceptionHandling(errors->errors
    .authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"detail\":\"Autenticacion requerida\"}");})
    .accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"detail\":\"Se requiere un rol de gestión\"}");}))
   .build();
 }
}
