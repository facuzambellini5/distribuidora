package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.CustomerRequest;
import com.example.distribuidora.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> saveCustomer(@Valid @RequestBody CustomerRequest customerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.saveCustomer(customerRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<?> getCustomers(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(customerService.getCustomers(active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<?> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id,
                                            @Valid @RequestBody CustomerRequest customerRequest) {

        return ResponseEntity.ok(customerService.updateCustomer(id, customerRequest));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> activateCustomer(@PathVariable Long id) {
        customerService.activateCustomer(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
