package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.OrderPaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderDeliveryRequest(
        @NotNull OrderPaymentMethod paymentMethod,
        BigDecimal cashAmount,
        BigDecimal transferAmount
) {
}
