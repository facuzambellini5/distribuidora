package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.DeliveryHistoryItemResponse;
import com.example.distribuidora.dtos.OrderDeliveryRequest;
import com.example.distribuidora.dtos.OrderRequest;
import com.example.distribuidora.dtos.OrderResponse;
import com.example.distribuidora.dtos.OrderReturnRequest;
import com.example.distribuidora.dtos.SalesHistoryItemResponse;
import com.example.distribuidora.dtos.TruckStockMovementResponse;
import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.enums.SalesDateType;
import com.example.distribuidora.services.DeliveryStockService;
import com.example.distribuidora.services.OrderReceiptPdfService;
import com.example.distribuidora.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Pedidos, Ventas y Repartos", description = "Gestión de pedidos, entregas con cobro, devoluciones por camión, comprobante PDF e historiales.")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final DeliveryStockService deliveryStockService;
    private final OrderReceiptPdfService receiptPdfService;

    @Autowired
    public OrderController(OrderService orderService,
                           DeliveryStockService deliveryStockService,
                           OrderReceiptPdfService receiptPdfService) {
        this.orderService = orderService;
        this.deliveryStockService = deliveryStockService;
        this.receiptPdfService = receiptPdfService;
    }

    public OrderController(OrderService orderService) {
        this(orderService, null, null);
    }

    @Operation(summary = "Crear pedido", description = "Requiere permiso orders:write. Si incluye truckId, carga automáticamente los productos al stock del camión.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado"),
            @ApiResponse(responseCode = "400", description = "Error de validación o regla de negocio"),
            @ApiResponse(responseCode = "403", description = "Sin permiso orders:write"),
            @ApiResponse(responseCode = "404", description = "Cliente, dirección, producto o camión inexistente")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('orders:write')")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @Operation(summary = "Listar pedidos", description = "Requiere permiso orders:read. Para usuarios con rol REPARTIDOR, filtra automáticamente a los pedidos de su camión asignado.")
    @GetMapping
    @PreAuthorize("hasAuthority('orders:read')")
    public ResponseEntity<List<OrderResponse>> list(@RequestParam(required = false) Long customerId,
                                                    @RequestParam(required = false) Long truckId,
                                                    @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrders(customerId, truckId, status));
    }

    @Operation(summary = "Consultar historial de ventas",
            description = "Requiere permiso orders:read o reports:read. Filtra por rango de fechas (YYYY-MM-DD), tipo de fecha (CREATED, DELIVERED, PAID; por defecto CREATED), cliente y usuario responsable.")
    @GetMapping("/sales-history")
    @PreAuthorize("hasAnyAuthority('orders:read', 'reports:read')")
    public ResponseEntity<List<SalesHistoryItemResponse>> salesHistory(
            @Parameter(description = "Fecha desde (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Fecha hasta (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Tipo de fecha a filtrar: CREATED (creación), DELIVERED (entrega) o PAID (pago)")
            @RequestParam(defaultValue = "CREATED") SalesDateType dateType,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(orderService.getSalesHistory(fromDate, toDate, dateType, customerId, userId));
    }

    @Operation(summary = "Consultar historial de repartos",
            description = "Requiere permiso orders:read. Filtra por fecha de reparto (YYYY-MM-DD), cliente, camión, chofer y estado. Los repartidores solo acceden a sus propios repartos.")
    @GetMapping("/deliveries")
    @PreAuthorize("hasAuthority('orders:read')")
    public ResponseEntity<List<DeliveryHistoryItemResponse>> deliveryHistory(
            @Parameter(description = "Fecha de reparto desde (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Fecha de reparto hasta (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long truckId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getDeliveryHistory(fromDate, toDate, customerId, truckId, driverId, status));
    }

    @Operation(summary = "Obtener detalle de pedido", description = "Requiere permiso orders:read.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('orders:read')")
    public ResponseEntity<OrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.get(id));
    }

    @Operation(summary = "Actualizar pedido pendiente", description = "Requiere permiso orders:write. Solo permite editar pedidos en estado PENDING.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('orders:write')")
    public ResponseEntity<OrderResponse> update(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.update(id, request));
    }

    @Operation(summary = "Marcar pedido en reparto", description = "Requiere permiso orders:deliver. Cambia el estado de PENDING a IN_ROUTE.")
    @PatchMapping("/{id}/in-route")
    @PreAuthorize("hasAuthority('orders:deliver')")
    public ResponseEntity<OrderResponse> markInRoute(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.markInRoute(id));
    }

    @Operation(summary = "Registrar entrega y pago de pedido",
            description = "Requiere permiso orders:deliver. Registra estado DELIVERED, importe recibido (total, parcial o cero en cuenta corriente), medio de pago (CASH, TRANSFER, MIXED, CURRENT_ACCOUNT) e idempotencia ante reintentos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega registrada (o devuelta idempotentemente si ya estaba entregada)"),
            @ApiResponse(responseCode = "400", description = "Importes o medio de pago inválidos, o pedido cancelado"),
            @ApiResponse(responseCode = "403", description = "Sin permiso orders:deliver o camión no asignado al repartidor"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @PatchMapping("/{id}/delivered")
    @PreAuthorize("hasAuthority('orders:deliver')")
    public ResponseEntity<OrderResponse> markDelivered(@PathVariable Long id,
                                                       @Valid @RequestBody OrderDeliveryRequest request) {
        return ResponseEntity.ok(orderService.markDelivered(id, request));
    }

    @Operation(summary = "Registrar devolución de mercadería no entregada",
            description = "Requiere permiso stock:write o orders:deliver. Reintegra la cantidad no entregada al camión asignado al pedido como movimiento histórico inmutable. Rechaza con error 400 si el pedido no tiene camión asignado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Devolución registrada y reintegrada al camión"),
            @ApiResponse(responseCode = "400", description = "Pedido sin camión asignado, cantidad inválida o superior a lo pendiente"),
            @ApiResponse(responseCode = "403", description = "Sin permiso stock:write ni orders:deliver"),
            @ApiResponse(responseCode = "404", description = "Pedido, camión o producto no encontrado")
    })
    @PostMapping("/{id}/returns")
    @PreAuthorize("hasAnyAuthority('stock:write', 'orders:deliver')")
    public ResponseEntity<TruckStockMovementResponse> registerReturn(@PathVariable Long id,
                                                                     @Valid @RequestBody OrderReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deliveryStockService.registerReturnForOrder(id, request));
    }

    @Operation(summary = "Descargar comprobante PDF del pedido",
            description = "Requiere permiso orders:read. Disponible una vez que el pedido fue entregado. Puede descargarse múltiples veces sin volver a registrar el pago.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF descargable generado"),
            @ApiResponse(responseCode = "400", description = "El pedido todavía no fue entregado"),
            @ApiResponse(responseCode = "403", description = "Sin permiso orders:read"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @GetMapping(value = "/{id}/receipt/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('orders:read')")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable Long id) {
        byte[] pdf = receiptPdfService.generateReceipt(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("comprobante-pedido-" + id + ".pdf").build().toString())
                .body(pdf);
    }

    @Operation(summary = "Cancelar pedido", description = "Requiere permiso orders:write. Revierte la carga de stock en el camión si no fue entregado.")
    @PatchMapping("/{id}/cancelled")
    @PreAuthorize("hasAuthority('orders:write')")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }

    @Operation(summary = "Eliminar (cancelar) pedido", description = "Requiere permiso orders:write.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('orders:write')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
