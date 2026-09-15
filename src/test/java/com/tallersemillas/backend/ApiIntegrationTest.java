package com.tallersemillas.backend;

import com.tallersemillas.backend.application.port.inbound.RegisterAccount;
import com.tallersemillas.backend.application.port.outbound.Accounts;
import com.tallersemillas.backend.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired RegisterAccount registration;
    @Autowired Accounts accounts;
    @Autowired PasswordEncoder encoder;
    private static final String PASSWORD="Una contraseña de prueba 2026!";
    private String email() { return UUID.randomUUID()+"@example.test"; }
    private MockHttpSession login(String email) throws Exception {
        var result=mvc.perform(post("/api/auth/login").with(csrf()).param("email",email).param("password",PASSWORD))
            .andExpect(status().isNoContent()).andReturn();
        return (MockHttpSession)result.getRequest().getSession(false);
    }
    private String createStudent(MockHttpSession session) throws Exception {
        var result=mvc.perform(post("/api/students").session(session).with(csrf()).contentType("application/json")
            .content("{\"fullName\":\"Alumno de prueba\",\"birthDate\":\"2022-02-01\"}"))
            .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
    @Test void publicHomeAndAnonymousProtection() throws Exception {
        mvc.perform(get("/api/public/home")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Taller Semillas"));
        mvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/students").with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
    }
    @Test void accountIsNormalizedHashedAndAlwaysUser() throws Exception {
        String email=email();
        String body=json.writeValueAsString(Map.of("name","Familia prueba","email",email.toUpperCase(Locale.ROOT),"password",PASSWORD));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USUARIO"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.password").doesNotExist());
        Account account=accounts.findByEmail(email).orElseThrow();
        assertThat(account.passwordHash()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD,account.passwordHash())).isTrue();
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(Map.of("name","Prueba","email",email(),"password",PASSWORD,"role","ADMINISTRADOR"))))
            .andExpect(status().isBadRequest());
    }
    @Test void csrfRequiredAndActualTokenWorks() throws Exception {
        String body=json.writeValueAsString(Map.of("name","Prueba","email",email(),"password",PASSWORD));
        mvc.perform(post("/api/auth/register").contentType("application/json").content(body)).andExpect(status().isForbidden());
        var result=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token=json.readTree(result.getResponse().getContentAsString());
        mvc.perform(post("/api/auth/register").session((MockHttpSession)result.getRequest().getSession(false))
            .header(token.get("headerName").asText(),token.get("token").asText()).contentType("application/json").content(body))
            .andExpect(status().isCreated());
    }
    @Test void loginMeBadPasswordAndLogout() throws Exception {
        String email=email(); registration.register("Prueba",email,PASSWORD);
        mvc.perform(post("/api/auth/login").with(csrf()).param("email",email).param("password","incorrecta"))
            .andExpect(status().isUnauthorized());
        MockHttpSession session=login(email);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void studentsAreIsolatedAndAdminCanReadAll() throws Exception {
        String first=email(),second=email(),admin=email();
        registration.register("Familia uno",first,PASSWORD);
        registration.register("Familia dos",second,PASSWORD);
        Account a=registration.register("Administración",admin,PASSWORD);
        accounts.save(new Account(a.id(),a.name(),a.email(),a.passwordHash(),Role.ADMINISTRADOR));
        MockHttpSession s1=login(first),s2=login(second),sa=login(admin);
        String id=createStudent(s1);
        mvc.perform(get("/api/students/"+id).session(s1)).andExpect(status().isOk());
        mvc.perform(get("/api/students/"+id).session(s2)).andExpect(status().isNotFound());
        mvc.perform(get("/api/students").session(s2)).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/students/"+id).session(sa)).andExpect(status().isOk());
        mvc.perform(get("/api/students").session(sa)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").exists());
    }
    @Test void invalidBirthDateAndForgedGuardianAreRejected() throws Exception {
        String email=email(); registration.register("Prueba",email,PASSWORD); MockHttpSession session=login(email);
        mvc.perform(post("/api/students").session(session).with(csrf()).contentType("application/json")
            .content("{\"fullName\":\"Prueba\",\"birthDate\":\"2999-01-01\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/students").session(session).with(csrf()).contentType("application/json")
            .content("{\"fullName\":\"Prueba\",\"birthDate\":\"2022-01-01\",\"guardianId\":\""+UUID.randomUUID()+"\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/students?size=101").session(session)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/students/not-a-uuid").session(session)).andExpect(status().isBadRequest());
    }
    @Test void corsAllowsOnlyConfiguredFrontend() throws Exception {
        mvc.perform(options("/api/students").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","POST"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
        mvc.perform(options("/api/students").header("Origin","https://untrusted.example").header("Access-Control-Request-Method","POST"))
            .andExpect(status().isForbidden());
    }
}
