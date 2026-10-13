package com.example.distribuidora.dtos;

import com.example.distribuidora.models.Truck;

public record TruckResponse(
        Long id,
        String licensePlate,
        Long driverId,
        String driverName,
        boolean active
) {

    public TruckResponse(Truck truck) {
        this(
                truck.getId(),
                truck.getLicensePlate(),
                truck.getDriver() == null ? null : truck.getDriver().getId(),
                truck.getDriver() == null ? null : truck.getDriver().getFullName(),
                truck.isActive()
        );
    }
}
