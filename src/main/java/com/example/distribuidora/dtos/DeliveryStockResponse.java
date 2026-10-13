package com.example.distribuidora.dtos;

import com.example.distribuidora.models.DeliveryStock;

import java.time.LocalDate;

public record DeliveryStockResponse(
        Long id,
        Long truckId,
        String licensePlate,
        Long productId,
        String productName,
        LocalDate deliveryDate,
        int quantityLoaded,
        int quantityReturned
) {

    public DeliveryStockResponse(DeliveryStock stock) {
        this(
                stock.getId(),
                stock.getTruck().getId(),
                stock.getTruck().getLicensePlate(),
                stock.getProduct().getId(),
                stock.getProduct().getName(),
                stock.getDeliveryDate(),
                stock.getQuantityLoaded(),
                stock.getQuantityReturned()
        );
    }
}
