package com.example.distribuidora.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "business_name", nullable = false)
    String businessName;

    @Column(name = "tax_id")
    String taxId;

    @Column(name = "name")
    String name;

    String phone;

    @Column(name = "email")
    String email;

    @Column(name = "is_active")
    boolean isActive = true;

    @Column(name = "created_at")
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;
}
