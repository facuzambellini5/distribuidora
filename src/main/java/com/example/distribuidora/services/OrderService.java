package com.example.distribuidora.services;

import com.example.distribuidora.dtos.OrderItemRequest;
import com.example.distribuidora.dtos.OrderRequest;
import com.example.distribuidora.dtos.OrderResponse;
import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.*;
import com.example.distribuidora.repositories.IOrderRepository;
import com.example.distribuidora.repositories.IUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final IOrderRepository orderRepo;
    private final CustomerService customerService;
    private final AddressService addressService;
    private final ProductService productService;
    private final IUserRepository userRepo;
    private final LedgerService ledgerService;
    private final DeliveryStockService deliveryStockService;

    public OrderService(IOrderRepository orderRepo,
                        CustomerService customerService,
                        AddressService addressService,
                        ProductService productService,
                        IUserRepository userRepo,
                        LedgerService ledgerService,
                        DeliveryStockService deliveryStockService) {
        this.orderRepo = orderRepo;
        this.customerService = customerService;
        this.addressService = addressService;
        this.productService = productService;
        this.userRepo = userRepo;
        this.ledgerService = ledgerService;
        this.deliveryStockService = deliveryStockService;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Customer customer = customerService.getCustomerEntity(request.customerId());
        Address address = addressService.getAddressEntity(customer.getId(), request.deliveryAddressId());

        Order order = new Order();
        order.setCustomer(customer);
        order.setDeliveryAddress(address);
        order.setTruckId(request.truckId());
        order.setDeliveryDate(resolveDeliveryDate(request.deliveryDate()));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedBy(resolveUser(request.createdById()));
        applyItems(order, request.items());

        Order saved = orderRepo.save(order);
        // El camion arranca en cero: al asignarlo, el pedido carga sus productos en el camion.
        deliveryStockService.registerLoadForOrder(saved);
        return new OrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(Long customerId, Long truckId, OrderStatus status) {
        return orderRepo.search(customerId, truckId, status).stream().map(OrderResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByTruck(Long truckId) {
        return getOrders(null, truckId, null);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return new OrderResponse(getOrderEntity(id));
    }

    @Transactional(readOnly = true)
    public Order getOrderEntity(Long id) {
        return orderRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Order", id));
    }

    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        Order order = getOrderEntity(id);
        requireStatus(order, OrderStatus.PENDING, "Only pending orders can be updated.");
        Customer customer = customerService.getCustomerEntity(request.customerId());
        Address address = addressService.getAddressEntity(customer.getId(), request.deliveryAddressId());

        order.setCustomer(customer);
        order.setDeliveryAddress(address);
        order.setTruckId(request.truckId());
        order.setDeliveryDate(resolveDeliveryDate(request.deliveryDate()));
        order.setCreatedBy(resolveUser(request.createdById()));
        order.getItems().clear();
        applyItems(order, request.items());

        Order saved = orderRepo.save(order);
        // Revierte la carga anterior (otro camion, fecha o items) y aplica la nueva; truckId null = desasignado.
        deliveryStockService.syncLoadForOrder(saved);
        return new OrderResponse(saved);
    }

    @Transactional
    public OrderResponse markInRoute(Long id) {
        Order order = getOrderEntity(id);
        requireStatus(order, OrderStatus.PENDING, "Only pending orders can be marked in route.");
        order.setStatus(OrderStatus.IN_ROUTE);
        return new OrderResponse(orderRepo.save(order));
    }

    @Transactional
    public OrderResponse markDelivered(Long id) {
        Order order = getOrderEntity(id);
        if (order.getStatus() == OrderStatus.DELIVERED) {
            return new OrderResponse(order);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled orders cannot be delivered.");
        }
        // La carga ya se registro al asignar el pedido; al entregar esas unidades quedan como entregadas (no se revierten).
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        Order saved = orderRepo.save(order);
        ledgerService.registerCharge(saved.getCustomer(), saved, saved.getTotalAmount());
        return new OrderResponse(saved);
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = getOrderEntity(id);
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Delivered orders cannot be cancelled.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return new OrderResponse(order); // ya cancelado: la reversion no se repite
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepo.save(order);
        deliveryStockService.revertLoadForOrder(saved.getId());
        return new OrderResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        cancel(id);
    }

    private void applyItems(Order order, List<OrderItemRequest> itemRequests) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productService.getProductEntity(itemRequest.productId());
            if (!product.isActive()) {
                throw new BusinessRuleException("Product is inactive: " + product.getId());
            }
            BigDecimal subtotal = product.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(product.getUnitPrice());
            item.setSubtotal(subtotal);
            order.getItems().add(item);
            total = total.add(subtotal);
        }
        order.setTotalAmount(total);
    }


    private User resolveUser(Long userId) {
        if (userId == null) return null;
        return userRepo.findById(userId).orElseThrow(() -> new EntityNotFoundException("User", userId));
    }

    private LocalDate resolveDeliveryDate(LocalDate deliveryDate) {
        return deliveryDate != null ? deliveryDate : LocalDate.now();
    }

    private void requireStatus(Order order, OrderStatus status, String message) {
        if (order.getStatus() != status) {
            throw new BusinessRuleException(message);
        }
    }
}
