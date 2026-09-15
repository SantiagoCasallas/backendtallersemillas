package com.tallersemillas.backend.infrastructure.adapter.inbound.web;

import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
public class PublicController {
    @GetMapping("/api/public/home") public Map<String,String> home() {
        return Map.of("name","Taller Semillas","title","Bienvenidos a Taller Semillas");
    }
}
