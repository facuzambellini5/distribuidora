package com.example.distribuidora.services;

import com.example.distribuidora.dtos.OrderItemRequest;
import com.example.distribuidora.dtos.OrderRequest;
import com.example.distribuidora.dtos.OrderDeliveryRequest;
import com.example.distribuidora.enums.OrderPaymentMethod;
import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.exceptions.BusinessRuleException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        assertEquals(2L, saved.getDeliveryAddress().getId());
        assertEquals("Av Siempre Viva 123", saved.getDeliveryAddress().getStreetAddress());
        assertEquals(new BigDecimal("31.00"), saved.getItems().getFirst().getSubtotal());
        verify(deliveryStockService).registerLoadForOrder(saved);
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

        // entregar no mueve stock: la carga se registro al asignar el pedido
        verifyNoInteractions(deliveryStockService);
        verify(ledgerService).registerCharge(customer, order, new BigDecimal("42.00"));
        verify(ledgerService).registerOrderPayment(order, BigDecimal.ZERO);
        assertEquals(OrderPaymentMethod.CURRENT_ACCOUNT, order.getPaymentMethod());
        assertEquals(BigDecimal.ZERO, order.getCashAmount());
        assertEquals(BigDecimal.ZERO, order.getTransferAmount());
    }

    @Test
    void markDeliveredWithCashStoresCashAndPaysFullOrderTotal() {
        Order order = orderWithTotal(20L, new BigDecimal("42.00"));
        when(orderRepo.findById(20L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        orderService.markDelivered(20L, new OrderDeliveryRequest(OrderPaymentMethod.CASH, null, null));

        assertEquals(OrderPaymentMethod.CASH, order.getPaymentMethod());
        assertEquals(new BigDecimal("42.00"), order.getCashAmount());
        assertEquals(BigDecimal.ZERO, order.getTransferAmount());
        verify(ledgerService).registerOrderPayment(order, new BigDecimal("42.00"));
    }

    @Test
    void markDeliveredWithTransferStoresTransferAndPaysFullOrderTotal() {
        Order order = orderWithTotal(21L, new BigDecimal("42.00"));
        when(orderRepo.findById(21L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        orderService.markDelivered(21L, new OrderDeliveryRequest(OrderPaymentMethod.TRANSFER, null, null));

        assertEquals(OrderPaymentMethod.TRANSFER, order.getPaymentMethod());
        assertEquals(BigDecimal.ZERO, order.getCashAmount());
        assertEquals(new BigDecimal("42.00"), order.getTransferAmount());
        verify(ledgerService).registerOrderPayment(order, new BigDecimal("42.00"));
    }

    @Test
    void markDeliveredWithMixedPaymentPersistsBothAmountsAndPaysTheirSum() {
        Order order = orderWithTotal(22L, new BigDecimal("42.00"));
        when(orderRepo.findById(22L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        orderService.markDelivered(22L, new OrderDeliveryRequest(OrderPaymentMethod.MIXED,
                new BigDecimal("12.00"), new BigDecimal("30.00")));

        assertEquals(OrderPaymentMethod.MIXED, order.getPaymentMethod());
        assertEquals(new BigDecimal("12.00"), order.getCashAmount());
        assertEquals(new BigDecimal("30.00"), order.getTransferAmount());
        verify(ledgerService).registerOrderPayment(order, new BigDecimal("42.00"));
    }

    @Test
    void mixedPaymentMustEqualOrderTotalAndDoesNotDeliverWhenInvalid() {
        Order order = orderWithTotal(23L, new BigDecimal("42.00"));
        when(orderRepo.findById(23L)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class, () -> orderService.markDelivered(23L,
                new OrderDeliveryRequest(OrderPaymentMethod.MIXED,
                        new BigDecimal("12.00"), new BigDecimal("29.00"))));

        assertEquals(OrderStatus.IN_ROUTE, order.getStatus());
        verify(orderRepo, never()).save(any(Order.class));
        verifyNoInteractions(ledgerService);
    }

    @Test
    void mixedPaymentRequiresBothMethodsAndRejectsNegativeAmounts() {
        Order order = orderWithTotal(24L, new BigDecimal("42.00"));
        when(orderRepo.findById(24L)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class, () -> orderService.markDelivered(24L,
                new OrderDeliveryRequest(OrderPaymentMethod.MIXED, BigDecimal.ZERO, new BigDecimal("42.00"))));
        assertThrows(BusinessRuleException.class, () -> orderService.markDelivered(24L,
                new OrderDeliveryRequest(OrderPaymentMethod.MIXED,
                        new BigDecimal("50.00"), new BigDecimal("-8.00"))));

        verify(orderRepo, never()).save(any(Order.class));
        verifyNoInteractions(ledgerService);
    }

    @Test
    void deliveringAlreadyDeliveredOrderDoesNotRegisterAnotherChargeOrPayment() {
        Order order = orderWithStatus(25L, OrderStatus.DELIVERED);
        when(orderRepo.findById(25L)).thenReturn(Optional.of(order));

        orderService.markDelivered(25L, new OrderDeliveryRequest(OrderPaymentMethod.CASH, null, null));

        verify(orderRepo, never()).save(any(Order.class));
        verifyNoInteractions(ledgerService);
    }

    @Test
    void getOrdersByTruckDelegatesTruckFilterToRepository() {
        when(orderRepo.search(null, 5L, null)).thenReturn(List.of());

        assertEquals(0, orderService.getOrdersByTruck(5L).size());
        verify(orderRepo).search(null, 5L, null);
    }

    @Test
    void markInRouteDoesNotMoveStock() {
        Order order = orderWithStatus(8L, OrderStatus.PENDING);
        when(orderRepo.findById(8L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        orderService.markInRoute(8L);

        assertEquals(OrderStatus.IN_ROUTE, order.getStatus());
        verifyNoInteractions(deliveryStockService);
    }

    @Test
    void cancelPendingOrderRevertsItsStockLoad() {
        Order order = orderWithStatus(7L, OrderStatus.PENDING);
        when(orderRepo.findById(7L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        orderService.cancel(7L);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(deliveryStockService).revertLoadForOrder(7L);
    }

    @Test
    void cancelInRouteOrderRevertsItsStockLoad() {
        Order order = orderWithStatus(9L, OrderStatus.IN_ROUTE);
        when(orderRepo.findById(9L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        orderService.cancel(9L);

        verify(deliveryStockService).revertLoadForOrder(9L);
    }

    @Test
    void cancelDeliveredOrderIsRejectedAndKeepsItsStockLoad() {
        Order order = orderWithStatus(6L, OrderStatus.DELIVERED);
        when(orderRepo.findById(6L)).thenReturn(Optional.of(order));

        assertThrows(BusinessRuleException.class, () -> orderService.cancel(6L));

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verifyNoInteractions(deliveryStockService);
    }

    @Test
    void cancelingAnAlreadyCancelledOrderDoesNotRevertAgain() {
        Order order = orderWithStatus(4L, OrderStatus.CANCELLED);
        when(orderRepo.findById(4L)).thenReturn(Optional.of(order));

        orderService.cancel(4L);

        verify(orderRepo, never()).save(any(Order.class));
        verifyNoInteractions(deliveryStockService);
    }

    @Test
    void updatePendingOrderSyncsStockLoadWithNewAssignment() {
        Customer customer = new Customer();
        customer.setId(1L);
        Address address = new Address();
        address.setId(2L);
        address.setCustomer(customer);
        address.setStreetAddress("Av Siempre Viva 123");
        Product product = new Product();
        product.setId(3L);
        product.setActive(true);
        product.setUnitPrice(new BigDecimal("10.00"));
        Order order = orderWithStatus(11L, OrderStatus.PENDING);
        order.setTruckId(1L);

        when(orderRepo.findById(11L)).thenReturn(Optional.of(order));
        when(customerService.getCustomerEntity(1L)).thenReturn(customer);
        when(addressService.getAddressEntity(1L, 2L)).thenReturn(address);
        when(productService.getProductEntity(3L)).thenReturn(product);
        when(orderRepo.save(order)).thenReturn(order);

        // se reasigna a otro camion (unassign seria truckId null)
        orderService.update(11L, new OrderRequest(1L, 2L, 5L, LocalDate.of(2026, 10, 13), null,
                List.of(new OrderItemRequest(3L, 4))));

        assertEquals(5L, order.getTruckId());
        verify(deliveryStockService).syncLoadForOrder(order);
    }

    private Order orderWithStatus(Long id, OrderStatus status) {
        Customer customer = new Customer();
        customer.setId(1L);
        Order order = new Order();
        order.setId(id);
        order.setCustomer(customer);
        order.setStatus(status);
        order.setDeliveryDate(LocalDate.of(2026, 10, 13));
        return order;
    }

    private Order orderWithTotal(Long id, BigDecimal total) {
        Order order = orderWithStatus(id, OrderStatus.IN_ROUTE);
        order.setTotalAmount(total);
        return order;
    }
}
