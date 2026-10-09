package com.example.distribuidora.services;

import com.example.distribuidora.dtos.LedgerEntryRequest;
import com.example.distribuidora.models.Customer;
import com.example.distribuidora.models.LedgerEntry;
import com.example.distribuidora.repositories.ILedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private ILedgerEntryRepository ledgerRepo;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private LedgerService ledgerService;

    @Test
    void registerPaymentStoresPositiveAmountAndSubtractsFromRunningBalance() {
        Customer customer = new Customer();
        customer.setId(1L);
        LedgerEntry previous = new LedgerEntry();
        previous.setRunningBalance(new BigDecimal("100.00"));

        when(customerService.getCustomerEntity(1L)).thenReturn(customer);
        when(ledgerRepo.findTopByCustomerIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(Optional.of(previous));
        when(ledgerRepo.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ledgerService.registerPayment(1L, new LedgerEntryRequest(new BigDecimal("30.00")));

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        org.mockito.Mockito.verify(ledgerRepo).save(captor.capture());
        assertEquals(new BigDecimal("30.00"), captor.getValue().getAmount());
        assertEquals(new BigDecimal("70.00"), captor.getValue().getRunningBalance());
    }
}
