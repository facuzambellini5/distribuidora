package com.example.distribuidora.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

//Tabla intermedia camion <-> producto: que producto y cuantas unidades se cargan en un camion para una fecha
@Entity
@Table(name = "delivery_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "truck_id", nullable = false)
    private Truck truck;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "quantity_loaded", nullable = false)
    private int quantityLoaded = 0;

    @Column(name = "quantity_returned", nullable = false)
    private int quantityReturned = 0;
}
