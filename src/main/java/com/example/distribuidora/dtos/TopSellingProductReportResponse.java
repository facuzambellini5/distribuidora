package com.example.distribuidora.dtos;

import java.math.BigDecimal;

public record TopSellingProductReportResponse(
        Long productId,
        String productName,
        Long quantitySold,
        BigDecimal salesAmount
) {
}
