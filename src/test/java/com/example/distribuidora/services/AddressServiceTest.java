package com.example.distribuidora.services;

import com.example.distribuidora.dtos.AddressRequest;
import com.example.distribuidora.models.Address;
import com.example.distribuidora.models.Customer;
import com.example.distribuidora.repositories.IAddressRepository;
import com.example.distribuidora.repositories.ICustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private IAddressRepository addressRepo;
    @Mock
    private ICustomerRepository customerRepo;
    @InjectMocks
    private AddressService addressService;

    @Test
    void saveFirstAddressMarksItAsDefault() {
        Customer customer = new Customer();
        customer.setId(1L);
        when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
        when(addressRepo.findByCustomerId(1L)).thenReturn(List.of());
        when(addressRepo.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        addressService.save(1L, new AddressRequest("Principal", "San Martin 100", "Cordoba", null, null, false));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepo).save(captor.capture());
        assertTrue(captor.getValue().isDefault());
    }
}
