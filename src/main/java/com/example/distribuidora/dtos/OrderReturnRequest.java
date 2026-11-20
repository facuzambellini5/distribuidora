package com.example.distribuidora.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderReturnRequest(
        @NotNull(message = "Product id is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Returned quantity must be greater than zero")
        Integer quantity,

        @Size(max = 255, message = "Notes cannot exceed 255 characters")
        String notes,

        @Size(max = 100, message = "Idempotency key cannot exceed 100 characters")
        String idempotencyKey
) {
    public OrderReturnRequest(Long productId, Integer quantity, String notes) {
        this(productId, quantity, notes, null);
    }
}
