package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.LoginRequest;
import com.example.distribuidora.dtos.LoginResponse;
import com.example.distribuidora.dtos.MeResponse;
import com.example.distribuidora.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticación", description = "Login JWT, cierre de sesión y consulta del perfil autenticado.")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Iniciar sesión", description = "Endpoint público. Devuelve token Bearer JWT y tiempo de expiración.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Cerrar sesión", description = "Requiere usuario autenticado.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener perfil del usuario autenticado", description = "Requiere usuario autenticado. Devuelve rol, permisos y camión activo asignado si corresponde.")
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }
}