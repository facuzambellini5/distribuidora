package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.LedgerEntryRequest;
import com.example.distribuidora.dtos.LedgerEntryResponse;
import com.example.distribuidora.services.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cuenta Corriente y Pagos", description = "Movimientos históricos inmutables de cuenta corriente, pagos por medio (CASH, TRANSFER, MIXED) y ajustes.")
@RestController
@RequestMapping("/api/customers/{customerId}/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Operation(summary = "Listar movimientos de cuenta corriente del cliente", description = "Requiere permiso ledger:read.")
    @GetMapping
    @PreAuthorize("hasAuthority('ledger:read')")
    public ResponseEntity<List<LedgerEntryResponse>> list(@PathVariable Long customerId) {
        return ResponseEntity.ok(ledgerService.getByCustomer(customerId));
    }

    @Operation(summary = "Registrar pago del cliente",
            description = "Requiere permiso ledger:write. Soporta CASH, TRANSFER y MIXED (con desglose positivo que sume el total), vinculación opcional a pedido e idempotencyKey. Los pagos son inmutables.")
    @PostMapping("/payments")
    @PreAuthorize("hasAuthority('ledger:write')")
    public ResponseEntity<LedgerEntryResponse> payment(@PathVariable Long customerId,
                                                       @Valid @RequestBody LedgerEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.registerPayment(customerId, request));
    }

    @Operation(summary = "Registrar ajuste en cuenta corriente",
            description = "Requiere permiso ledger:write. Registra un movimiento histórico inmutable positivo o negativo (no cero). Soporta idempotencyKey.")
    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('ledger:write')")
    public ResponseEntity<LedgerEntryResponse> adjustment(@PathVariable Long customerId,
                                                          @Valid @RequestBody LedgerEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.registerAdjustment(customerId, request));
    }
}
