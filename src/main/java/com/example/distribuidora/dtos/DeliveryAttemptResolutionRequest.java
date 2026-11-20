package com.example.distribuidora.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryAttemptResolutionRequest(
        @NotBlank(message = "El motivo de la resolución de entrega es obligatorio")
        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
        String reason,

        @Size(max = 100, message = "La clave de idempotencia no puede superar los 100 caracteres")
        String idempotencyKey
) {
}
