package com.example.distribuidora.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Carga de stock en un camion originada por un pedido (una fila por pedido y producto).
// La clave unica (delivery_order_id, product_id) garantiza que reintentos no dupliquen la carga.
// revertedAt != null significa que la carga ya fue revertida.
@Entity
@Table(name = "order_stock_loads")
@Getter
@Setter
@NoArgsConstructor
public class OrderStockLoad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "truck_id", nullable = false)
    private Long truckId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "reverted_at")
    private LocalDateTime revertedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isActive() {
        return revertedAt == null;
    }
}
