package com.example.distribuidora.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "permissions")
@Data
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // formato "recurso:accion", ej: "products:read"
    @Column(nullable = false, unique = true, length = 60)
    String code;

    @Column(length = 200)
    String description;
}