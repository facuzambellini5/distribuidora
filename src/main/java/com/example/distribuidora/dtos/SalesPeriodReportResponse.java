package com.example.distribuidora.dtos;

import java.math.BigDecimal;

public record SalesPeriodReportResponse(
        String period,
        Long orderCount,
        BigDecimal salesAmount
) {
}
