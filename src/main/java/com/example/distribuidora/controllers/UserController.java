package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.UserCreateRequest;
import com.example.distribuidora.dtos.UserResponse;
import com.example.distribuidora.dtos.UserUpdateRequest;
import com.example.distribuidora.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Usuarios", description = "Administración de usuarios y asignación de roles.")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Crear usuario", description = "Requiere permiso users:manage.")
    @PostMapping
    @PreAuthorize("hasAuthority('users:manage')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @Operation(summary = "Listar usuarios", description = "Requiere permiso users:manage.")
    @GetMapping
    @PreAuthorize("hasAuthority('users:manage')")
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.getUsers());
    }

    @Operation(summary = "Obtener usuario por ID", description = "Requiere permiso users:manage.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users:manage')")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(userService.get(id));
    }

    @Operation(summary = "Actualizar usuario", description = "Requiere permiso users:manage.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users:manage')")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @Operation(summary = "Activar usuario", description = "Requiere permiso users:manage.")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('users:manage')")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        userService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Desactivar usuario", description = "Requiere permiso users:manage.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('users:manage')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
