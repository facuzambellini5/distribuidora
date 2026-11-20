package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.LedgerEntryType;
import com.example.distribuidora.enums.OrderPaymentMethod;
import com.example.distribuidora.models.LedgerEntry;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LedgerEntryResponse(
        Long id,
        Long customerId,
        Long deliveryOrderId,
        String orderCode,
        LedgerEntryType entryType,
        OrderPaymentMethod paymentMethod,
        BigDecimal cashAmount,
        BigDecimal transferAmount,
        BigDecimal amount,
        BigDecimal runningBalance,
        Long createdById,
        String createdByName,
        String notes,
        LocalDateTime createdAt
) {
    public LedgerEntryResponse(LedgerEntry entry) {
        this(
                entry.getId(),
                entry.getCustomer().getId(),
                entry.getDeliveryOrder() == null ? null : entry.getDeliveryOrder().getId(),
                entry.getDeliveryOrder() == null ? null : entry.getDeliveryOrder().getOrderCode(),
                entry.getEntryType(),
                entry.getPaymentMethod(),
                entry.getCashAmount(),
                entry.getTransferAmount(),
                entry.getAmount(),
                entry.getRunningBalance(),
                entry.getCreatedBy() == null ? null : entry.getCreatedBy().getId(),
                entry.getCreatedBy() == null ? null : entry.getCreatedBy().getFullName(),
                entry.getNotes(),
                entry.getCreatedAt()
        );
    }
}
