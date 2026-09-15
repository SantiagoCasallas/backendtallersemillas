package com.tallersemillas.backend.school.infrastructure.web;

import com.tallersemillas.backend.infrastructure.adapter.inbound.security.AccountPrincipal;
import com.tallersemillas.backend.school.application.AdmissionsWorkflow;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tallersemillas.backend.school.domain.Fields.*;

@RestController
@RequestMapping("/api/v1")
public class AdmissionsController {
    private final AdmissionsWorkflow workflow;

    public AdmissionsController(AdmissionsWorkflow workflow) {
        this.workflow = workflow;
    }

    @PatchMapping("/interview/{id}/result")
    public Map<String, Object> result(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return workflow.result(id, bool(body, "asistio", false), bool(body, "continuar", false));
    }

    @PostMapping("/interview/{id}/invitation")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> invitation(@PathVariable long id) {
        return workflow.issue(id);
    }

    @PatchMapping("/interview/{id}/reschedule")
    public Map<String, Object> reschedule(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Object value = body.get("id_franja_entrevista");
        return workflow.reschedule(id, value == null ? null : number(body, "id_franja_entrevista"));
    }

    @GetMapping("/preinscription/{token}")
    public Map<String, Object> context(@PathVariable String token) {
        return workflow.context(token);
    }

    @PostMapping("/preinscription/{token}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> preinscription(@PathVariable String token, @RequestBody Map<String, Object> body) {
        return workflow.enroll(token, FrontendFields.enrollment(body), number(body, "schema_version"), bool(body, "accepted_privacy_policy", false));
    }

    @PostMapping("/inscription/{id}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> formalize(@PathVariable long id, @RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal AccountPrincipal actor) {
        return workflow.formalize(id, Math.toIntExact(number(body, "school_year")), actor.id().toString());
    }

    @PostMapping("/students/{id}/observations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> observe(@PathVariable long id, @RequestBody Map<String, Object> body,
                                        @AuthenticationPrincipal AccountPrincipal actor) {
        return workflow.observe(id, required(body, "description", 5000), actor.id().toString());
    }

    @GetMapping("/students/{id}/observations")
    public List<Map<String, Object>> observations(@PathVariable long id) {
        return workflow.observations(id);
    }

    @GetMapping("/students/{id}/enrollments")
    public List<Map<String, Object>> enrollments(@PathVariable long id) {
        return workflow.years(id);
    }

    @GetMapping("/inscription/{id}/document")
    public Map<String, Object> document(@PathVariable long id) {
        return workflow.document(id);
    }
}
