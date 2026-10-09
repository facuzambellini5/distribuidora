package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.AddressRequest;
import com.example.distribuidora.services.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers/{customerId}/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> create(@PathVariable Long customerId, @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.save(customerId, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<?> list(@PathVariable Long customerId) {
        return ResponseEntity.ok(addressService.getByCustomer(customerId));
    }

    @GetMapping("/{addressId}")
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<?> get(@PathVariable Long customerId, @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.get(customerId, addressId));
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> update(@PathVariable Long customerId,
                                    @PathVariable Long addressId,
                                    @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(customerId, addressId, request));
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> delete(@PathVariable Long customerId, @PathVariable Long addressId) {
        addressService.delete(customerId, addressId);
        return ResponseEntity.noContent().build();
    }
}
