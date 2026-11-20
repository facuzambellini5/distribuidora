package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.ProductUnit;
import com.example.distribuidora.enums.StockMovementType;
import com.example.distribuidora.models.TruckStockMovement;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TruckStockMovementResponse(
        Long id,
        Long truckId,
        String truckLicensePlate,
        Long productId,
        String productName,
        ProductUnit productUnit,
        Long orderId,
        String orderCode,
        StockMovementType movementType,
        int quantity,
        LocalDate movementDate,
        Long createdById,
        String createdByName,
        String notes,
        LocalDateTime createdAt
) {
    public TruckStockMovementResponse(TruckStockMovement movement) {
        this(
                movement.getId(),
                movement.getTruck().getId(),
                movement.getTruck().getLicensePlate(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getProduct().getUnit(),
                movement.getDeliveryOrder() == null ? null : movement.getDeliveryOrder().getId(),
                movement.getDeliveryOrder() == null ? null : movement.getDeliveryOrder().getOrderCode(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getMovementDate(),
                movement.getCreatedBy() == null ? null : movement.getCreatedBy().getId(),
                movement.getCreatedBy() == null ? null : movement.getCreatedBy().getFullName(),
                movement.getNotes(),
                movement.getCreatedAt()
        );
    }
}
