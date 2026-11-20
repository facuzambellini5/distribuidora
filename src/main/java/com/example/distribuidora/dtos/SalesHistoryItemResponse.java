package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.OrderPaymentMethod;
import com.example.distribuidora.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SalesHistoryItemResponse(
        Long orderId,
        String orderCode,
        LocalDateTime createdAt,
        LocalDate deliveryDate,
        LocalDateTime deliveredAt,
        LocalDateTime lastPaymentAt,
        Long customerId,
        String customerBusinessName,
        Long responsibleUserId,
        String responsibleUserName,
        Long deliveredById,
        String deliveredByName,
        OrderStatus status,
        OrderPaymentMethod paymentMethod,
        BigDecimal cashAmount,
        BigDecimal transferAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal pendingBalance,
        List<OrderItemResponse> items
) {
}
