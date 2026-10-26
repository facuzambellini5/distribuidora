package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.OrderRequest;
import com.example.distribuidora.dtos.OrderDeliveryRequest;
import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.services.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('orders:write')")
    public ResponseEntity<?> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('orders:read')")
    public ResponseEntity<?> list(@RequestParam(required = false) Long customerId,
                                  @RequestParam(required = false) Long truckId,
                                  @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrders(customerId, truckId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('orders:read')")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('orders:write')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.update(id, request));
    }

    @PatchMapping("/{id}/in-route")
    @PreAuthorize("hasAuthority('orders:deliver')")
    public ResponseEntity<?> markInRoute(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.markInRoute(id));
    }

    @PatchMapping("/{id}/delivered")
    @PreAuthorize("hasAuthority('orders:deliver')")
    public ResponseEntity<?> markDelivered(@PathVariable Long id, @Valid @RequestBody OrderDeliveryRequest request) {
        return ResponseEntity.ok(orderService.markDelivered(id, request));
    }

    @PatchMapping("/{id}/cancelled")
    @PreAuthorize("hasAuthority('orders:write')")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('orders:write')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
