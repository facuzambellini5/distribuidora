package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.CustomerDto;
import com.example.distribuidora.services.interfaces.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Autowired
    private ICustomerService customerService;

    @PostMapping("/save")
    public ResponseEntity<?> saveCustomer(@RequestBody CustomerDto customerDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.saveCustomer(customerDto));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getCustomers() {
        return ResponseEntity.ok(customerService.getCustomers());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @GetMapping("/get/active")
    public ResponseEntity<?> getActiveCustomers() {
        return ResponseEntity.ok(customerService.getActiveCustomers());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id,
                                            @RequestBody CustomerDto customerDto) {

        return ResponseEntity.ok(customerService.updateCustomer(id, customerDto));
    }

    @PatchMapping("/desactivate/{id}")
    public ResponseEntity<?> desactivateCustomer(@PathVariable Long id) {
        customerService.desactivateCustomer(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/activate/{id}")
    public ResponseEntity<?> activateCustomer(@PathVariable Long id) {
        customerService.activateCustomer(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
