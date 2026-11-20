package com.example.distribuidora.models;

import com.example.distribuidora.enums.LedgerEntryType;
import com.example.distribuidora.enums.OrderPaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_order_id")
    private Order deliveryOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private LedgerEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 32)
    private OrderPaymentMethod paymentMethod;

    @Column(name = "cash_amount")
    private BigDecimal cashAmount;

    @Column(name = "transfer_amount")
    private BigDecimal transferAmount;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "running_balance", nullable = false)
    private BigDecimal runningBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(length = 255)
    private String notes;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
