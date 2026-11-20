package com.example.distribuidora.repositories.projections;

public interface EmployeeDeliveryProjection {
    Long getEmployeeId();
    String getEmployeeName();
    String getRole();
    Long getDeliveriesCompleted();
}
