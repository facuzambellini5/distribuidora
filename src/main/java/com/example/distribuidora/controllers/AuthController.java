package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.LoginRequest;
import com.example.distribuidora.dtos.LoginResponse;
import com.example.distribuidora.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    //TODO VER SI ESTO ES NECESARIO, YA QUE EL LOGOUT SE HACE DEL LADO DEL FRONTEND, SOLO BORRANDO EL JWT
    //VER SI INVALIDAR TOKEN EN EL LADO DEL BACKEND
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    // Le permite al frontend preguntar "¿quién está logueado ahora?" sin
    // tener que decodificar el JWT del lado de Angular. Spring ya nos
    // arma este objeto Authentication solo, gracias al JwtAuthenticationFilter.
    //TODO DEVOLVER LOS DATOS CORRESPONDIENTES
    @GetMapping("/me")
    public ResponseEntity<String> me(Authentication authentication) {
        return ResponseEntity.ok(authentication.getName());
    }
}