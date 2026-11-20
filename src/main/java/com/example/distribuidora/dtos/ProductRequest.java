package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.ProductUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @Size(max = 50) String sku,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal unitPrice,
        @NotNull ProductUnit unit,
        Long categoryId
) {
}