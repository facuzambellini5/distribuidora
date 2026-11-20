package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.ProductUnit;

public record TruckStockSummaryItemResponse(
        Long productId,
        String productName,
        ProductUnit productUnit,
        int quantityLoaded,
        int quantityDelivered,
        int quantityReturned,
        int resultingBalance
) {
}
