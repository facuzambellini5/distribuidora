package com.example.distribuidora.services;

import com.example.distribuidora.dtos.OrderItemRequest;
import com.example.distribuidora.dtos.OrderRequest;
import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.models.*;
import com.example.distribuidora.repositories.IOrderRepository;
import com.example.distribuidora.repositories.IUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private IOrderRepository orderRepo;
    @Mock
    private CustomerService customerService;
    @Mock
    private AddressService addressService;
    @Mock
    private ProductService productService;
    @Mock
    private IUserRepository userRepo;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private DeliveryStockService deliveryStockService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createCalculatesItemSubtotalsAndTotalFromActiveProducts() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setBusinessName("Cafe Centro");
        Address address = new Address();
        address.setId(2L);
        address.setCustomer(customer);
        address.setStreetAddress("Av Siempre Viva 123");

        Product product = new Product();
        product.setId(3L);
        product.setName("Cafe molido");
        product.setActive(true);
        product.setUnitPrice(new BigDecimal("15.50"));

        when(customerService.getCustomerEntity(1L)).thenReturn(customer);
        when(addressService.getAddressEntity(1L, 2L)).thenReturn(address);
        when(productService.getProductEntity(3L)).thenReturn(product);
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate deliveryDate = LocalDate.of(2026, 10, 13);
        orderService.create(new OrderRequest(1L, 2L, null, deliveryDate, null, List.of(new OrderItemRequest(3L, 2))));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepo).save(captor.capture());
        Order saved = captor.getValue();
        assertEquals(OrderStatus.PENDING, saved.getStatus());
        assertEquals(deliveryDate, saved.getDeliveryDate());
        assertEquals(new BigDecimal("31.00"), saved.getTotalAmount());
        assertEquals("Av Siempre Viva 123", saved.getDeliveryAddressSnapshot());
        assertEquals(new BigDecimal("31.00"), saved.getItems().getFirst().getSubtotal());
    }

    @Test
    void markDeliveredRegistersLedgerCharge() {
        Customer customer = new Customer();
        customer.setId(1L);
        Order order = new Order();
        order.setId(5L);
        order.setCustomer(customer);
        order.setDeliveryDate(LocalDate.of(2026, 10, 13));
        order.setStatus(OrderStatus.IN_ROUTE);
        order.setTotalAmount(new BigDecimal("42.00"));

        when(orderRepo.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        orderService.markDelivered(5L);

        verify(deliveryStockService).validateStockForOrder(null, LocalDate.of(2026, 10, 13), List.of(), 5L);
        verify(ledgerService).registerCharge(customer, order, new BigDecimal("42.00"));
    }
}
