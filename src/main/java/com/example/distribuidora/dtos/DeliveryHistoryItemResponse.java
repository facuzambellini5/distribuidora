package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.OrderStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DeliveryHistoryItemResponse(
        Long orderId,
        String orderCode,
        Long customerId,
        String customerBusinessName,
        String deliveryAddressSnapshot,
        LocalDate deliveryDate,
        LocalDateTime deliveredAt,
        Long truckId,
        String truckLicensePlate,
        Long driverId,
        String driverName,
        OrderStatus status,
        List<OrderItemResponse> items
) {
}
