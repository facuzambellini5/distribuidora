package com.example.distribuidora.dtos;

import jakarta.validation.constraints.Min;

//Para corregir lo cargado o registrar lo que volvio en el camion
public record DeliveryStockUpdateRequest(
        @Min(value = 0, message = "Quantity loaded can't be negative")
        int quantityLoaded,

        @Min(value = 0, message = "Quantity returned can't be negative")
        int quantityReturned
) {
}
