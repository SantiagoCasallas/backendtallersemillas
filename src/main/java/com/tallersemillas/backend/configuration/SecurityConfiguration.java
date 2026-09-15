package com.tallersemillas.backend.configuration;

import com.tallersemillas.backend.application.port.outbound.Accounts;
import com.tallersemillas.backend.infrastructure.adapter.inbound.security.AccountPrincipal;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.util.*;
@Configuration
public class SecurityConfiguration {
    @Bean UserDetailsService userDetailsService(Accounts accounts) {
        return email -> new AccountPrincipal(accounts.findByEmail(email.strip().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas")));
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.cors(cors -> {}).authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET,"/api/public/home","/api/auth/csrf").permitAll()
            .requestMatchers(HttpMethod.POST,"/api/auth/register","/api/auth/login").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
            .requestMatchers("/api/students/**","/api/students","/api/auth/me").authenticated()
            .anyRequest().denyAll())
            .formLogin(login -> login.loginProcessingUrl("/api/auth/login").usernameParameter("email")
                .successHandler((req,res,auth) -> res.setStatus(204))
                .failureHandler((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"error\":\"Credenciales invalidas\"}"); }))
            .logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req,res,auth) -> res.setStatus(204)))
            .requestCache(cache -> cache.disable())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"error\":\"Autenticacion requerida\"}"); })
                .accessDeniedHandler((req,res,ex) -> { res.setStatus(403); res.setContentType("application/json"); res.getWriter().write("{\"error\":\"Acceso denegado o token CSRF invalido\"}"); }))
            .build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origins) {
        CorsConfiguration cors=new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::strip).toList());
        cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type","X-CSRF-TOKEN","Authorization"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/api/**",cors); return source;
    }
}
