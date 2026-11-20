package com.example.distribuidora.services;

import com.example.distribuidora.dtos.DeliveryStockUpdateRequest;
import com.example.distribuidora.exceptions.BusinessRuleException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock
    private IOrderStockLoadRepository loadRepo;

    @InjectMocks
    private DeliveryStockService deliveryStockService;

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

    private static final LocalDate DATE = LocalDate.of(2026, 10, 13);

    private Truck truck() {
        Truck truck = new Truck();
        truck.setId(1L);
        truck.setActive(true);
        return truck;
    }

    private Product product() {
        Product product = new Product();
        product.setId(7L);
        product.setActive(true);
        return product;
    }

    private Order orderWithItems(Long truckId, int... quantities) {
        Order order = new Order();
        order.setId(100L);
        order.setTruckId(truckId);
        order.setDeliveryDate(DATE);
        for (int quantity : quantities) {
            OrderItem item = new OrderItem();
            item.setProduct(product());
            item.setQuantity(quantity);
            order.getItems().add(item);
        }
        return order;
    }

    private OrderStockLoad load(int quantity, boolean active) {
        OrderStockLoad load = new OrderStockLoad();
        load.setOrderId(100L);
        load.setProductId(7L);
        load.setTruckId(1L);
        load.setDeliveryDate(DATE);
        load.setQuantity(quantity);
        load.setRevertedAt(active ? null : java.time.LocalDateTime.now());
        return load;
    }

    @Test
    void registerLoadCreatesTruckStockFromEmptyTruckAndSumsRepeatedProducts() {
        Truck truck = truck();
        when(truckRepo.findById(1L)).thenReturn(Optional.of(truck));
        when(loadRepo.findByOrderIdAndProductId(100L, 7L)).thenReturn(Optional.empty());
        when(stockRepo.findForUpdate(1L, 7L, DATE)).thenReturn(Optional.empty());
        when(productRepo.findById(7L)).thenReturn(Optional.of(product()));

        deliveryStockService.registerLoadForOrder(orderWithItems(1L, 3, 2));

        ArgumentCaptor<DeliveryStock> stockCaptor = ArgumentCaptor.forClass(DeliveryStock.class);
        verify(stockRepo).save(stockCaptor.capture());
        assertEquals(5, stockCaptor.getValue().getQuantityLoaded());
        assertEquals(0, stockCaptor.getValue().getQuantityReturned());
        assertSame(truck, stockCaptor.getValue().getTruck());

        ArgumentCaptor<OrderStockLoad> loadCaptor = ArgumentCaptor.forClass(OrderStockLoad.class);
        verify(loadRepo).save(loadCaptor.capture());
        assertEquals(5, loadCaptor.getValue().getQuantity());
        assertEquals(100L, loadCaptor.getValue().getOrderId());
        assertTrue(loadCaptor.getValue().isActive());
    }

    @Test
    void registerLoadAddsToExistingTruckStock() {
        DeliveryStock stock = new DeliveryStock();
        stock.setQuantityLoaded(10);
        when(truckRepo.findById(1L)).thenReturn(Optional.of(truck()));
        when(loadRepo.findByOrderIdAndProductId(100L, 7L)).thenReturn(Optional.empty());
        when(stockRepo.findForUpdate(1L, 7L, DATE)).thenReturn(Optional.of(stock));

        deliveryStockService.registerLoadForOrder(orderWithItems(1L, 4));

        assertEquals(14, stock.getQuantityLoaded());
    }

    @Test
    void registerLoadIsIdempotentWhenRetried() {
        when(truckRepo.findById(1L)).thenReturn(Optional.of(truck()));
        when(loadRepo.findByOrderIdAndProductId(100L, 7L)).thenReturn(Optional.of(load(5, true)));

        deliveryStockService.registerLoadForOrder(orderWithItems(1L, 5));

        verify(stockRepo, never()).save(any(DeliveryStock.class));
        verify(loadRepo, never()).save(any(OrderStockLoad.class));
    }

    @Test
    void registerLoadWithoutTruckDoesNothing() {
        deliveryStockService.registerLoadForOrder(orderWithItems(null, 5));

        verifyNoInteractions(truckRepo, stockRepo, loadRepo);
    }

    @Test
    void registerLoadReactivatesRevertedLoadOfTheSameOrder() {
        OrderStockLoad reverted = load(5, false);
        DeliveryStock stock = new DeliveryStock();
        stock.setQuantityLoaded(0);
        when(truckRepo.findById(1L)).thenReturn(Optional.of(truck()));
        when(loadRepo.findByOrderIdAndProductId(100L, 7L)).thenReturn(Optional.of(reverted));
        when(stockRepo.findForUpdate(1L, 7L, DATE)).thenReturn(Optional.of(stock));

        deliveryStockService.registerLoadForOrder(orderWithItems(1L, 5));

        assertEquals(5, stock.getQuantityLoaded());
        assertTrue(reverted.isActive());
    }

    @Test
    void revertLoadReleasesTruckStockAndMarksLoadReverted() {
        OrderStockLoad load = load(3, true);
        DeliveryStock stock = new DeliveryStock();
        stock.setQuantityLoaded(5);
        when(loadRepo.findByOrderIdAndRevertedAtIsNull(100L)).thenReturn(List.of(load));
        when(stockRepo.findForUpdate(1L, 7L, DATE)).thenReturn(Optional.of(stock));

        deliveryStockService.revertLoadForOrder(100L);

        assertEquals(2, stock.getQuantityLoaded());
        assertFalse(load.isActive());
    }

    @Test
    void revertLoadNeverDropsBelowQuantityAlreadyReturned() {
        OrderStockLoad load = load(3, true);
        DeliveryStock stock = new DeliveryStock();
        stock.setQuantityLoaded(5);
        stock.setQuantityReturned(4);
        when(loadRepo.findByOrderIdAndRevertedAtIsNull(100L)).thenReturn(List.of(load));
        when(stockRepo.findForUpdate(1L, 7L, DATE)).thenReturn(Optional.of(stock));

        deliveryStockService.revertLoadForOrder(100L);

        assertEquals(4, stock.getQuantityLoaded());
    }

    @Test
    void revertLoadIsIdempotentWhenNothingIsActive() {
        when(loadRepo.findByOrderIdAndRevertedAtIsNull(100L)).thenReturn(List.of());

        deliveryStockService.revertLoadForOrder(100L);

        verifyNoInteractions(stockRepo);
    }
}
