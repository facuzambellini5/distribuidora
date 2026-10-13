package com.example.distribuidora.services;

import com.example.distribuidora.dtos.DeliveryStockRequest;
import com.example.distribuidora.dtos.DeliveryStockResponse;
import com.example.distribuidora.dtos.DeliveryStockUpdateRequest;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.DeliveryStock;
import com.example.distribuidora.models.Product;
import com.example.distribuidora.models.Truck;
import com.example.distribuidora.repositories.IDeliveryStockRepository;
import com.example.distribuidora.repositories.IProductRepository;
import com.example.distribuidora.repositories.ITruckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

//Maneja la relacion camion <-> producto (que se carga en cada camion, en que fecha y cuanto)
@Service
public class DeliveryStockService {

    @Autowired
    private IDeliveryStockRepository stockRepo;

    @Autowired
    private ITruckRepository truckRepo;

    @Autowired
    private IProductRepository productRepo;

    public DeliveryStockResponse load(Long truckId, DeliveryStockRequest request) {

        Truck truck = truckRepo.findById(truckId)
                .orElseThrow(() -> new EntityNotFoundException("Truck", truckId));
        Product product = productRepo.findById(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product", request.productId()));

        if (!truck.isActive()) {
            throw new IllegalArgumentException("Truck " + truckId + " is not active");
        }
        if (!product.isActive()) {
            throw new IllegalArgumentException("Product " + product.getId() + " is not active");
        }

        LocalDate date = request.deliveryDate() != null ? request.deliveryDate() : LocalDate.now();

        //Un producto se carga una sola vez por camion y por dia; para cambiar la cantidad se usa update
        if (stockRepo.existsByTruck_IdAndProduct_IdAndDeliveryDate(truckId, product.getId(), date)) {
            throw new IllegalArgumentException("Product " + product.getId() + " is already loaded in truck "
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
    public List<DeliveryStockResponse> getStockByTruck(Long truckId, LocalDate date) {

        if (!truckRepo.existsById(truckId)) {
            throw new EntityNotFoundException("Truck", truckId);
        }

        LocalDate day = date != null ? date : LocalDate.now();

        return stockRepo.findByTruck_IdAndDeliveryDate(truckId, day)
                .stream()
                .map(DeliveryStockResponse::new)
                .toList();
    }

    public DeliveryStockResponse update(Long stockId, DeliveryStockUpdateRequest request) {

        DeliveryStock stock = this.findStock(stockId);

        if (request.quantityReturned() > request.quantityLoaded()) {
            throw new IllegalArgumentException("Quantity returned can't be greater than quantity loaded");
        }

        stock.setQuantityLoaded(request.quantityLoaded());
        stock.setQuantityReturned(request.quantityReturned());

        return new DeliveryStockResponse(stockRepo.save(stock));
    }

    public void delete(Long stockId) {
        stockRepo.delete(this.findStock(stockId));
    }

    private DeliveryStock findStock(Long stockId) {
        return stockRepo.findById(stockId)
                .orElseThrow(() -> new EntityNotFoundException("DeliveryStock", stockId));
    }
}
