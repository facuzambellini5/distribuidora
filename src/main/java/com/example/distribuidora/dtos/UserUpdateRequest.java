package com.example.distribuidora.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 150) String fullName,
        @Email @Size(max = 150) String email,
        @Size(min = 8, max = 100) String password,
        Long roleId,
        Boolean active
) {
}
