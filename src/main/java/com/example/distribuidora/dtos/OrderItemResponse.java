package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.ProductUnit;
import com.example.distribuidora.models.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        ProductUnit productUnit,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public OrderItemResponse(OrderItem item) {
        this(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getUnit(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
