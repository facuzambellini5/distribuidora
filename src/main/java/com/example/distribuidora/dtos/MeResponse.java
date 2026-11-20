package com.example.distribuidora.dtos;

import com.example.distribuidora.models.Permission;
import com.example.distribuidora.models.Truck;
import com.example.distribuidora.models.User;

import java.util.List;

// Respuesta de GET /api/auth/me. truckId y truckLicensePlate son null si el
// usuario no es repartidor o no tiene un camion activo asignado.
public record MeResponse(
        Long id,
        String fullName,
        String email,
        Long roleId,
        String roleName,
        List<String> permissions,
        Long truckId,
        String truckLicensePlate
) {
    public MeResponse(User user, Truck truck) {
        this(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getId(),
                user.getRole().getName(),
                user.getRole().getPermissions().stream().map(Permission::getCode).sorted().toList(),
                truck == null ? null : truck.getId(),
                truck == null ? null : truck.getLicensePlate()
        );
    }
}
