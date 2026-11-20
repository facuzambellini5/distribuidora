package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.RoleResponse;
import com.example.distribuidora.services.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Roles y Permisos", description = "Consulta de roles predefinidos del sistema con su descripción y permisos detallados.")
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "Listar roles y permisos",
            description = "Requiere permiso users:manage. Devuelve nombre, descripción y lista de permisos (código estable, nombre legible y descripción) para cada rol del sistema.")
    @GetMapping
    @PreAuthorize("hasAuthority('users:manage')")
    public ResponseEntity<List<RoleResponse>> list() {
        return ResponseEntity.ok(roleService.getRoles());
    }
}
