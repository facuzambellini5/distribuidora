package com.example.distribuidora.dtos;

import com.example.distribuidora.models.Role;

public record RoleResponse(
        Long id,
        String name
) {
    public RoleResponse(Role role) {
        this(role.getId(), role.getName());
    }
}
