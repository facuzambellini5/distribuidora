package com.example.distribuidora.services;

import com.example.distribuidora.dtos.DeliveryHistoryItemResponse;
import com.example.distribuidora.dtos.OrderDeliveryRequest;
import com.example.distribuidora.dtos.OrderItemRequest;
import com.example.distribuidora.dtos.OrderItemResponse;
import com.example.distribuidora.dtos.OrderRequest;
import com.example.distribuidora.dtos.OrderResponse;
import com.example.distribuidora.dtos.SalesHistoryItemResponse;
import com.example.distribuidora.enums.OrderPaymentMethod;
import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.enums.SalesDateType;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Address;
import com.example.distribuidora.models.Customer;
import com.example.distribuidora.models.Order;
import com.example.distribuidora.models.OrderItem;
import com.example.distribuidora.models.Product;
import com.example.distribuidora.models.Truck;
import com.example.distribuidora.models.User;
import com.example.distribuidora.repositories.ILedgerEntryRepository;
import com.example.distribuidora.repositories.IOrderRepository;
import com.example.distribuidora.repositories.ITruckRepository;
import com.example.distribuidora.repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final String DRIVER_ROLE = "REPARTIDOR";

    private final IOrderRepository orderRepo;
    private final CustomerService customerService;
    private final AddressService addressService;
    private final ProductService productService;
    private final IUserRepository userRepo;
    private final LedgerService ledgerService;
    private final DeliveryStockService deliveryStockService;
    private final ITruckRepository truckRepo;
    private final ILedgerEntryRepository ledgerRepo;
    private final CurrentUserService currentUserService;

    @Autowired
    public OrderService(IOrderRepository orderRepo,
                        CustomerService customerService,
                        AddressService addressService,
                        ProductService productService,
                        IUserRepository userRepo,
                        LedgerService ledgerService,
                        DeliveryStockService deliveryStockService,
                        ITruckRepository truckRepo,
                        ILedgerEntryRepository ledgerRepo,
                        CurrentUserService currentUserService) {
        this.orderRepo = orderRepo;
        this.customerService = customerService;
        this.addressService = addressService;
        this.productService = productService;
        this.userRepo = userRepo;
        this.ledgerService = ledgerService;
        this.deliveryStockService = deliveryStockService;
        this.truckRepo = truckRepo;
        this.ledgerRepo = ledgerRepo;
        this.currentUserService = currentUserService;
    }

    public OrderService(IOrderRepository orderRepo,
                        CustomerService customerService,
                        AddressService addressService,
                        ProductService productService,
                        IUserRepository userRepo,
                        LedgerService ledgerService,
                        DeliveryStockService deliveryStockService) {
        this(orderRepo, customerService, addressService, productService, userRepo,
                ledgerService, deliveryStockService, null, null, null);
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
        User createdBy = resolveUser(request.createdById());
        if (createdBy == null && currentUserService != null) {
            createdBy = currentUserService.getCurrentUserOrNull();
        }
        order.setCreatedBy(createdBy);
        applyItems(order, request.items());

        Order saved = orderRepo.save(order);
        deliveryStockService.registerLoadForOrder(saved);
        return new OrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(Long customerId, Long truckId, OrderStatus status) {
        Long effectiveTruckId = enforceDriverTruckScope(truckId);
        if (isDriverWithoutTruck()) {
            return List.of();
        }
        return orderRepo.search(customerId, effectiveTruckId, status).stream().map(OrderResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByTruck(Long truckId) {
        return getOrders(null, truckId, null);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        Order order = getOrderEntity(id);
        enforceDriverOrderAccess(order);
        BigDecimal paid = computeTotalPaidForOrder(order);
        return new OrderResponse(order, paid);
    }

    @Transactional(readOnly = true)
    public Order getOrderEntity(Long id) {
        return orderRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Order", id));
    }

    // Historial detallado de ventas con filtros por rango de fechas, tipo de fecha (CREATED/DELIVERED/PAID), cliente y usuario responsable.
    @Transactional(readOnly = true)
    public List<SalesHistoryItemResponse> getSalesHistory(LocalDate fromDate,
                                                          LocalDate toDate,
                                                          SalesDateType dateType,
                                                          Long customerId,
                                                          Long userId) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("La fecha desde debe ser anterior o igual a la fecha hasta.");
        }
        SalesDateType effectiveType = dateType != null ? dateType : SalesDateType.CREATED;
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;

        List<Order> orders = switch (effectiveType) {
            case CREATED -> orderRepo.searchSalesByCreatedDate(customerId, userId, fromDateTime, toDateTime);
            case DELIVERED -> orderRepo.searchSalesByDeliveredDate(customerId, userId, fromDateTime, toDateTime);
            case PAID -> orderRepo.searchSalesByPaidDate(customerId, userId, fromDateTime, toDateTime);
        };

        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> paidByOrder = new HashMap<>();
        Map<Long, LocalDateTime> lastPaymentByOrder = new HashMap<>();
        if (ledgerRepo != null) {
            List<Long> orderIds = orders.stream().map(Order::getId).toList();
            for (Object[] row : ledgerRepo.summarizePaymentsByOrderIds(orderIds)) {
                Long orderId = (Long) row[0];
                BigDecimal sum = (BigDecimal) row[1];
                LocalDateTime lastAt = (LocalDateTime) row[2];
                paidByOrder.put(orderId, sum != null ? sum : BigDecimal.ZERO);
                lastPaymentByOrder.put(orderId, lastAt);
            }
        }

        return orders.stream().map(order -> {
            BigDecimal amountPaid = paidByOrder.getOrDefault(order.getId(), computeOrderDeliveryPayment(order));
            BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal pending = total.subtract(amountPaid);
            if (pending.compareTo(BigDecimal.ZERO) < 0) {
                pending = BigDecimal.ZERO;
            }
            User responsible = order.getCreatedBy() != null ? order.getCreatedBy() : order.getDeliveredBy();
            return new SalesHistoryItemResponse(
                    order.getId(),
                    order.getOrderCode(),
                    order.getCreatedAt(),
                    order.getDeliveryDate(),
                    order.getDeliveredAt(),
                    lastPaymentByOrder.get(order.getId()),
                    order.getCustomer() == null ? null : order.getCustomer().getId(),
                    order.getCustomer() == null ? null : order.getCustomer().getBusinessName(),
                    responsible == null ? null : responsible.getId(),
                    responsible == null ? null : responsible.getFullName(),
                    order.getDeliveredBy() == null ? null : order.getDeliveredBy().getId(),
                    order.getDeliveredBy() == null ? null : order.getDeliveredBy().getFullName(),
                    order.getStatus(),
                    order.getPaymentMethod(),
                    order.getCashAmount(),
                    order.getTransferAmount(),
                    total,
                    amountPaid,
                    pending,
                    order.getItems() == null ? List.of()
                            : order.getItems().stream().map(OrderItemResponse::new).toList()
            );
        }).toList();
    }

    // Historial de repartos con filtros por fecha de reparto, cliente, camion, chofer y estado.
    // Aplica permisos por rol: un REPARTIDOR solo visualiza repartos de su camion asignado.
    @Transactional(readOnly = true)
    public List<DeliveryHistoryItemResponse> getDeliveryHistory(LocalDate fromDate,
                                                                LocalDate toDate,
                                                                Long customerId,
                                                                Long truckId,
                                                                Long driverId,
                                                                OrderStatus status) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("La fecha desde debe ser anterior o igual a la fecha hasta.");
        }

        Long effectiveTruckId = enforceDriverTruckScope(truckId);
        Long effectiveDriverId = enforceDriverUserScope(driverId);
        if (isDriverWithoutTruck()) {
            return List.of();
        }

        List<Order> orders = orderRepo.searchDeliveries(
                fromDate, toDate, customerId, effectiveTruckId, effectiveDriverId, status);
        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Long, Truck> trucksById = new HashMap<>();
        if (truckRepo != null) {
            Set<Long> truckIds = orders.stream()
                    .map(Order::getTruckId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!truckIds.isEmpty()) {
                for (Truck truck : truckRepo.findAllById(truckIds)) {
                    trucksById.put(truck.getId(), truck);
                }
            }
        }

        return orders.stream().map(order -> {
            Truck truck = order.getTruckId() == null ? null : trucksById.get(order.getTruckId());
            User driver = truck == null ? null : truck.getDriver();
            return new DeliveryHistoryItemResponse(
                    order.getId(),
                    order.getOrderCode(),
                    order.getCustomer() == null ? null : order.getCustomer().getId(),
                    order.getCustomer() == null ? null : order.getCustomer().getBusinessName(),
                    order.getDeliveryAddress() == null ? null : buildAddressSnapshot(order.getDeliveryAddress()),
                    order.getDeliveryDate(),
                    order.getDeliveredAt(),
                    order.getTruckId(),
                    truck == null ? null : truck.getLicensePlate(),
                    driver == null ? null : driver.getId(),
                    driver == null ? null : driver.getFullName(),
                    order.getStatus(),
                    order.getItems() == null ? List.of()
                            : order.getItems().stream().map(OrderItemResponse::new).toList()
            );
        }).toList();
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
        deliveryStockService.syncLoadForOrder(saved);
        return new OrderResponse(saved);
    }

    @Transactional
    public OrderResponse markInRoute(Long id) {
        Order order = getOrderEntity(id);
        enforceDriverOrderAccess(order);
        requireStatus(order, OrderStatus.PENDING, "Only pending orders can be marked in route.");
        order.setStatus(OrderStatus.IN_ROUTE);
        return new OrderResponse(orderRepo.save(order));
    }

    @Transactional
    public OrderResponse markDelivered(Long id, OrderDeliveryRequest request) {
        if (request != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            var existingByKey = orderRepo.findByDeliveryIdempotencyKey(request.idempotencyKey().trim());
            if (existingByKey.isPresent()) {
                Order existing = existingByKey.get();
                if (!existing.getId().equals(id)) {
                    throw new BusinessRuleException("La clave de idempotencia ya fue utilizada para otro pedido.");
                }
                return new OrderResponse(existing, computeTotalPaidForOrder(existing));
            }
        }

        Order order = getOrderEntity(id);
        enforceDriverOrderAccess(order);

        if (order.getStatus() == OrderStatus.DELIVERED) {
            return new OrderResponse(order, computeTotalPaidForOrder(order));
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled orders cannot be delivered.");
        }

        applyPayment(order, request);

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        if (currentUserService != null) {
            order.setDeliveredBy(currentUserService.getCurrentUserOrNull());
        }
        if (request != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            order.setDeliveryIdempotencyKey(request.idempotencyKey().trim());
        }

        Order saved = orderRepo.save(order);
        deliveryStockService.registerDeliveryForOrder(saved);
        ledgerService.registerCharge(saved.getCustomer(), saved, saved.getTotalAmount());
        BigDecimal paidNow = saved.getCashAmount().add(saved.getTransferAmount());
        ledgerService.registerOrderPayment(saved, paidNow);
        return new OrderResponse(saved, paidNow);
    }

    /** Preserves the service-level behavior for callers that deliver without collecting payment. */
    @Transactional
    public OrderResponse markDelivered(Long id) {
        return markDelivered(id, new OrderDeliveryRequest(OrderPaymentMethod.CURRENT_ACCOUNT, null, null));
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = getOrderEntity(id);
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Delivered orders cannot be cancelled.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return new OrderResponse(order);
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

    private void applyPayment(Order order, OrderDeliveryRequest request) {
        if (request == null || request.paymentMethod() == null) {
            throw new BusinessRuleException("A payment method is required when delivering an order.");
        }

        BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        BigDecimal cash = request.cashAmount() == null ? BigDecimal.ZERO : request.cashAmount();
        BigDecimal transfer = request.transferAmount() == null ? BigDecimal.ZERO : request.transferAmount();
        BigDecimal explicitAmountPaid = request.amountPaid();

        if (cash.compareTo(BigDecimal.ZERO) < 0 || transfer.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Payment amounts cannot be negative.");
        }
        if (explicitAmountPaid != null) {
            if (explicitAmountPaid.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("Paid amount cannot be negative.");
            }
            if (explicitAmountPaid.compareTo(total) > 0) {
                throw new BusinessRuleException("Paid amount cannot exceed the order total.");
            }
        }

        switch (request.paymentMethod()) {
            case CASH -> {
                if (transfer.compareTo(BigDecimal.ZERO) != 0) {
                    throw new BusinessRuleException("Transfer amount is not allowed for CASH payments.");
                }
                if (explicitAmountPaid != null) {
                    if (explicitAmountPaid.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BusinessRuleException("CASH payment amount must be greater than zero.");
                    }
                    if (cash.compareTo(BigDecimal.ZERO) > 0 && cash.compareTo(explicitAmountPaid) != 0) {
                        throw new BusinessRuleException("Cash amount must equal the specified paid amount.");
                    }
                    cash = explicitAmountPaid;
                } else if (cash.compareTo(BigDecimal.ZERO) > 0) {
                    if (cash.compareTo(total) > 0) {
                        throw new BusinessRuleException("Cash amount cannot exceed the order total.");
                    }
                } else {
                    cash = total;
                }
            }
            case TRANSFER -> {
                if (cash.compareTo(BigDecimal.ZERO) != 0) {
                    throw new BusinessRuleException("Cash amount is not allowed for TRANSFER payments.");
                }
                if (explicitAmountPaid != null) {
                    if (explicitAmountPaid.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BusinessRuleException("TRANSFER payment amount must be greater than zero.");
                    }
                    if (transfer.compareTo(BigDecimal.ZERO) > 0 && transfer.compareTo(explicitAmountPaid) != 0) {
                        throw new BusinessRuleException("Transfer amount must equal the specified paid amount.");
                    }
                    transfer = explicitAmountPaid;
                } else if (transfer.compareTo(BigDecimal.ZERO) > 0) {
                    if (transfer.compareTo(total) > 0) {
                        throw new BusinessRuleException("Transfer amount cannot exceed the order total.");
                    }
                } else {
                    transfer = total;
                }
            }
            case MIXED -> {
                if (cash.compareTo(BigDecimal.ZERO) <= 0 || transfer.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException("Mixed payment requires positive cash and transfer amounts.");
                }
                BigDecimal sum = cash.add(transfer);
                BigDecimal expected = explicitAmountPaid != null ? explicitAmountPaid : total;
                if (sum.compareTo(expected) != 0) {
                    throw new BusinessRuleException(explicitAmountPaid != null
                            ? "Cash and transfer amounts must equal the specified paid amount."
                            : "Cash and transfer amounts must equal the order total.");
                }
                if (sum.compareTo(total) > 0) {
                    throw new BusinessRuleException("Cash and transfer amounts cannot exceed the order total.");
                }
            }
            case CURRENT_ACCOUNT -> {
                if (cash.compareTo(BigDecimal.ZERO) != 0
                        || transfer.compareTo(BigDecimal.ZERO) != 0
                        || (explicitAmountPaid != null && explicitAmountPaid.compareTo(BigDecimal.ZERO) != 0)) {
                    throw new BusinessRuleException("Current account delivery cannot include cash or transfer amounts.");
                }
            }
        }

        order.setPaymentMethod(request.paymentMethod());
        order.setCashAmount(cash);
        order.setTransferAmount(transfer);
    }

    private BigDecimal computeTotalPaidForOrder(Order order) {
        if (ledgerRepo != null && order.getId() != null) {
            BigDecimal sum = ledgerRepo.sumPaymentsByOrderId(order.getId());
            if (sum != null && sum.compareTo(BigDecimal.ZERO) > 0) {
                return sum;
            }
        }
        return computeOrderDeliveryPayment(order);
    }

    private BigDecimal computeOrderDeliveryPayment(Order order) {
        BigDecimal cash = order.getCashAmount() == null ? BigDecimal.ZERO : order.getCashAmount();
        BigDecimal transfer = order.getTransferAmount() == null ? BigDecimal.ZERO : order.getTransferAmount();
        return cash.add(transfer);
    }

    private Long enforceDriverTruckScope(Long requestedTruckId) {
        User currentUser = currentUserService != null ? currentUserService.getCurrentUserOrNull() : null;
        if (currentUser == null || currentUser.getRole() == null
                || !DRIVER_ROLE.equals(currentUser.getRole().getName())) {
            return requestedTruckId;
        }
        Truck assignedTruck = truckRepo != null
                ? truckRepo.findFirstByDriverIdAndIsActiveTrue(currentUser.getId()).orElse(null)
                : null;
        if (assignedTruck == null) {
            return null;
        }
        if (requestedTruckId != null && !requestedTruckId.equals(assignedTruck.getId())) {
            throw new AccessDeniedException("Solo podés consultar los repartos de tu camión asignado.");
        }
        return assignedTruck.getId();
    }

    private Long enforceDriverUserScope(Long requestedDriverId) {
        User currentUser = currentUserService != null ? currentUserService.getCurrentUserOrNull() : null;
        if (currentUser == null || currentUser.getRole() == null
                || !DRIVER_ROLE.equals(currentUser.getRole().getName())) {
            return requestedDriverId;
        }
        if (requestedDriverId != null && !requestedDriverId.equals(currentUser.getId())) {
            throw new AccessDeniedException("Solo podés consultar tus propios repartos.");
        }
        return currentUser.getId();
    }

    private boolean isDriverWithoutTruck() {
        User currentUser = currentUserService != null ? currentUserService.getCurrentUserOrNull() : null;
        if (currentUser == null || currentUser.getRole() == null
                || !DRIVER_ROLE.equals(currentUser.getRole().getName())) {
            return false;
        }
        return truckRepo != null
                && truckRepo.findFirstByDriverIdAndIsActiveTrue(currentUser.getId()).isEmpty();
    }

    private void enforceDriverOrderAccess(Order order) {
        User currentUser = currentUserService != null ? currentUserService.getCurrentUserOrNull() : null;
        if (currentUser == null || currentUser.getRole() == null
                || !DRIVER_ROLE.equals(currentUser.getRole().getName())
                || truckRepo == null) {
            return;
        }
        Truck assignedTruck = truckRepo.findFirstByDriverIdAndIsActiveTrue(currentUser.getId()).orElse(null);
        if (assignedTruck == null || order.getTruckId() == null
                || !assignedTruck.getId().equals(order.getTruckId())) {
            throw new AccessDeniedException("No tenés permiso para acceder a pedidos de otro camión.");
        }
    }

    private String buildAddressSnapshot(Address address) {
        String label = address.getLabel() == null || address.getLabel().isBlank() ? "" : address.getLabel() + " - ";
        String street = address.getStreetAddress() == null ? "" : address.getStreetAddress();
        String city = address.getCity() == null || address.getCity().isBlank() ? "" : ", " + address.getCity();
        return label + street + city;
    }
}
