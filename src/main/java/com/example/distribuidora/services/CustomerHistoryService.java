package com.example.distribuidora.services;

import com.example.distribuidora.dtos.CustomerHistoryItemResponse;
import com.example.distribuidora.dtos.CustomerHistoryResponse;
import com.example.distribuidora.enums.CustomerHistoryEventType;
import com.example.distribuidora.enums.LedgerEntryType;
import com.example.distribuidora.models.Customer;
import com.example.distribuidora.models.LedgerEntry;
import com.example.distribuidora.models.Order;
import com.example.distribuidora.models.TruckStockMovement;
import com.example.distribuidora.repositories.ILedgerEntryRepository;
import com.example.distribuidora.repositories.IOrderRepository;
import com.example.distribuidora.repositories.ITruckStockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CustomerHistoryService {

    private final CustomerService customerService;
    private final IOrderRepository orderRepo;
    private final ILedgerEntryRepository ledgerRepo;
    private final ITruckStockMovementRepository movementRepo;

    public CustomerHistoryService(CustomerService customerService,
                                  IOrderRepository orderRepo,
                                  ILedgerEntryRepository ledgerRepo,
                                  ITruckStockMovementRepository movementRepo) {
        this.customerService = customerService;
        this.orderRepo = orderRepo;
        this.ledgerRepo = ledgerRepo;
        this.movementRepo = movementRepo;
    }

    @Transactional(readOnly = true)
    public CustomerHistoryResponse getCustomerHistory(Long customerId) {
        Customer customer = customerService.getCustomerEntity(customerId);
        List<CustomerHistoryItemResponse> entries = new ArrayList<>();

        // 1) Pedidos y entregas del cliente
        List<Order> orders = orderRepo.findByCustomerIdOrderByCreatedAtDescIdDesc(customerId);
        for (Order order : orders) {
            int totalUnits = order.getItems().stream()
                    .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                    .sum();
            entries.add(new CustomerHistoryItemResponse(
                    order.getCreatedAt() != null ? order.getCreatedAt() : order.getDeliveryDate().atStartOfDay(),
                    CustomerHistoryEventType.ORDER,
                    "Compra / Pedido " + order.getOrderCode(),
                    order.getTotalAmount(),
                    null,
                    totalUnits,
                    null,
                    null,
                    order.getStatus().name(),
                    order.getId(),
                    order.getOrderCode(),
                    order.getPaymentMethod(),
                    order.getTruckId(),
                    null,
                    order.getCreatedBy() == null ? null : order.getCreatedBy().getId(),
                    order.getCreatedBy() == null ? null : order.getCreatedBy().getFullName()
            ));

            if (order.getDeliveredAt() != null) {
                entries.add(new CustomerHistoryItemResponse(
                        order.getDeliveredAt(),
                        CustomerHistoryEventType.DELIVERY,
                        "Entrega registrada del pedido " + order.getOrderCode(),
                        order.getTotalAmount(),
                        null,
                        totalUnits,
                        null,
                        null,
                        order.getStatus().name(),
                        order.getId(),
                        order.getOrderCode(),
                        order.getPaymentMethod(),
                        order.getTruckId(),
                        null,
                        order.getDeliveredBy() == null ? null : order.getDeliveredBy().getId(),
                        order.getDeliveredBy() == null ? null : order.getDeliveredBy().getFullName()
                ));
            }
        }

        // 2) Pagos y ajustes en cuenta corriente
        List<LedgerEntry> ledgerEntries = ledgerRepo.findByCustomerIdOrderByCreatedAtAscIdAsc(customerId);
        BigDecimal currentDebt = BigDecimal.ZERO;
        for (LedgerEntry entry : ledgerEntries) {
            currentDebt = entry.getRunningBalance();
            if (entry.getEntryType() == LedgerEntryType.CHARGE) {
                continue; // El cargo ya se refleja con la compra/entrega y en la deuda acumulada
            }
            CustomerHistoryEventType eventType = entry.getEntryType() == LedgerEntryType.PAYMENT
                    ? CustomerHistoryEventType.PAYMENT
                    : CustomerHistoryEventType.ADJUSTMENT;
            String description = entry.getNotes() != null && !entry.getNotes().isBlank()
                    ? entry.getNotes()
                    : (eventType == CustomerHistoryEventType.PAYMENT ? "Pago registrado" : "Ajuste en cuenta corriente");
            entries.add(new CustomerHistoryItemResponse(
                    entry.getCreatedAt(),
                    eventType,
                    description,
                    entry.getAmount(),
                    entry.getRunningBalance(),
                    null,
                    null,
                    null,
                    entry.getEntryType().name(),
                    entry.getDeliveryOrder() == null ? null : entry.getDeliveryOrder().getId(),
                    entry.getDeliveryOrder() == null ? null : entry.getDeliveryOrder().getOrderCode(),
                    entry.getPaymentMethod(),
                    null,
                    null,
                    entry.getCreatedBy() == null ? null : entry.getCreatedBy().getId(),
                    entry.getCreatedBy() == null ? null : entry.getCreatedBy().getFullName()
            ));
        }

        // 3) Devoluciones con camion de reintegro
        List<TruckStockMovement> returns = movementRepo.findReturnsByCustomerId(customerId);
        for (TruckStockMovement movement : returns) {
            entries.add(new CustomerHistoryItemResponse(
                    movement.getCreatedAt() != null ? movement.getCreatedAt() : movement.getMovementDate().atStartOfDay(),
                    CustomerHistoryEventType.RETURN,
                    movement.getNotes() != null && !movement.getNotes().isBlank()
                            ? movement.getNotes()
                            : "Devolución de mercadería al camión " + movement.getTruck().getLicensePlate(),
                    null,
                    null,
                    movement.getQuantity(),
                    movement.getProduct().getId(),
                    movement.getProduct().getName(),
                    "RETURNED",
                    movement.getDeliveryOrder() == null ? null : movement.getDeliveryOrder().getId(),
                    movement.getDeliveryOrder() == null ? null : movement.getDeliveryOrder().getOrderCode(),
                    null,
                    movement.getTruck().getId(),
                    movement.getTruck().getLicensePlate(),
                    movement.getCreatedBy() == null ? null : movement.getCreatedBy().getId(),
                    movement.getCreatedBy() == null ? null : movement.getCreatedBy().getFullName()
            ));
        }

        entries.sort(Comparator.comparing(CustomerHistoryItemResponse::date,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return new CustomerHistoryResponse(
                customer.getId(),
                customer.getBusinessName(),
                currentDebt,
                entries
        );
    }
}
