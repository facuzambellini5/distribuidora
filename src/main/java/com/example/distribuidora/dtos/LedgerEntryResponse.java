package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.LedgerEntryType;
import com.example.distribuidora.models.LedgerEntry;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LedgerEntryResponse(
        Long id,
        Long customerId,
        Long deliveryOrderId,
        LedgerEntryType entryType,
        BigDecimal amount,
        BigDecimal runningBalance,
        LocalDateTime createdAt
) {
    public LedgerEntryResponse(LedgerEntry entry) {
        this(
                entry.getId(),
                entry.getCustomer().getId(),
                entry.getDeliveryOrder() == null ? null : entry.getDeliveryOrder().getId(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getRunningBalance(),
                entry.getCreatedAt()
        );
    }
}
