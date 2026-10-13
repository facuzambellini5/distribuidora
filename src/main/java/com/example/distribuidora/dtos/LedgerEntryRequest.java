package com.example.distribuidora.dtos;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LedgerEntryRequest(
        @NotNull BigDecimal amount
) {
}
