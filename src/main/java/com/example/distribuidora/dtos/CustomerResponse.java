package com.example.distribuidora.dtos;

import com.example.distribuidora.models.Customer;

import java.time.LocalDateTime;
import java.util.List;

public record CustomerResponse(
        Long id,
        String businessName,
        String taxId,
        String name,
        String phone,
        String email,
        boolean active,
        List<AddressResponse> addresses,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public CustomerResponse(Customer customer) {
        this(
                customer.getId(),
                customer.getBusinessName(),
                customer.getTaxId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.isActive(),
                customer.getAddresses().stream().map(AddressResponse::new).toList(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
