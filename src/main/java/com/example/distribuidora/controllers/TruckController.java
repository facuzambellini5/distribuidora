package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.DeliveryStockRequest;
import com.example.distribuidora.dtos.DeliveryStockResponse;
import com.example.distribuidora.dtos.DeliveryStockUpdateRequest;
import com.example.distribuidora.dtos.OrderResponse;
import com.example.distribuidora.dtos.TruckRequest;
import com.example.distribuidora.dtos.TruckResponse;
import com.example.distribuidora.dtos.TruckStockHistoryResponse;
import com.example.distribuidora.dtos.TruckStockMovementResponse;
import com.example.distribuidora.enums.StockMovementType;
import com.example.distribuidora.services.DeliveryStockService;
import com.example.distribuidora.services.OrderService;
import com.example.distribuidora.services.TruckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Camiones y Stock por Camión", description = "CRUD de camiones y gestión/historial de stock exclusivamente por camión (sin inventario central).")
@RestController
@RequestMapping("/api/trucks")
public class TruckController {

    private final TruckService truckService;
    private final DeliveryStockService stockService;
    private final OrderService orderService;

    public TruckController(TruckService truckService, DeliveryStockService stockService, OrderService orderService) {
        this.truckService = truckService;
        this.stockService = stockService;
        this.orderService = orderService;
    }

    // ---------------------- CRUD de camiones ----------------------

    @Operation(summary = "Crear camión", description = "Requiere permiso trucks:write.")
    @PostMapping
    @PreAuthorize("hasAuthority('trucks:write')")
    public ResponseEntity<TruckResponse> save(@Valid @RequestBody TruckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(truckService.save(request));
    }

    @Operation(summary = "Listar camiones", description = "Requiere permiso trucks:read.")
    @GetMapping
    @PreAuthorize("hasAuthority('trucks:read')")
    public ResponseEntity<List<TruckResponse>> getTrucks(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(Boolean.TRUE.equals(active)
                ? truckService.getActiveTrucks()
                : truckService.getTrucks());
    }

    @Operation(summary = "Obtener camión por ID", description = "Requiere permiso trucks:read.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('trucks:read')")
    public ResponseEntity<TruckResponse> getTruckById(@PathVariable Long id) {
        return ResponseEntity.ok(truckService.getTruckById(id));
    }

    @Operation(summary = "Actualizar camión", description = "Requiere permiso trucks:write.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('trucks:write')")
    public ResponseEntity<TruckResponse> update(@PathVariable Long id, @Valid @RequestBody TruckRequest request) {
        return ResponseEntity.ok(truckService.update(id, request));
    }

    @Operation(summary = "Activar camión", description = "Requiere permiso trucks:write.")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('trucks:write')")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        truckService.activate(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Desactivar camión", description = "Requiere permiso trucks:write.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('trucks:write')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        truckService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ---------------- Productos cargados y movimientos de stock en el camion ----------------

    @Operation(summary = "Listar pedidos asignados al camión", description = "Requiere permiso orders:read.")
    @GetMapping("/{truckId}/orders")
    @PreAuthorize("hasAuthority('orders:read')")
    public ResponseEntity<List<OrderResponse>> getOrders(@PathVariable Long truckId) {
        return ResponseEntity.ok(orderService.getOrdersByTruck(truckId));
    }

    @Operation(summary = "Cargar producto en camión", description = "Requiere permiso stock:write. Registra movimiento histórico LOAD.")
    @PostMapping("/{truckId}/stock")
    @PreAuthorize("hasAuthority('stock:write')")
    public ResponseEntity<DeliveryStockResponse> loadProduct(@PathVariable Long truckId,
                                                             @Valid @RequestBody DeliveryStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.load(truckId, request));
    }

    @Operation(summary = "Ver stock cargado del camión", description = "Requiere permiso stock:read. Sin date devuelve el stock del día actual (YYYY-MM-DD).")
    @GetMapping("/{truckId}/stock")
    @PreAuthorize("hasAuthority('stock:read')")
    public ResponseEntity<List<DeliveryStockResponse>> getStock(
            @PathVariable Long truckId,
            @Parameter(description = "Fecha del stock (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(stockService.getStockByTruck(truckId, date));
    }

    @Operation(summary = "Consultar historial de movimientos y saldos de stock del camión",
            description = "Requiere permiso stock:read. Devuelve saldos resultantes (cargado, entregado, devuelto, saldo) y el historial de movimientos filtrable por producto, fecha y usuario.")
    @GetMapping("/{truckId}/stock/history")
    @PreAuthorize("hasAuthority('stock:read')")
    public ResponseEntity<TruckStockHistoryResponse> getStockHistory(
            @PathVariable Long truckId,
            @RequestParam(required = false) Long productId,
            @Parameter(description = "Fecha desde (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Fecha hasta (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) StockMovementType movementType) {
        return ResponseEntity.ok(stockService.getStockHistory(truckId, productId, fromDate, toDate, userId, movementType));
    }

    @Operation(summary = "Consultar movimientos de stock entre camiones",
            description = "Requiere permiso stock:read. Filtra movimientos históricos de stock por camión, producto, rango de fechas (YYYY-MM-DD), usuario y tipo (LOAD, DELIVERY, RETURN).")
    @GetMapping("/stock/movements")
    @PreAuthorize("hasAuthority('stock:read')")
    public ResponseEntity<List<TruckStockMovementResponse>> getStockMovements(
            @RequestParam(required = false) Long truckId,
            @RequestParam(required = false) Long productId,
            @Parameter(description = "Fecha desde (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Fecha hasta (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) StockMovementType movementType) {
        return ResponseEntity.ok(stockService.getMovements(truckId, productId, fromDate, toDate, userId, movementType));
    }

    @Operation(summary = "Actualizar registro de stock del camión", description = "Requiere permiso stock:write.")
    @PutMapping("/{truckId}/stock/{stockId}")
    @PreAuthorize("hasAuthority('stock:write')")
    public ResponseEntity<DeliveryStockResponse> updateStock(@PathVariable Long truckId,
                                                             @PathVariable Long stockId,
                                                             @Valid @RequestBody DeliveryStockUpdateRequest request) {
        return ResponseEntity.ok(stockService.update(truckId, stockId, request));
    }

    @Operation(summary = "Eliminar registro de stock sin reservas", description = "Requiere permiso stock:write.")
    @DeleteMapping("/{truckId}/stock/{stockId}")
    @PreAuthorize("hasAuthority('stock:write')")
    public ResponseEntity<Void> deleteStock(@PathVariable Long truckId, @PathVariable Long stockId) {
        stockService.delete(truckId, stockId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
