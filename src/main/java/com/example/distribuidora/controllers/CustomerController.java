package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.CustomerHistoryResponse;
import com.example.distribuidora.dtos.CustomerRequest;
import com.example.distribuidora.dtos.CustomerResponse;
import com.example.distribuidora.services.CustomerHistoryService;
import com.example.distribuidora.services.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Clientes", description = "Gestión de clientes e historial consolidado de compras, pagos, deuda, entregas y devoluciones.")
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerHistoryService customerHistoryService;

    @Autowired
    public CustomerController(CustomerService customerService, CustomerHistoryService customerHistoryService) {
        this.customerService = customerService;
        this.customerHistoryService = customerHistoryService;
    }

    public CustomerController(CustomerService customerService) {
        this(customerService, null);
    }

    @Operation(summary = "Crear cliente", description = "Requiere permiso customers:write.")
    @PostMapping
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<CustomerResponse> saveCustomer(@Valid @RequestBody CustomerRequest customerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.saveCustomer(customerRequest));
    }

    @Operation(summary = "Listar clientes", description = "Requiere permiso customers:read.")
    @GetMapping
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<List<CustomerResponse>> getCustomers(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(customerService.getCustomers(active));
    }

    @Operation(summary = "Obtener cliente por ID", description = "Requiere permiso customers:read.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @Operation(summary = "Consultar historial consolidado del cliente",
            description = "Requiere permiso customers:read o ledger:read. Reúne compras, pagos, deuda actual, entregas y devoluciones (indicando el camión de reintegro).")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyAuthority('customers:read', 'ledger:read')")
    public ResponseEntity<CustomerHistoryResponse> getCustomerHistory(@PathVariable Long id) {
        return ResponseEntity.ok(customerHistoryService.getCustomerHistory(id));
    }

    @Operation(summary = "Actualizar cliente", description = "Requiere permiso customers:write.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id,
                                                           @Valid @RequestBody CustomerRequest customerRequest) {
        return ResponseEntity.ok(customerService.updateCustomer(id, customerRequest));
    }

    @Operation(summary = "Activar cliente", description = "Requiere permiso customers:write.")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<Void> activateCustomer(@PathVariable Long id) {
        customerService.activateCustomer(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Desactivar cliente", description = "Requiere permiso customers:write.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
