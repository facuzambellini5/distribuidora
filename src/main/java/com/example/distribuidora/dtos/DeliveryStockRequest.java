package com.example.distribuidora.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

//Para cargar un producto en un camion
public record DeliveryStockRequest(
        @NotNull(message = "Product id is required")
        Long productId,

        //Si no viene, se toma la fecha de hoy
        LocalDate deliveryDate,

        @Min(value = 1, message = "Quantity loaded must be at least 1")
        int quantityLoaded
) {
}
