package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.enums.OrderPaymentMethod;
import com.example.distribuidora.models.Address;
import com.example.distribuidora.models.Order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        String customerBusinessName,
        Long truckId,
        LocalDate deliveryDate,
        OrderStatus status,
        String deliveryAddressSnapshot,
        Long addressId,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        BigDecimal totalAmount,
        Long createdById,
        LocalDateTime createdAt,
        LocalDateTime deliveredAt,
        OrderPaymentMethod paymentMethod,
        BigDecimal cashAmount,
        BigDecimal transferAmount,
        List<OrderItemResponse> items
) {
    public OrderResponse(Order order) {
        this(
                order.getId(),
                order.getCustomer() == null ? null : order.getCustomer().getId(),
                order.getCustomer() == null ? null : order.getCustomer().getBusinessName(),
                order.getTruckId(),
                order.getDeliveryDate(),
                order.getStatus(),
                order.getDeliveryAddress() == null ? null : buildAddressSnapshot(order.getDeliveryAddress()),
                order.getDeliveryAddress() == null ? null : order.getDeliveryAddress().getId(),
                order.getDeliveryAddress() == null ? null : order.getDeliveryAddress().getLatitude(),
                order.getDeliveryAddress() == null ? null : order.getDeliveryAddress().getLongitude(),
                order.getTotalAmount(),
                order.getCreatedBy() == null ? null : order.getCreatedBy().getId(),
                order.getCreatedAt(),
                order.getDeliveredAt(),
                order.getPaymentMethod(),
                order.getCashAmount(),
                order.getTransferAmount(),
                order.getItems() == null ? List.of() : order.getItems().stream().map(OrderItemResponse::new).toList()
        );
    }

    private static String buildAddressSnapshot(Address address) {
        String label = address.getLabel() == null || address.getLabel().isBlank() ? "" : address.getLabel() + " - ";
        String street = address.getStreetAddress() == null ? "" : address.getStreetAddress();
        String city = address.getCity() == null || address.getCity().isBlank() ? "" : ", " + address.getCity();
        return label + street + city;
    }
}
