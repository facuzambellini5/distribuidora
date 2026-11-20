package com.example.distribuidora.repositories.projections;

import java.math.BigDecimal;

public interface EmployeeSalesProjection {
    Long getEmployeeId();
    String getEmployeeName();
    String getRole();
    Long getOrderCount();
    BigDecimal getSalesAmount();
}
