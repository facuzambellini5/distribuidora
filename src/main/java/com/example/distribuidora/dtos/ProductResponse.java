package com.example.distribuidora.dtos;

import com.example.distribuidora.models.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String sku,
        BigDecimal unitPrice,
        Long categoryId,
        String categoryName,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public ProductResponse(Product product) {
        this(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getUnitPrice(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
