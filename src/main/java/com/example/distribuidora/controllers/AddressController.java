package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.AddressRequest;
import com.example.distribuidora.dtos.AddressResponse;
import com.example.distribuidora.services.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Direcciones de Clientes", description = "Gestión de direcciones de entrega asociadas a cada cliente.")
@RestController
@RequestMapping("/api/customers/{customerId}/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @Operation(summary = "Crear dirección de cliente", description = "Requiere permiso customers:write.")
    @PostMapping
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<AddressResponse> create(@PathVariable Long customerId,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.save(customerId, request));
    }

    @Operation(summary = "Listar direcciones de cliente", description = "Requiere permiso customers:read.")
    @GetMapping
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<List<AddressResponse>> list(@PathVariable Long customerId) {
        return ResponseEntity.ok(addressService.getByCustomer(customerId));
    }

    @Operation(summary = "Obtener dirección por ID", description = "Requiere permiso customers:read.")
    @GetMapping("/{addressId}")
    @PreAuthorize("hasAuthority('customers:read')")
    public ResponseEntity<AddressResponse> get(@PathVariable Long customerId, @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.get(customerId, addressId));
    }

    @Operation(summary = "Actualizar dirección de cliente", description = "Requiere permiso customers:write.")
    @PutMapping("/{addressId}")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<AddressResponse> update(@PathVariable Long customerId,
                                                  @PathVariable Long addressId,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(customerId, addressId, request));
    }

    @Operation(summary = "Eliminar dirección de cliente", description = "Requiere permiso customers:write.")
    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasAuthority('customers:write')")
    public ResponseEntity<Void> delete(@PathVariable Long customerId, @PathVariable Long addressId) {
        addressService.delete(customerId, addressId);
        return ResponseEntity.noContent().build();
    }
}
