package com.example.distribuidora.services;

import com.example.distribuidora.dtos.DeliveryStockUpdateRequest;
import com.example.distribuidora.dtos.OrderItemRequest;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.models.DeliveryStock;
import com.example.distribuidora.models.Product;
import com.example.distribuidora.models.Truck;
import com.example.distribuidora.repositories.IDeliveryStockRepository;
import com.example.distribuidora.repositories.IOrderRepository;
import com.example.distribuidora.repositories.IProductRepository;
import com.example.distribuidora.repositories.ITruckRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryStockServiceTest {

    @Mock
    private IDeliveryStockRepository stockRepo;
    @Mock
    private ITruckRepository truckRepo;
    @Mock
    private IProductRepository productRepo;
    @Mock
    private IOrderRepository orderRepo;

    @InjectMocks
    private DeliveryStockService deliveryStockService;

    @Test
    void validateStockForOrderRejectsWhenTruckHasInsufficientAvailableProduct() {
        Truck truck = new Truck();
        truck.setId(1L);
        truck.setActive(true);
        Product product = new Product();
        product.setId(7L);
        DeliveryStock stock = new DeliveryStock();
        stock.setTruck(truck);
        stock.setProduct(product);
        stock.setQuantityLoaded(5);
        stock.setQuantityReturned(1);

        LocalDate date = LocalDate.of(2026, 10, 13);
        when(truckRepo.findById(1L)).thenReturn(Optional.of(truck));
        when(stockRepo.findByTruck_IdAndDeliveryDate(1L, date)).thenReturn(List.of(stock));
        when(orderRepo.sumReservedQuantity(1L, date, 7L, null)).thenReturn(2);

        assertThrows(BusinessRuleException.class, () ->
                deliveryStockService.validateStockForOrder(1L, date, List.of(new OrderItemRequest(7L, 3)), null)
        );
    }

    @Test
    void updateRejectsReducingLoadedQuantityBelowReservedOrders() {
        Truck truck = new Truck();
        truck.setId(1L);
        Product product = new Product();
        product.setId(7L);
        DeliveryStock stock = new DeliveryStock();
        stock.setId(10L);
        stock.setTruck(truck);
        stock.setProduct(product);
        stock.setDeliveryDate(LocalDate.of(2026, 10, 13));

        when(stockRepo.findById(10L)).thenReturn(Optional.of(stock));
        when(orderRepo.sumReservedQuantity(1L, LocalDate.of(2026, 10, 13), 7L, null)).thenReturn(4);

        assertThrows(BusinessRuleException.class, () ->
                deliveryStockService.update(10L, new DeliveryStockUpdateRequest(3, 0))
        );
    }
}
