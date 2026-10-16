package com.example.distribuidora.controllers;

import com.example.distribuidora.services.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // Lista de roles con sus IDs, para armar el selector al crear/editar usuarios.
    @GetMapping
    @PreAuthorize("hasAuthority('users:manage')")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(roleService.getRoles());
    }
}
