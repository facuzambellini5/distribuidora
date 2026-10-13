package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.models.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        String customerBusinessName,
        Long truckId,
        OrderStatus status,
        String deliveryAddressSnapshot,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        BigDecimal totalAmount,
        Long createdById,
        LocalDateTime createdAt,
        LocalDateTime deliveredAt,
        List<OrderItemResponse> items
) {
    public OrderResponse(Order order) {
        this(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getBusinessName(),
                order.getTruckId(),
                order.getStatus(),
                order.getDeliveryAddressSnapshot(),
                order.getDeliveryLatitude(),
                order.getDeliveryLongitude(),
                order.getTotalAmount(),
                order.getCreatedBy() == null ? null : order.getCreatedBy().getId(),
                order.getCreatedAt(),
                order.getDeliveredAt(),
                order.getItems().stream().map(OrderItemResponse::new).toList()
        );
    }
}
