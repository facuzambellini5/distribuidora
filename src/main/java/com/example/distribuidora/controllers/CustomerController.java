package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.CustomerDto;
import com.example.distribuidora.dtos.CustomerRequest;
import com.example.distribuidora.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/customers", "/api/customer"})
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping({"", "/save"})
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> saveCustomer(@Valid @RequestBody CustomerRequest customerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.saveCustomer(customerRequest));
    }

    @PostMapping("/save-legacy")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> saveCustomerLegacy(@Valid @RequestBody CustomerDto customerDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.saveCustomer(toRequest(customerDto)));
    }

    @GetMapping({"", "/get"})
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<?> getCustomers(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(customerService.getCustomers(active));
    }

    @GetMapping({"/{id}", "/get/{id}"})
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<?> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @GetMapping("/get/active")
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<?> getActiveCustomers() {
        return ResponseEntity.ok(customerService.getCustomers(true));
    }

    @PutMapping({"/{id}", "/update/{id}"})
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id,
                                            @Valid @RequestBody CustomerRequest customerRequest) {

        return ResponseEntity.ok(customerService.updateCustomer(id, customerRequest));
    }

    @PatchMapping("/desactivate/{id}")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> desactivateCustomer(@PathVariable Long id) {
        customerService.desactivateCustomer(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping({"/{id}/activate", "/activate/{id}"})
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> activateCustomer(@PathVariable Long id) {
        customerService.activateCustomer(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private CustomerRequest toRequest(CustomerDto dto) {
        return new CustomerRequest(dto.getBusinessName(), dto.getTaxId(), dto.getName(), dto.getPhone(), dto.getEmail(), List.of());
    }
}
