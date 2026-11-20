package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.OrderPaymentMethod;
import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.models.Address;
import com.example.distribuidora.models.Order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderCode,
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
        BigDecimal amountPaid,
        BigDecimal pendingBalance,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime deliveredAt,
        Long deliveredById,
        String deliveredByName,
        OrderPaymentMethod paymentMethod,
        BigDecimal cashAmount,
        BigDecimal transferAmount,
        List<OrderItemResponse> items
) {
    public OrderResponse(Order order) {
        this(order, computeOrderPaid(order));
    }

    public OrderResponse(Order order, BigDecimal totalPaid) {
        this(
                order.getId(),
                order.getOrderCode(),
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
                totalPaid,
                computePending(order.getTotalAmount(), totalPaid),
                order.getCreatedBy() == null ? null : order.getCreatedBy().getId(),
                order.getCreatedBy() == null ? null : order.getCreatedBy().getFullName(),
                order.getCreatedAt(),
                order.getDeliveredAt(),
                order.getDeliveredBy() == null ? null : order.getDeliveredBy().getId(),
                order.getDeliveredBy() == null ? null : order.getDeliveredBy().getFullName(),
                order.getPaymentMethod(),
                order.getCashAmount(),
                order.getTransferAmount(),
                order.getItems() == null ? List.of() : order.getItems().stream().map(OrderItemResponse::new).toList()
        );
    }

    private static BigDecimal computeOrderPaid(Order order) {
        BigDecimal cash = order.getCashAmount() == null ? BigDecimal.ZERO : order.getCashAmount();
        BigDecimal transfer = order.getTransferAmount() == null ? BigDecimal.ZERO : order.getTransferAmount();
        return cash.add(transfer);
    }

    private static BigDecimal computePending(BigDecimal totalAmount, BigDecimal paid) {
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal safePaid = paid == null ? BigDecimal.ZERO : paid;
        BigDecimal diff = total.subtract(safePaid);
        return diff.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : diff;
    }

    private static String buildAddressSnapshot(Address address) {
        String label = address.getLabel() == null || address.getLabel().isBlank() ? "" : address.getLabel() + " - ";
        String street = address.getStreetAddress() == null ? "" : address.getStreetAddress();
        String city = address.getCity() == null || address.getCity().isBlank() ? "" : ", " + address.getCity();
        return label + street + city;
    }
}
