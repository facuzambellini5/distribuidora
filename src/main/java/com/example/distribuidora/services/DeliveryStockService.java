package com.example.distribuidora.services;

import com.example.distribuidora.dtos.DeliveryStockRequest;
import com.example.distribuidora.dtos.DeliveryStockResponse;
import com.example.distribuidora.dtos.DeliveryStockUpdateRequest;
import com.example.distribuidora.dtos.OrderReturnRequest;
import com.example.distribuidora.dtos.TruckStockHistoryResponse;
import com.example.distribuidora.dtos.TruckStockMovementResponse;
import com.example.distribuidora.dtos.TruckStockSummaryItemResponse;
import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.enums.StockMovementType;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.exceptions.DuplicateResourceException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.DeliveryStock;
import com.example.distribuidora.models.Order;
import com.example.distribuidora.models.OrderItem;
import com.example.distribuidora.models.OrderStockLoad;
import com.example.distribuidora.models.Product;
import com.example.distribuidora.models.Truck;
import com.example.distribuidora.models.TruckStockMovement;
import com.example.distribuidora.models.User;
import com.example.distribuidora.repositories.IDeliveryStockRepository;
import com.example.distribuidora.repositories.IOrderRepository;
import com.example.distribuidora.repositories.IOrderStockLoadRepository;
import com.example.distribuidora.repositories.IProductRepository;
import com.example.distribuidora.repositories.ITruckRepository;
import com.example.distribuidora.repositories.ITruckStockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Maneja la relacion camion <-> producto y el historial inmutable de movimientos (solo stock por camion, sin stock central)
@Service
public class DeliveryStockService {

    private final IDeliveryStockRepository stockRepo;
    private final ITruckRepository truckRepo;
    private final IProductRepository productRepo;
    private final IOrderRepository orderRepo;
    private final IOrderStockLoadRepository loadRepo;
    private final ITruckStockMovementRepository movementRepo;
    private final CurrentUserService currentUserService;

    @Autowired
    public DeliveryStockService(IDeliveryStockRepository stockRepo,
                                ITruckRepository truckRepo,
                                IProductRepository productRepo,
                                IOrderRepository orderRepo,
                                IOrderStockLoadRepository loadRepo,
                                ITruckStockMovementRepository movementRepo,
                                CurrentUserService currentUserService) {
        this.stockRepo = stockRepo;
        this.truckRepo = truckRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.loadRepo = loadRepo;
        this.movementRepo = movementRepo;
        this.currentUserService = currentUserService;
    }

    public DeliveryStockService(IDeliveryStockRepository stockRepo,
                                ITruckRepository truckRepo,
                                IProductRepository productRepo,
                                IOrderRepository orderRepo,
                                IOrderStockLoadRepository loadRepo) {
        this(stockRepo, truckRepo, productRepo, orderRepo, loadRepo, null, null);
    }

