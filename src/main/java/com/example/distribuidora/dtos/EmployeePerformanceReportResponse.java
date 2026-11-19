package com.example.distribuidora.dtos;

import java.math.BigDecimal;

public record EmployeePerformanceReportResponse(
        Long employeeId,
        String employeeName,
        String role,
        Long orderCount,
        BigDecimal salesAmount,
        Long deliveriesCompleted
) {
}
