package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.LedgerEntryRequest;
import com.example.distribuidora.services.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers/{customerId}/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ledger:read')")
    public ResponseEntity<?> list(@PathVariable Long customerId) {
        return ResponseEntity.ok(ledgerService.getByCustomer(customerId));
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAuthority('ledger:write')")
    public ResponseEntity<?> payment(@PathVariable Long customerId, @Valid @RequestBody LedgerEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.registerPayment(customerId, request));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('ledger:write')")
    public ResponseEntity<?> adjustment(@PathVariable Long customerId, @Valid @RequestBody LedgerEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.registerAdjustment(customerId, request));
    }
}
