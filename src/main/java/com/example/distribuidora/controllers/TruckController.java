package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.DeliveryStockRequest;
import com.example.distribuidora.dtos.DeliveryStockUpdateRequest;
import com.example.distribuidora.dtos.TruckRequest;
import com.example.distribuidora.services.DeliveryStockService;
import com.example.distribuidora.services.TruckService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
//TODO VER SI ESTANDARIZAR LOS PATHS DE LOS ENDPOINTS
//TODO VER SI AGREGAR PERMISSIONS para VENDEDOR
@RestController
@RequestMapping({"/api/trucks", "/api/truck"})
public class TruckController {

    private final TruckService truckService;
    private final DeliveryStockService stockService;

    public TruckController(TruckService truckService, DeliveryStockService stockService) {
        this.truckService = truckService;
        this.stockService = stockService;
    }

    // ---------------------- CRUD de camiones ----------------------

    @PostMapping({"", "/save"})
    @PreAuthorize("hasAuthority('trucks:write')")
    public ResponseEntity<?> save(@Valid @RequestBody TruckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(truckService.save(request));
    }


    @GetMapping({"", "/get"})
    @PreAuthorize("hasAuthority('trucks:read')")
    public ResponseEntity<?> getTrucks() {
        return ResponseEntity.ok(truckService.getTrucks());
    }

    @GetMapping("/get/active")
    @PreAuthorize("hasAuthority('trucks:read')")
    public ResponseEntity<?> getActiveTrucks() {
        return ResponseEntity.ok(truckService.getActiveTrucks());
    }

    @GetMapping({"/{id}", "/get/{id}"})
    @PreAuthorize("hasAuthority('trucks:read')")
    public ResponseEntity<?> getTruckById(@PathVariable Long id) {
        return ResponseEntity.ok(truckService.getTruckById(id));
    }

    @PutMapping({"/{id}", "/update/{id}"})
    @PreAuthorize("hasAuthority('trucks:write')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody TruckRequest request) {
        return ResponseEntity.ok(truckService.update(id, request));
    }

    @PatchMapping("/desactivate/{id}")
    @PreAuthorize("hasAuthority('trucks:write')")
    public ResponseEntity<?> desactivate(@PathVariable Long id) {
        truckService.desactivate(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping({"/{id}/activate", "/activate/{id}"})
    @PreAuthorize("hasAuthority('trucks:write')")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        truckService.activate(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    @PreAuthorize("hasAuthority('trucks:write')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        truckService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ---------------- Productos cargados en el camion ----------------

    //Cargar un producto en el camion
    @PostMapping("/{truckId}/stock")
    @PreAuthorize("hasAuthority('stock:write')")
    public ResponseEntity<?> loadProduct(@PathVariable Long truckId,
                                         @Valid @RequestBody DeliveryStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.load(truckId, request));
    }

    //Ver lo cargado en el camion. Ej: /api/truck/1/stock?date=2026-09-28 (sin date = hoy)
    @GetMapping("/{truckId}/stock")
    @PreAuthorize("hasAuthority('stock:read')")
    public ResponseEntity<?> getStock(@PathVariable Long truckId,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(stockService.getStockByTruck(truckId, date));
    }

    //Corregir cantidad cargada / registrar cantidad devuelta
    @PutMapping("/stock/{stockId}")
    @PreAuthorize("hasAuthority('stock:write')")
    public ResponseEntity<?> updateStock(@PathVariable Long stockId,
                                         @Valid @RequestBody DeliveryStockUpdateRequest request) {
        return ResponseEntity.ok(stockService.update(stockId, request));
    }

    @DeleteMapping("/stock/{stockId}")
    @PreAuthorize("hasAuthority('stock:write')")
    public ResponseEntity<?> deleteStock(@PathVariable Long stockId) {
        stockService.delete(stockId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
