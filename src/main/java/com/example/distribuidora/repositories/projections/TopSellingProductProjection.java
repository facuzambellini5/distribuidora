package com.example.distribuidora.repositories.projections;

import java.math.BigDecimal;

public interface TopSellingProductProjection {
    Long getProductId();
    String getProductName();
    Long getQuantitySold();
    BigDecimal getSalesAmount();
}
