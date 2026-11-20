package com.example.distribuidora.dtos;

import com.example.distribuidora.models.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Long roleId,
        String roleName,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public UserResponse(User user) {
        this(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getId(),
                user.getRole().getName(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
