package com.example.distribuidora.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotNull Long customerId,
        @NotNull Long deliveryAddressId,
        Long truckId,
        Long createdById,
        @Valid @NotEmpty List<OrderItemRequest> items
) {
}
