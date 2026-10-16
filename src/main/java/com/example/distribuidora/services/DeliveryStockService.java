package com.example.distribuidora.services;

import com.example.distribuidora.dtos.DeliveryStockRequest;
import com.example.distribuidora.dtos.DeliveryStockResponse;
import com.example.distribuidora.dtos.DeliveryStockUpdateRequest;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.exceptions.DuplicateResourceException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.DeliveryStock;
import com.example.distribuidora.models.Order;
import com.example.distribuidora.models.OrderItem;
import com.example.distribuidora.models.OrderStockLoad;
import com.example.distribuidora.models.Product;
import com.example.distribuidora.models.Truck;
import com.example.distribuidora.repositories.IDeliveryStockRepository;
import com.example.distribuidora.repositories.IOrderRepository;
import com.example.distribuidora.repositories.IOrderStockLoadRepository;
import com.example.distribuidora.repositories.IProductRepository;
import com.example.distribuidora.repositories.ITruckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Maneja la relacion camion <-> producto (que se carga en cada camion, en que fecha y cuanto)
@Service
public class DeliveryStockService {

    private final IDeliveryStockRepository stockRepo;
    private final ITruckRepository truckRepo;
    private final IProductRepository productRepo;
    private final IOrderRepository orderRepo;
    private final IOrderStockLoadRepository loadRepo;

    public DeliveryStockService(IDeliveryStockRepository stockRepo,
                                ITruckRepository truckRepo,
                                IProductRepository productRepo,
                                IOrderRepository orderRepo,
                                IOrderStockLoadRepository loadRepo) {
        this.stockRepo = stockRepo;
        this.truckRepo = truckRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.loadRepo = loadRepo;
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

        //Un producto se carga una sola vez por camion y por dia; para cambiar la cantidad se usa update
        if (stockRepo.existsByTruck_IdAndProduct_IdAndDeliveryDate(truckId, product.getId(), date)) {
            throw new DuplicateResourceException("Product " + product.getId() + " is already loaded in truck "
                    + truckId + " for " + date + ". Use the update endpoint to change the quantity");
        }

        DeliveryStock stock = new DeliveryStock();
        stock.setTruck(truck);
        stock.setProduct(product);
        stock.setDeliveryDate(date);
        stock.setQuantityLoaded(request.quantityLoaded());

        return new DeliveryStockResponse(stockRepo.save(stock));
    }

    //Sin fecha: devuelve lo cargado hoy
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

        stock.setQuantityLoaded(request.quantityLoaded());
        stock.setQuantityReturned(request.quantityReturned());

        return new DeliveryStockResponse(stockRepo.save(stock));
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

    // ------------------------------------------------------------------
    // Cargas originadas por pedidos (no hay stock central: el camion arranca en cero)
    // ------------------------------------------------------------------

    // Carga en el camion las cantidades del pedido. Idempotente: si el pedido ya tiene su carga vigente
    // (mismo camion, fecha y cantidad) no vuelve a cargar. Sin camion asignado no hace nada.
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
                    continue; // reintento: ya esta cargado
                }
                releaseFromTruck(load); // la carga vigente no coincide: se revierte y se vuelve a aplicar
            }
            if (load == null) {
                load = new OrderStockLoad();
                load.setOrderId(order.getId());
                load.setProductId(productId);
            }

            DeliveryStock stock = stockRepo.findForUpdate(truckId, productId, date).orElseGet(() -> {
                DeliveryStock created = new DeliveryStock();
                created.setTruck(truck);
                created.setProduct(productRepo.findById(productId)
                        .orElseThrow(() -> new EntityNotFoundException("Product", productId)));
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
        }
    }

    // Revierte las cargas vigentes del pedido. Idempotente: las ya revertidas no se tocan.
    @Transactional
    public void revertLoadForOrder(Long orderId) {
        for (OrderStockLoad load : loadRepo.findByOrderIdAndRevertedAtIsNull(orderId)) {
            releaseFromTruck(load);
        }
    }

    // Reasignacion / edicion de un pedido PENDING: revierte lo anterior y aplica lo nuevo.
    @Transactional
    public void syncLoadForOrder(Order order) {
        revertLoadForOrder(order.getId());
        registerLoadForOrder(order);
    }

    private void releaseFromTruck(OrderStockLoad load) {
        stockRepo.findForUpdate(load.getTruckId(), load.getProductId(), load.getDeliveryDate()).ifPresent(stock -> {
            // nunca por debajo de lo ya devuelto por deposito
            stock.setQuantityLoaded(Math.max(stock.getQuantityReturned(), stock.getQuantityLoaded() - load.getQuantity()));
            stockRepo.save(stock);
        });
        load.setRevertedAt(LocalDateTime.now());
        loadRepo.save(load);
    }

    // Un mismo producto puede venir repetido en los items: se suma para respetar la clave (pedido, producto).
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
}