    @Transactional
    public DeliveryStockResponse load(Long truckId, DeliveryStockRequest request) {
        Truck truck = truckRepo.findById(truckId)
                .orElseThrow(() -> new EntityNotFoundException("Truck", truckId));
        Product product = productRepo.findById(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product", request.productId()));

        if (!truck.isActive()) {
            throw new BusinessRuleException("Truck " + truckId + " is not active");
        }
        if (!product.isActive()) {
            throw new BusinessRuleException("Product " + product.getId() + " is not active");
        }

        LocalDate date = request.deliveryDate() != null ? request.deliveryDate() : LocalDate.now();

        if (stockRepo.existsByTruck_IdAndProduct_IdAndDeliveryDate(truckId, product.getId(), date)) {
            throw new DuplicateResourceException("Product " + product.getId() + " is already loaded in truck "
                    + truckId + " for " + date + ". Use the update endpoint to change the quantity");
        }

        DeliveryStock stock = new DeliveryStock();
        stock.setTruck(truck);
        stock.setProduct(product);
        stock.setDeliveryDate(date);
        stock.setQuantityLoaded(request.quantityLoaded());

        DeliveryStock saved = stockRepo.save(stock);
        recordMovement(truck, product, null, StockMovementType.LOAD, request.quantityLoaded(), date,
                "Carga manual de stock en camión", null);
        return new DeliveryStockResponse(saved);
    }

    // Sin fecha: devuelve lo cargado hoy
    @Transactional(readOnly = true)
    public List<DeliveryStockResponse> getStockByTruck(Long truckId, LocalDate date) {
        Truck truck = truckRepo.findById(truckId)
                .orElseThrow(() -> new EntityNotFoundException("Truck", truckId));
        if (!truck.isActive()) {
            throw new BusinessRuleException("Truck " + truckId + " is not active");
        }

        LocalDate day = date != null ? date : LocalDate.now();

        return stockRepo.findByTruck_IdAndDeliveryDate(truckId, day)
                .stream()
                .map(DeliveryStockResponse::new)
                .toList();
    }

    // Devuelve historial de movimientos y saldos resultantes del camion filtrables por producto, fecha y usuario
    @Transactional(readOnly = true)
    public TruckStockHistoryResponse getStockHistory(Long truckId,
                                                     Long productId,
                                                     LocalDate fromDate,
                                                     LocalDate toDate,
                                                     Long userId,
                                                     StockMovementType movementType) {
        Truck truck = truckRepo.findById(truckId)
                .orElseThrow(() -> new EntityNotFoundException("Truck", truckId));
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("La fecha desde debe ser anterior o igual a la fecha hasta.");
        }

        List<TruckStockMovement> movements = movementRepo == null
                ? List.of()
                : movementRepo.search(truckId, productId, fromDate, toDate, userId, movementType);

        Map<Long, ProductStockAccumulator> byProduct = new LinkedHashMap<>();
        for (TruckStockMovement movement : movements) {
            Product product = movement.getProduct();
            ProductStockAccumulator acc = byProduct.computeIfAbsent(product.getId(),
                    id -> new ProductStockAccumulator(product));
            switch (movement.getMovementType()) {
                case LOAD -> acc.loaded += movement.getQuantity();
                case DELIVERY -> acc.delivered += movement.getQuantity();
                case RETURN -> acc.returned += movement.getQuantity();
            }
        }

        // Si no hubo movimientos cargados aun pero existe stock en delivery_stock para la fecha, se incluye el saldo base
        if (byProduct.isEmpty() && movementType == null && userId == null) {
            LocalDate targetDate = fromDate != null ? fromDate : LocalDate.now();
            if (toDate == null || toDate.equals(targetDate)) {
                for (DeliveryStock ds : stockRepo.findByTruck_IdAndDeliveryDate(truckId, targetDate)) {
                    if (productId != null && !ds.getProduct().getId().equals(productId)) {
                        continue;
                    }
                    ProductStockAccumulator acc = byProduct.computeIfAbsent(ds.getProduct().getId(),
                            id -> new ProductStockAccumulator(ds.getProduct()));
                    acc.loaded += ds.getQuantityLoaded();
                    acc.returned += ds.getQuantityReturned();
                }
            }
        }

        List<TruckStockSummaryItemResponse> balances = byProduct.values().stream()
                .map(ProductStockAccumulator::toResponse)
                .sorted(Comparator.comparing(TruckStockSummaryItemResponse::productName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<TruckStockMovementResponse> movementResponses = movements.stream()
                .map(TruckStockMovementResponse::new)
                .toList();

        return new TruckStockHistoryResponse(
                truck.getId(),
                truck.getLicensePlate(),
                fromDate,
                toDate,
                balances,
                movementResponses
        );
    }

    @Transactional(readOnly = true)
    public List<TruckStockMovementResponse> getMovements(Long truckId,
                                                         Long productId,
                                                         LocalDate fromDate,
                                                         LocalDate toDate,
                                                         Long userId,
                                                         StockMovementType movementType) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("La fecha desde debe ser anterior o igual a la fecha hasta.");
        }
        if (movementRepo == null) {
            return List.of();
        }
        return movementRepo.search(truckId, productId, fromDate, toDate, userId, movementType)
                .stream()
                .map(TruckStockMovementResponse::new)
                .toList();
    }

