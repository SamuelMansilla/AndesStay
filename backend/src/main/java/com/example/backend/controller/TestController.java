package com.example.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/public/hello")
    public String publicEndpoint() {
        return "Acceso público concedido: No requiere autenticación.";
    }

    @GetMapping("/protected/hello")
    @PreAuthorize("hasAuthority('SCOPE_OT.Create')") // Revisa que el token traiga el scope OT.Create
    public String protectedEndpoint() {
        return "Acceso concedido: Token con el scope OT.Create validado correctamente.";
    }
}