package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.OrderPaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderDeliveryRequest(
        @NotNull OrderPaymentMethod paymentMethod,
        BigDecimal cashAmount,
        BigDecimal transferAmount,
        BigDecimal amountPaid,
        @Size(max = 100) String idempotencyKey
) {
    public OrderDeliveryRequest(OrderPaymentMethod paymentMethod,
                                BigDecimal cashAmount,
                                BigDecimal transferAmount) {
        this(paymentMethod, cashAmount, transferAmount, null, null);
    }
}
