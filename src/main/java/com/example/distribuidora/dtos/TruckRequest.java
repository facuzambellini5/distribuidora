package com.example.distribuidora.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TruckRequest(
        @NotBlank(message = "License plate is required")
        @Size(max = 20, message = "License plate must be at most 20 characters")
        String licensePlate,

        //Id del usuario repartidor (opcional). Si es null el camion queda sin chofer
        Long driverId
) {
}
