package com.example.distribuidora.models;

import com.example.distribuidora.enums.DeliveryAttemptStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_delivery_attempts",
        uniqueConstraints = @UniqueConstraint(name = "uq_delivery_attempt_order_number",
                columnNames = {"delivery_order_id", "attempt_number"}))
@Getter
@Setter
@NoArgsConstructor
public class OrderDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_order_id", nullable = false)
    private Order order;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "truck_id")
    private Long truckId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryAttemptStatus status;

    @Column(name = "failure_reason", length = 255)
    private String reason;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private LocalDateTime attemptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
