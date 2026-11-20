package com.example.distribuidora.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trucks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Truck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    //Repartidor asignado (usuario con rol REPARTIDOR). Puede ser null si el camion no tiene chofer
    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
