package com.example.distribuidora.services;

import com.example.distribuidora.dtos.LedgerEntryRequest;
import com.example.distribuidora.dtos.LedgerEntryResponse;
import com.example.distribuidora.enums.LedgerEntryType;
import com.example.distribuidora.enums.OrderPaymentMethod;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Customer;
import com.example.distribuidora.models.LedgerEntry;
import com.example.distribuidora.models.Order;
import com.example.distribuidora.models.User;
import com.example.distribuidora.repositories.ILedgerEntryRepository;
import com.example.distribuidora.repositories.IOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LedgerService {

    private final ILedgerEntryRepository ledgerRepo;
    private final CustomerService customerService;
    private final IOrderRepository orderRepo;
    private final CurrentUserService currentUserService;

    @Autowired
    public LedgerService(ILedgerEntryRepository ledgerRepo,
                         CustomerService customerService,
                         IOrderRepository orderRepo,
                         CurrentUserService currentUserService) {
        this.ledgerRepo = ledgerRepo;
        this.customerService = customerService;
        this.orderRepo = orderRepo;
        this.currentUserService = currentUserService;
    }

    public LedgerService(ILedgerEntryRepository ledgerRepo, CustomerService customerService) {
        this(ledgerRepo, customerService, null, null);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getByCustomer(Long customerId) {
        customerService.getCustomerEntity(customerId);
        return ledgerRepo.findByCustomerIdOrderByCreatedAtAscIdAsc(customerId)
                .stream()
                .map(LedgerEntryResponse::new)
                .toList();
    }

    @Transactional
    public LedgerEntryResponse registerPayment(Long customerId, LedgerEntryRequest request) {
        String idempotencyKey = normalizeKey(request.idempotencyKey());
        if (idempotencyKey != null) {
            var existing = ledgerRepo.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return new LedgerEntryResponse(existing.get());
            }
        }

        Customer customer = customerService.getCustomerEntity(customerId);
        requirePositive(request.amount(), "Payment amount must be greater than zero.");

        OrderPaymentMethod method = request.paymentMethod() != null
                ? request.paymentMethod()
                : OrderPaymentMethod.CASH;

        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal transfer = BigDecimal.ZERO;

        switch (method) {
            case CASH -> {
                if (request.transferAmount() != null && request.transferAmount().compareTo(BigDecimal.ZERO) != 0) {
                    throw new BusinessRuleException("Un pago en efectivo no puede incluir importe por transferencia.");
                }
                if (request.cashAmount() != null && request.cashAmount().compareTo(request.amount()) != 0) {
                    throw new BusinessRuleException("El importe en efectivo debe coincidir con el monto total recibido.");
                }
                cash = request.amount();
            }
            case TRANSFER -> {
                if (request.cashAmount() != null && request.cashAmount().compareTo(BigDecimal.ZERO) != 0) {
                    throw new BusinessRuleException("Un pago por transferencia no puede incluir importe en efectivo.");
                }
                if (request.transferAmount() != null && request.transferAmount().compareTo(request.amount()) != 0) {
                    throw new BusinessRuleException("El importe por transferencia debe coincidir con el monto total recibido.");
                }
                transfer = request.amount();
            }
            case MIXED -> {
                cash = request.cashAmount() == null ? BigDecimal.ZERO : request.cashAmount();
                transfer = request.transferAmount() == null ? BigDecimal.ZERO : request.transferAmount();
                if (cash.compareTo(BigDecimal.ZERO) <= 0 || transfer.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException("Mixed payment requires positive cash and transfer amounts.");
                }
                if (cash.add(transfer).compareTo(request.amount()) != 0) {
                    throw new BusinessRuleException("Cash and transfer amounts must equal the total payment amount.");
                }
            }
            case CURRENT_ACCOUNT -> throw new BusinessRuleException(
                    "Un pago registrado debe usar un medio de pago válido: CASH, TRANSFER o MIXED.");
        }

        Order order = null;
        if (request.orderId() != null) {
            if (orderRepo == null) {
                throw new BusinessRuleException("Order repository is unavailable.");
            }
            order = orderRepo.findById(request.orderId())
                    .orElseThrow(() -> new EntityNotFoundException("Order", request.orderId()));
            if (!order.getCustomer().getId().equals(customerId)) {
                throw new BusinessRuleException("El pedido indicado no pertenece al cliente.");
            }
        }

        User user = resolveCurrentUser();
        LedgerEntry saved = saveEntry(
                customer, order, LedgerEntryType.PAYMENT, request.amount().negate(),
                method, cash, transfer, user, request.notes(), idempotencyKey);
        return new LedgerEntryResponse(saved);
    }

    @Transactional
    public void registerOrderPayment(Order order, BigDecimal amount) {
        registerOrderPayment(order, amount, order.getPaymentMethod(),
                order.getCashAmount(), order.getTransferAmount(), order.getDeliveredBy(), null);
    }

    @Transactional
    public void registerOrderPayment(Order order,
                                     BigDecimal amount,
                                     OrderPaymentMethod paymentMethod,
                                     BigDecimal cashAmount,
                                     BigDecimal transferAmount,
                                     User createdBy,
                                     String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return;
        requirePositive(amount, "Payment amount must be greater than zero.");

        String normalizedKey = normalizeKey(idempotencyKey);
        if (normalizedKey != null && ledgerRepo.findByIdempotencyKey(normalizedKey).isPresent()) {
            return;
        }

        User responsible = createdBy != null ? createdBy : resolveCurrentUser();
        saveEntry(order.getCustomer(), order, LedgerEntryType.PAYMENT, amount.negate(),
                paymentMethod, cashAmount, transferAmount, responsible,
                "Pago en entrega del pedido " + order.getOrderCode(), normalizedKey);
    }

    @Transactional
    public LedgerEntryResponse registerAdjustment(Long customerId, LedgerEntryRequest request) {
        String idempotencyKey = normalizeKey(request.idempotencyKey());
        if (idempotencyKey != null) {
            var existing = ledgerRepo.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return new LedgerEntryResponse(existing.get());
            }
        }

        Customer customer = customerService.getCustomerEntity(customerId);
        if (BigDecimal.ZERO.compareTo(request.amount()) == 0) {
            throw new BusinessRuleException("Adjustment amount cannot be zero.");
        }

        Order order = null;
        if (request.orderId() != null && orderRepo != null) {
            order = orderRepo.findById(request.orderId())
                    .orElseThrow(() -> new EntityNotFoundException("Order", request.orderId()));
            if (!order.getCustomer().getId().equals(customerId)) {
                throw new BusinessRuleException("El pedido indicado no pertenece al cliente.");
            }
        }

        User user = resolveCurrentUser();
        return new LedgerEntryResponse(saveEntry(
                customer, order, LedgerEntryType.ADJUSTMENT, request.amount(),
                null, null, null, user, request.notes(), idempotencyKey));
    }

    @Transactional
    public LedgerEntryResponse registerCharge(Customer customer, Order order, BigDecimal amount) {
        requirePositive(amount, "Charge amount must be greater than zero.");
        if (ledgerRepo.existsChargeForOrder(order.getId(), LedgerEntryType.CHARGE)) {
            throw new BusinessRuleException("Order already has a ledger charge.");
        }
        User user = order.getDeliveredBy() != null ? order.getDeliveredBy() : resolveCurrentUser();
        return new LedgerEntryResponse(saveEntry(
                customer, order, LedgerEntryType.CHARGE, amount,
                null, null, null, user, "Cargo por pedido " + order.getOrderCode(), null));
    }

    private LedgerEntry saveEntry(Customer customer,
                                  Order order,
                                  LedgerEntryType type,
                                  BigDecimal signedAmount,
                                  OrderPaymentMethod paymentMethod,
                                  BigDecimal cashAmount,
                                  BigDecimal transferAmount,
                                  User createdBy,
                                  String notes,
                                  String idempotencyKey) {
        BigDecimal previousBalance = ledgerRepo.findTopByCustomerIdOrderByCreatedAtDescIdDesc(customer.getId())
                .map(LedgerEntry::getRunningBalance)
                .orElse(BigDecimal.ZERO);

        LedgerEntry entry = new LedgerEntry();
        entry.setCustomer(customer);
        entry.setDeliveryOrder(order);
        entry.setEntryType(type);
        entry.setPaymentMethod(paymentMethod);
        entry.setCashAmount(cashAmount);
        entry.setTransferAmount(transferAmount);
        entry.setAmount(signedAmount.abs());
        entry.setRunningBalance(previousBalance.add(signedAmount));
        entry.setCreatedBy(createdBy);
        entry.setNotes(notes);
        entry.setIdempotencyKey(idempotencyKey);
        return ledgerRepo.save(entry);
    }

    private User resolveCurrentUser() {
        return currentUserService != null ? currentUserService.getCurrentUserOrNull() : null;
    }

    private String normalizeKey(String key) {
        return (key == null || key.isBlank()) ? null : key.trim();
    }

    private void requirePositive(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(message);
        }
    }
}
