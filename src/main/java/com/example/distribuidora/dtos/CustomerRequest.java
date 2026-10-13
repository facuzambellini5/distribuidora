package com.example.distribuidora.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CustomerRequest(
        @NotBlank @Size(max = 150) String businessName,
        @Size(max = 30) String taxId,
        @Size(max = 150) String name,
        @Size(max = 30) String phone,
        @Email @Size(max = 150) String email,
        @Valid List<AddressRequest> addresses
) {
}
