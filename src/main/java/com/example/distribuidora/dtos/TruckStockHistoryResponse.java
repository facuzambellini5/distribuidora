package com.example.distribuidora.dtos;

import java.time.LocalDate;
import java.util.List;

public record TruckStockHistoryResponse(
        Long truckId,
        String truckLicensePlate,
        LocalDate fromDate,
        LocalDate toDate,
        List<TruckStockSummaryItemResponse> balances,
        List<TruckStockMovementResponse> movements
) {
}
