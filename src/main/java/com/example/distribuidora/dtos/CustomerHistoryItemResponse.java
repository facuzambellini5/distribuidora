package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.CustomerHistoryEventType;
import com.example.distribuidora.enums.OrderPaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerHistoryItemResponse(
        LocalDateTime date,
        CustomerHistoryEventType eventType,
        String description,
        BigDecimal amount,
        BigDecimal runningBalance,
        Integer quantity,
        Long productId,
        String productName,
        String status,
        Long orderId,
        String orderCode,
        OrderPaymentMethod paymentMethod,
        Long truckId,
        String truckLicensePlate,
        Long responsibleUserId,
        String responsibleUserName
) {
}
