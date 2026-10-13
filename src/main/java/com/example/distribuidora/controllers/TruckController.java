package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.DeliveryStockRequest;
import com.example.distribuidora.dtos.DeliveryStockUpdateRequest;
import com.example.distribuidora.dtos.TruckRequest;
import com.example.distribuidora.services.DeliveryStockService;
import com.example.distribuidora.services.TruckService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/truck")
public class TruckController {

    @Autowired
    private TruckService truckService;

    @Autowired
    private DeliveryStockService stockService;

    // ---------------------- CRUD de camiones ----------------------

    @PostMapping("/save")
    public ResponseEntity<?> save(@Valid @RequestBody TruckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(truckService.save(request));
    }

    @GetMapping("/get")
    public ResponseEntity<?> getTrucks() {
        return ResponseEntity.ok(truckService.getTrucks());
    }

    @GetMapping("/get/active")
    public ResponseEntity<?> getActiveTrucks() {
        return ResponseEntity.ok(truckService.getActiveTrucks());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getTruckById(@PathVariable Long id) {
        return ResponseEntity.ok(truckService.getTruckById(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody TruckRequest request) {
        return ResponseEntity.ok(truckService.update(id, request));
    }

    @PatchMapping("/desactivate/{id}")
    public ResponseEntity<?> desactivate(@PathVariable Long id) {
        truckService.desactivate(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/activate/{id}")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        truckService.activate(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        truckService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ---------------- Productos cargados en el camion ----------------

    //Cargar un producto en el camion
    @PostMapping("/{truckId}/stock")
    public ResponseEntity<?> loadProduct(@PathVariable Long truckId,
                                         @Valid @RequestBody DeliveryStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.load(truckId, request));
    }

    //Ver lo cargado en el camion. Ej: /api/truck/1/stock?date=2026-09-28 (sin date = hoy)
    @GetMapping("/{truckId}/stock")
    public ResponseEntity<?> getStock(@PathVariable Long truckId,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(stockService.getStockByTruck(truckId, date));
    }

    //Corregir cantidad cargada / registrar cantidad devuelta
    @PutMapping("/stock/{stockId}")
    public ResponseEntity<?> updateStock(@PathVariable Long stockId,
                                         @Valid @RequestBody DeliveryStockUpdateRequest request) {
        return ResponseEntity.ok(stockService.update(stockId, request));
    }

    @DeleteMapping("/stock/{stockId}")
    public ResponseEntity<?> deleteStock(@PathVariable Long stockId) {
        stockService.delete(stockId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
