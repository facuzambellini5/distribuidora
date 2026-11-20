package com.example.distribuidora.dtos;

import com.example.distribuidora.models.Role;

import java.util.Comparator;
import java.util.List;

public record RoleResponse(
        Long id,
        String name,
        String description,
        List<PermissionResponse> permissions
) {
    public RoleResponse(Role role) {
        this(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions() == null ? List.of() : role.getPermissions().stream()
                        .map(PermissionResponse::new)
                        .sorted(Comparator.comparing(PermissionResponse::code))
                        .toList()
        );
    }
}
