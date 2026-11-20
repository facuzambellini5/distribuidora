package com.example.distribuidora.dtos;

import com.example.distribuidora.enums.DeliveryAttemptStatus;
import com.example.distribuidora.models.OrderDeliveryAttempt;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DeliveryAttemptResponse(
        Long id,
        Long orderId,
        int attemptNumber,
        Long truckId,
        LocalDate deliveryDate,
        DeliveryAttemptStatus status,
        String reason,
        LocalDateTime attemptedAt,
        LocalDateTime completedAt,
        Long recordedById,
        String recordedByName
) {
    public DeliveryAttemptResponse(OrderDeliveryAttempt attempt) {
        this(attempt.getId(),
                attempt.getOrder().getId(),
                attempt.getAttemptNumber(),
                attempt.getTruckId(),
                attempt.getDeliveryDate(),
                attempt.getStatus(),
                attempt.getReason(),
                attempt.getAttemptedAt(),
                attempt.getCompletedAt(),
                attempt.getCreatedBy() == null ? null : attempt.getCreatedBy().getId(),
                attempt.getCreatedBy() == null ? null : attempt.getCreatedBy().getFullName());
    }
}
