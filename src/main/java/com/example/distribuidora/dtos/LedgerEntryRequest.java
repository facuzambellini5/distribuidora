package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.OrderPaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LedgerEntryRequest(
        @NotNull BigDecimal amount,
        OrderPaymentMethod paymentMethod,
        BigDecimal cashAmount,
        BigDecimal transferAmount,
        Long orderId,
        @Size(max = 255) String notes,
        @Size(max = 100) String idempotencyKey
) {
    public LedgerEntryRequest(BigDecimal amount) {
        this(amount, null, null, null, null, null, null);
    }
}
