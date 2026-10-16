package com.example.distribuidora.services;

import com.example.distribuidora.dtos.LedgerEntryRequest;
import com.example.distribuidora.dtos.LedgerEntryResponse;
import com.example.distribuidora.enums.LedgerEntryType;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.models.Customer;
import com.example.distribuidora.models.LedgerEntry;
import com.example.distribuidora.models.Order;
import com.example.distribuidora.repositories.ILedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LedgerService {

    private final ILedgerEntryRepository ledgerRepo;
    private final CustomerService customerService;

    public LedgerService(ILedgerEntryRepository ledgerRepo, CustomerService customerService) {
        this.ledgerRepo = ledgerRepo;
        this.customerService = customerService;
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
        Customer customer = customerService.getCustomerEntity(customerId);
        requirePositive(request.amount(), "Payment amount must be greater than zero.");
        return new LedgerEntryResponse(saveEntry(customer, null, LedgerEntryType.PAYMENT, request.amount().negate()));
    }

    @Transactional
    public void registerOrderPayment(Order order, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return;
        requirePositive(amount, "Payment amount must be greater than zero.");
        saveEntry(order.getCustomer(), order, LedgerEntryType.PAYMENT, amount.negate());
    }

    @Transactional
    public LedgerEntryResponse registerAdjustment(Long customerId, LedgerEntryRequest request) {
        Customer customer = customerService.getCustomerEntity(customerId);
        if (BigDecimal.ZERO.compareTo(request.amount()) == 0) {
            throw new BusinessRuleException("Adjustment amount cannot be zero.");
        }
        return new LedgerEntryResponse(saveEntry(customer, null, LedgerEntryType.ADJUSTMENT, request.amount()));
    }

    @Transactional
    public LedgerEntryResponse registerCharge(Customer customer, Order order, BigDecimal amount) {
        requirePositive(amount, "Charge amount must be greater than zero.");
        if (ledgerRepo.existsChargeForOrder(order.getId(), LedgerEntryType.CHARGE)) {
            throw new BusinessRuleException("Order already has a ledger charge.");
        }
        return new LedgerEntryResponse(saveEntry(customer, order, LedgerEntryType.CHARGE, amount));
    }

    private LedgerEntry saveEntry(Customer customer, Order order, LedgerEntryType type, BigDecimal signedAmount) {
        BigDecimal previousBalance = ledgerRepo.findTopByCustomerIdOrderByCreatedAtDescIdDesc(customer.getId())
                .map(LedgerEntry::getRunningBalance)
                .orElse(BigDecimal.ZERO);

        LedgerEntry entry = new LedgerEntry();
        entry.setCustomer(customer);
        entry.setDeliveryOrder(order);
        entry.setEntryType(type);
        entry.setAmount(signedAmount.abs());
        entry.setRunningBalance(previousBalance.add(signedAmount));
        return ledgerRepo.save(entry);
    }

    private void requirePositive(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(message);
        }
    }
}
