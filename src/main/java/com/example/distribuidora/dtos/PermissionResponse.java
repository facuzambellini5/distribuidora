package com.example.distribuidora.dtos;

import com.example.distribuidora.models.Permission;

public record PermissionResponse(
        String code,
        String name,
        String description
) {
    public PermissionResponse(Permission permission) {
        this(
                permission.getCode(),
                permission.getName() != null ? permission.getName() : permission.getCode(),
                permission.getDescription()
        );
    }
}
