package com.example.distribuidora.services;

import com.example.distribuidora.dtos.LedgerEntryRequest;
import com.example.distribuidora.dtos.OrderDeliveryRequest;
import com.example.distribuidora.dtos.OrderReturnRequest;
import com.example.distribuidora.dtos.RoleResponse;
import com.example.distribuidora.enums.LedgerEntryType;
import com.example.distribuidora.enums.OrderPaymentMethod;
import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.enums.ProductUnit;
import com.example.distribuidora.enums.StockMovementType;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.models.*;
import com.example.distribuidora.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderReturnAndHistoryTest {

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
    @Mock
    private ITruckStockMovementRepository movementRepo;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ILedgerEntryRepository ledgerRepo;
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
    private DeliveryStockService deliveryStockServiceMock;

    @Test
    void registerReturnRejectsOrderWithoutAssignedTruck() {
        DeliveryStockService service = new DeliveryStockService(
                stockRepo, truckRepo, productRepo, orderRepo, loadRepo, movementRepo, currentUserService);

        Order order = new Order();
        order.setId(10L);
        order.setTruckId(null);
        order.setStatus(OrderStatus.IN_ROUTE);
        when(orderRepo.findById(10L)).thenReturn(Optional.of(order));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.registerReturnForOrder(10L, new OrderReturnRequest(5L, 2, "Sin cliente")));

        assertTrue(ex.getMessage().contains("no tiene un camión asignado"));
        verifyNoInteractions(stockRepo);
    }

    @Test
    void registerReturnRejectsQuantityAbovePending() {
        DeliveryStockService service = new DeliveryStockService(
                stockRepo, truckRepo, productRepo, orderRepo, loadRepo, movementRepo, currentUserService);

        Product product = new Product();
        product.setId(5L);
        product.setName("Café Molido");
        product.setUnit(ProductUnit.KILOGRAM);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(4);

        Order order = new Order();
        order.setId(11L);
        order.setTruckId(3L);
        order.setStatus(OrderStatus.IN_ROUTE);
        order.setDeliveryDate(LocalDate.of(2026, 11, 19));
        order.setItems(List.of(item));

        when(orderRepo.findById(11L)).thenReturn(Optional.of(order));
        when(movementRepo.sumQuantityByOrderAndProductAndType(11L, 5L, StockMovementType.RETURN)).thenReturn(2);

        assertThrows(BusinessRuleException.class,
                () -> service.registerReturnForOrder(11L, new OrderReturnRequest(5L, 3, null)));
    }

    @Test
    void markDeliveredSupportsPartialMixedPayment() {
        OrderService orderService = new OrderService(
                orderRepo, customerService, addressService, productService, userRepo,
                ledgerService, deliveryStockServiceMock, truckRepo, ledgerRepo, currentUserService);

        Customer customer = new Customer();
        customer.setId(1L);
        Order order = new Order();
        order.setId(30L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.IN_ROUTE);
        order.setTotalAmount(new BigDecimal("100.00"));

        when(orderRepo.findById(30L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        OrderDeliveryRequest req = new OrderDeliveryRequest(
                OrderPaymentMethod.MIXED,
                new BigDecimal("25.00"),
                new BigDecimal("35.00"),
                new BigDecimal("60.00"),
                "deliv-30-1"
        );
        var response = orderService.markDelivered(30L, req);

        assertEquals(OrderStatus.DELIVERED, response.status());
        assertEquals(new BigDecimal("60.00"), response.amountPaid());
        assertEquals(new BigDecimal("40.00"), response.pendingBalance());
        verify(ledgerService).registerCharge(customer, order, new BigDecimal("100.00"));
        verify(ledgerService).registerOrderPayment(order, new BigDecimal("60.00"));
    }

    @Test
    void ledgerPaymentValidatesMixedSumAndSupportsIdempotency() {
        LedgerService realLedgerService = new LedgerService(ledgerRepo, customerService, orderRepo, currentUserService);
        Customer customer = new Customer();
        customer.setId(2L);
        when(customerService.getCustomerEntity(2L)).thenReturn(customer);

        LedgerEntryRequest invalidMixed = new LedgerEntryRequest(
                new BigDecimal("50.00"), OrderPaymentMethod.MIXED,
                new BigDecimal("20.00"), new BigDecimal("25.00"),
                null, "Pago mixto inválido", null);

        assertThrows(BusinessRuleException.class, () -> realLedgerService.registerPayment(2L, invalidMixed));

        LedgerEntry existing = new LedgerEntry();
        existing.setId(99L);
        existing.setCustomer(customer);
        existing.setEntryType(LedgerEntryType.PAYMENT);
        existing.setPaymentMethod(OrderPaymentMethod.CASH);
        existing.setAmount(new BigDecimal("50.00"));
        existing.setRunningBalance(new BigDecimal("10.00"));
        when(ledgerRepo.findByIdempotencyKey("key-99")).thenReturn(Optional.of(existing));

        var idempotent = realLedgerService.registerPayment(2L, new LedgerEntryRequest(
                new BigDecimal("50.00"), OrderPaymentMethod.CASH, null, null, null, null, "key-99"));

        assertEquals(99L, idempotent.id());
        verify(ledgerRepo, never()).save(any());
    }

    @Test
    void receiptPdfGeneratesValidPdfForDeliveredOrder() {
        OrderService mockOrderService = mock(OrderService.class);
        OrderReceiptPdfService pdfService = new OrderReceiptPdfService(mockOrderService, ledgerRepo);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setBusinessName("Kiosco Centro");
        customer.setTaxId("20-30123456-7");

        Product product = new Product();
        product.setId(1L);
        product.setName("Agua Mineral 2L");

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(5);
        item.setUnitPrice(new BigDecimal("1200.00"));
        item.setSubtotal(new BigDecimal("6000.00"));

        Order order = new Order();
        order.setId(50L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveryDate(LocalDate.of(2026, 11, 19));
        order.setDeliveredAt(LocalDateTime.of(2026, 11, 19, 15, 30));
        order.setPaymentMethod(OrderPaymentMethod.MIXED);
        order.setCashAmount(new BigDecimal("2000.00"));
        order.setTransferAmount(new BigDecimal("2500.00"));
        order.setTotalAmount(new BigDecimal("6000.00"));
        order.setItems(List.of(item));

        when(mockOrderService.getOrderEntity(50L)).thenReturn(order);
        when(ledgerRepo.sumPaymentsByOrderId(50L)).thenReturn(new BigDecimal("4500.00"));

        byte[] pdf = pdfService.generateReceipt(50L);
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    void roleResponseIncludesDescriptionAndDetailedPermissions() {
        Permission perm = new Permission();
        perm.setId(1L);
        perm.setCode("products:write");
        perm.setName("Administrar productos, categorías y precios");
        perm.setDescription("Crear y editar productos");

        Role role = new Role();
        role.setId(5L);
        role.setName("DEPOSITO");
        role.setDescription("Gestión de catálogo y stock por camión");
        role.setPermissions(Set.of(perm));

        RoleResponse response = new RoleResponse(role);
        assertEquals("DEPOSITO", response.name());
        assertEquals("Gestión de catálogo y stock por camión", response.description());
        assertEquals(1, response.permissions().size());
        assertEquals("products:write", response.permissions().get(0).code());
        assertEquals("Administrar productos, categorías y precios", response.permissions().get(0).name());
    }
}
