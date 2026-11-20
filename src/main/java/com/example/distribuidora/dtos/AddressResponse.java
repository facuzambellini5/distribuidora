package com.example.distribuidora.dtos;

import com.example.distribuidora.models.Address;

import java.math.BigDecimal;

public record AddressResponse(
        Long id,
        Long customerId,
        String label,
        String streetAddress,
        String city,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean isDefault
) {
    public AddressResponse(Address address) {
        this(
                address.getId(),
                address.getCustomer().getId(),
                address.getLabel(),
                address.getStreetAddress(),
                address.getCity(),
                address.getLatitude(),
                address.getLongitude(),
                address.isDefault()
        );
    }
}
