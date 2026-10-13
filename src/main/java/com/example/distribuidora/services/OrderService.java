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

    public OrderService(IOrderRepository orderRepo,
                        CustomerService customerService,
                        AddressService addressService,
                        ProductService productService,
                        IUserRepository userRepo,
                        LedgerService ledgerService) {
        this.orderRepo = orderRepo;
        this.customerService = customerService;
        this.addressService = addressService;
        this.productService = productService;
        this.userRepo = userRepo;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Customer customer = customerService.getCustomerEntity(request.customerId());
        Address address = addressService.getAddressEntity(customer.getId(), request.deliveryAddressId());

        Order order = new Order();
        order.setCustomer(customer);
        order.setTruckId(request.truckId());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedBy(resolveUser(request.createdById()));
        applyAddressSnapshot(order, address);
        applyItems(order, request.items());

        return new OrderResponse(orderRepo.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(Long customerId, OrderStatus status) {
        List<Order> orders;
        if (customerId != null) {
            orders = orderRepo.findByCustomerId(customerId);
        } else if (status != null) {
            orders = orderRepo.findByStatus(status);
        } else {
            orders = orderRepo.findAll();
        }
        return orders.stream().map(OrderResponse::new).toList();
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
        order.setTruckId(request.truckId());
        order.setCreatedBy(resolveUser(request.createdById()));
        applyAddressSnapshot(order, address);
        order.getItems().clear();
        applyItems(order, request.items());

        return new OrderResponse(orderRepo.save(order));
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
        order.setStatus(OrderStatus.CANCELLED);
        return new OrderResponse(orderRepo.save(order));
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

    private void applyAddressSnapshot(Order order, Address address) {
        String snapshot = "%s%s%s".formatted(
                address.getLabel() == null || address.getLabel().isBlank() ? "" : address.getLabel() + " - ",
                address.getStreetAddress(),
                address.getCity() == null || address.getCity().isBlank() ? "" : ", " + address.getCity()
        );
        order.setDeliveryAddressSnapshot(snapshot);
        order.setDeliveryLatitude(address.getLatitude());
        order.setDeliveryLongitude(address.getLongitude());
    }

    private User resolveUser(Long userId) {
        if (userId == null) return null;
        return userRepo.findById(userId).orElseThrow(() -> new EntityNotFoundException("User", userId));
    }

    private void requireStatus(Order order, OrderStatus status, String message) {
        if (order.getStatus() != status) {
            throw new BusinessRuleException(message);
        }
    }
}
