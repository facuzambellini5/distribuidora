package com.example.distribuidora.services;

import com.example.distribuidora.dtos.DeliveryStockRequest;
import com.example.distribuidora.dtos.DeliveryStockResponse;
import com.example.distribuidora.dtos.DeliveryStockUpdateRequest;
import com.example.distribuidora.dtos.OrderItemRequest;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.exceptions.DuplicateResourceException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.DeliveryStock;
import com.example.distribuidora.models.Product;
import com.example.distribuidora.models.Truck;
import com.example.distribuidora.repositories.IDeliveryStockRepository;
import com.example.distribuidora.repositories.IOrderRepository;
import com.example.distribuidora.repositories.IProductRepository;
import com.example.distribuidora.repositories.ITruckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

//Maneja la relacion camion <-> producto (que se carga en cada camion, en que fecha y cuanto)
@Service
public class DeliveryStockService {

    private final IDeliveryStockRepository stockRepo;
    private final ITruckRepository truckRepo;
    private final IProductRepository productRepo;
    private final IOrderRepository orderRepo;

    public DeliveryStockService(IDeliveryStockRepository stockRepo,
                                ITruckRepository truckRepo,
                                IProductRepository productRepo,
                                IOrderRepository orderRepo) {
        this.stockRepo = stockRepo;
        this.truckRepo = truckRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
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

    @Transactional(readOnly = true)
    public void validateStockForOrder(Long truckId, LocalDate deliveryDate, List<OrderItemRequest> items, Long excludedOrderId) {
        if (truckId == null) {
            return;
        }
        if (!truckRepo.existsById(truckId)) {
            throw new EntityNotFoundException("Truck", truckId);
        }
        LocalDate date = deliveryDate != null ? deliveryDate : LocalDate.now();

        for (OrderItemRequest item : items) {
            int loadedAvailable = stockRepo.findByTruck_IdAndDeliveryDate(truckId, date)
                    .stream()
                    .filter(stock -> stock.getProduct().getId().equals(item.productId()))
                    .mapToInt(stock -> stock.getQuantityLoaded() - stock.getQuantityReturned())
                    .sum();
            int reservedByOtherOrders = orderRepo.sumReservedQuantity(truckId, date, item.productId(), excludedOrderId);
            int available = loadedAvailable - reservedByOtherOrders;
            if (available < item.quantity()) {
                throw new BusinessRuleException("Insufficient stock in truck " + truckId
                        + " for product " + item.productId()
                        + " on " + date
                        + ". Available: " + available
                        + ", requested: " + item.quantity());
            }
        }
    }

    private DeliveryStock findStock(Long stockId) {
        return stockRepo.findById(stockId)
                .orElseThrow(() -> new EntityNotFoundException("DeliveryStock", stockId));
    }
}