    // Registra la devolucion de mercaderia no entregada de un pedido y la reintegra al stock del camion asignado.
    // No existe inventario central: si el pedido no tiene camion asignado, se rechaza con error claro.
    @Transactional
    public TruckStockMovementResponse registerReturnForOrder(Long orderId, OrderReturnRequest request) {
        if (request == null || request.productId() == null) {
            throw new BusinessRuleException("Debe indicar el producto a devolver.");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BusinessRuleException("La cantidad devuelta debe ser mayor que cero.");
        }

        if (movementRepo != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            var existing = movementRepo.findByIdempotencyKey(request.idempotencyKey().trim());
            if (existing.isPresent()) {
                return new TruckStockMovementResponse(existing.get());
            }
        }

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        if (order.getTruckId() == null) {
            throw new BusinessRuleException(
                    "No se puede registrar la devolución porque el pedido " + order.getOrderCode()
                            + " no tiene un camión asignado y no existe inventario central al que reintegrar mercadería.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("No se pueden registrar devoluciones sobre un pedido cancelado.");
        }
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleException(
                    "El pedido ya fue entregado por completo y no tiene cantidades pendientes de entrega.");
        }

        int orderedQuantity = quantitiesByProduct(order).getOrDefault(request.productId(), 0);
        if (orderedQuantity <= 0) {
            throw new BusinessRuleException(
                    "El producto " + request.productId() + " no forma parte del pedido " + order.getOrderCode() + ".");
        }

        int alreadyReturned = movementRepo == null ? 0
                : movementRepo.sumQuantityByOrderAndProductAndType(orderId, request.productId(), StockMovementType.RETURN);
        int pendingQuantity = orderedQuantity - alreadyReturned;

        if (pendingQuantity <= 0) {
            throw new BusinessRuleException(
                    "Ya se devolvió la totalidad de las unidades pendientes para el producto "
                            + request.productId() + " en el pedido " + order.getOrderCode() + ".");
        }
        if (request.quantity() > pendingQuantity) {
            throw new BusinessRuleException(
                    "La cantidad devuelta (" + request.quantity()
                            + ") supera la cantidad pendiente de entrega (" + pendingQuantity + ").");
        }

        Truck truck = truckRepo.findById(order.getTruckId())
                .orElseThrow(() -> new EntityNotFoundException("Truck", order.getTruckId()));
        Product product = productRepo.findById(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product", request.productId()));

        LocalDate movementDate = order.getDeliveryDate() != null ? order.getDeliveryDate() : LocalDate.now();

        DeliveryStock stock = stockRepo.findForUpdate(truck.getId(), product.getId(), movementDate)
                .orElseGet(() -> {
                    DeliveryStock created = new DeliveryStock();
                    created.setTruck(truck);
                    created.setProduct(product);
                    created.setDeliveryDate(movementDate);
                    created.setQuantityLoaded(orderedQuantity);
                    return created;
                });

        int newReturned = stock.getQuantityReturned() + request.quantity();
        if (newReturned > stock.getQuantityLoaded()) {
            throw new BusinessRuleException(
                    "La devolución acumulada (" + newReturned
                            + ") supera la cantidad cargada en el camión (" + stock.getQuantityLoaded() + ").");
        }
        stock.setQuantityReturned(newReturned);
        stockRepo.save(stock);

        String notes = (request.notes() != null && !request.notes().isBlank())
                ? request.notes().trim()
                : "Devolución de mercadería no entregada del pedido " + order.getOrderCode();

        String normalizedKey = (request.idempotencyKey() != null && !request.idempotencyKey().isBlank())
                ? request.idempotencyKey().trim()
                : null;

        TruckStockMovement savedMovement = recordMovement(
                truck, product, order, StockMovementType.RETURN, request.quantity(), movementDate, notes, normalizedKey);
        return new TruckStockMovementResponse(savedMovement);
    }

    @Transactional
    public void registerDeliveryForOrder(Order order) {
        if (order.getTruckId() == null || movementRepo == null) {
            return;
        }
        Truck truck = truckRepo.findById(order.getTruckId())
                .orElseThrow(() -> new EntityNotFoundException("Truck", order.getTruckId()));
        LocalDate date = order.getDeliveryDate() != null ? order.getDeliveryDate() : LocalDate.now();

        for (Map.Entry<Long, Integer> entry : quantitiesByProduct(order).entrySet()) {
            Long productId = entry.getKey();
            int ordered = entry.getValue();
            int returned = movementRepo.sumQuantityByOrderAndProductAndType(order.getId(), productId, StockMovementType.RETURN);
            int alreadyDelivered = movementRepo.sumQuantityByOrderAndProductAndType(order.getId(), productId, StockMovementType.DELIVERY);
            int netToDeliver = ordered - returned - alreadyDelivered;
            if (netToDeliver > 0) {
                Product product = productRepo.findById(productId)
                        .orElseThrow(() -> new EntityNotFoundException("Product", productId));
                recordMovement(truck, product, order, StockMovementType.DELIVERY, netToDeliver, date,
                        "Entrega del pedido " + order.getOrderCode(), null);
            }
        }
    }

    @Transactional
    public DeliveryStockResponse update(Long stockId, DeliveryStockUpdateRequest request) {
        DeliveryStock stock = this.findStock(stockId);

        if (request.quantityReturned() > request.quantityLoaded()) {
            throw new BusinessRuleException("Quantity returned can't be greater than quantity loaded");
        }

        int reserved = orderRepo.sumReservedQuantity(
                stock.getTruck().getId(),
                stock.getDeliveryDate(),
                stock.getProduct().getId(),
                null
        );
        int availableAfterReturn = request.quantityLoaded() - request.quantityReturned();
        if (availableAfterReturn < reserved) {
            throw new BusinessRuleException("Cannot reduce stock below already assigned order quantity. Reserved: " + reserved);
        }

        int previousLoaded = stock.getQuantityLoaded();
        stock.setQuantityLoaded(request.quantityLoaded());
        stock.setQuantityReturned(request.quantityReturned());

        DeliveryStock saved = stockRepo.save(stock);
        if (request.quantityLoaded() > previousLoaded) {
            recordMovement(stock.getTruck(), stock.getProduct(), null, StockMovementType.LOAD,
                    request.quantityLoaded() - previousLoaded, stock.getDeliveryDate(),
                    "Ampliación de carga en camión", null);
        }
        return new DeliveryStockResponse(saved);
    }

    @Transactional
    public DeliveryStockResponse update(Long truckId, Long stockId, DeliveryStockUpdateRequest request) {
        DeliveryStock stock = this.findStock(stockId);
        if (!stock.getTruck().getId().equals(truckId)) {
            throw new EntityNotFoundException("DeliveryStock", stockId);
        }
        return update(stockId, request);
    }

    @Transactional
    public void delete(Long stockId) {
        DeliveryStock stock = this.findStock(stockId);
        int reserved = orderRepo.sumReservedQuantity(
                stock.getTruck().getId(),
                stock.getDeliveryDate(),
                stock.getProduct().getId(),
                null
        );
        if (reserved > 0) {
            throw new BusinessRuleException("Cannot delete stock assigned to existing orders. Reserved: " + reserved);
        }
        stockRepo.delete(stock);
    }

    @Transactional
    public void delete(Long truckId, Long stockId) {
        DeliveryStock stock = this.findStock(stockId);
        if (!stock.getTruck().getId().equals(truckId)) {
            throw new EntityNotFoundException("DeliveryStock", stockId);
        }
        delete(stockId);
    }

    // ------------------------------------------------------------------
    // Cargas originadas por pedidos (no hay stock central: el camion arranca en cero)
    // ------------------------------------------------------------------

    @Transactional
    public void registerLoadForOrder(Order order) {
        Long truckId = order.getTruckId();
        if (truckId == null) {
            return;
        }
        Truck truck = truckRepo.findById(truckId)
                .orElseThrow(() -> new EntityNotFoundException("Truck", truckId));
        LocalDate date = order.getDeliveryDate() != null ? order.getDeliveryDate() : LocalDate.now();

        for (Map.Entry<Long, Integer> entry : quantitiesByProduct(order).entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            OrderStockLoad load = loadRepo.findByOrderIdAndProductId(order.getId(), productId).orElse(null);
            if (load != null && load.isActive()) {
                if (load.getTruckId().equals(truckId) && load.getDeliveryDate().equals(date)
                        && load.getQuantity() == quantity) {
                    continue;
                }
                releaseFromTruck(load);
            }
            if (load == null) {
                load = new OrderStockLoad();
                load.setOrderId(order.getId());
                load.setProductId(productId);
            }

            Product product = productRepo.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product", productId));

            DeliveryStock stock = stockRepo.findForUpdate(truckId, productId, date).orElseGet(() -> {
                DeliveryStock created = new DeliveryStock();
                created.setTruck(truck);
                created.setProduct(product);
                created.setDeliveryDate(date);
                return created;
            });
            stock.setQuantityLoaded(stock.getQuantityLoaded() + quantity);
            stockRepo.save(stock);

            load.setTruckId(truckId);
            load.setDeliveryDate(date);
            load.setQuantity(quantity);
            load.setRevertedAt(null);
            loadRepo.save(load);

            recordMovement(truck, product, order, StockMovementType.LOAD, quantity, date,
                    "Carga asignada para pedido " + order.getOrderCode(), null);
        }
    }

    @Transactional
    public void revertLoadForOrder(Long orderId) {
        for (OrderStockLoad load : loadRepo.findByOrderIdAndRevertedAtIsNull(orderId)) {
            releaseFromTruck(load);
        }
    }

    @Transactional
    public void syncLoadForOrder(Order order) {
        revertLoadForOrder(order.getId());
        registerLoadForOrder(order);
    }

    private void releaseFromTruck(OrderStockLoad load) {
        stockRepo.findForUpdate(load.getTruckId(), load.getProductId(), load.getDeliveryDate()).ifPresent(stock -> {
            stock.setQuantityLoaded(Math.max(stock.getQuantityReturned(), stock.getQuantityLoaded() - load.getQuantity()));
            stockRepo.save(stock);
        });
        load.setRevertedAt(LocalDateTime.now());
        loadRepo.save(load);
    }

    private TruckStockMovement recordMovement(Truck truck,
                                              Product product,
                                              Order order,
                                              StockMovementType type,
                                              int quantity,
                                              LocalDate movementDate,
                                              String notes,
                                              String idempotencyKey) {
        if (movementRepo == null) {
            return null;
        }
        TruckStockMovement movement = new TruckStockMovement();
        movement.setTruck(truck);
        movement.setProduct(product);
        movement.setDeliveryOrder(order);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setMovementDate(movementDate);
        User currentUser = currentUserService != null ? currentUserService.getCurrentUserOrNull() : null;
        movement.setCreatedBy(currentUser);
        movement.setNotes(notes);
        movement.setIdempotencyKey(idempotencyKey);
        return movementRepo.save(movement);
    }

    private Map<Long, Integer> quantitiesByProduct(Order order) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            quantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private DeliveryStock findStock(Long stockId) {
        return stockRepo.findById(stockId)
                .orElseThrow(() -> new EntityNotFoundException("DeliveryStock", stockId));
    }

    private static class ProductStockAccumulator {
        private final Product product;
        private int loaded;
        private int delivered;
        private int returned;

        private ProductStockAccumulator(Product product) {
            this.product = product;
        }

        private TruckStockSummaryItemResponse toResponse() {
            int balance = Math.max(0, loaded - delivered);
            return new TruckStockSummaryItemResponse(
                    product.getId(),
                    product.getName(),
                    product.getUnit(),
                    loaded,
                    delivered,
                    returned,
                    balance
            );
        }
    }
}
