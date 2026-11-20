package com.example.distribuidora.repositories.projections;

import java.math.BigDecimal;

public interface SalesPeriodProjection {
    String getPeriod();
    Long getOrderCount();
    BigDecimal getSalesAmount();
}
