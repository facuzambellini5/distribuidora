package com.example.distribuidora.dtos;

import java.math.BigDecimal;
import java.util.List;

public record CustomerHistoryResponse(
        Long customerId,
        String customerBusinessName,
        BigDecimal currentDebt,
        List<CustomerHistoryItemResponse> entries
) {
}
