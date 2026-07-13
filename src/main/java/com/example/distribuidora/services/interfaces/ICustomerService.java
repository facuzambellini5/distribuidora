package com.example.distribuidora.services.interfaces;

import com.example.distribuidora.dtos.CustomerDto;
import com.example.distribuidora.models.Customer;

import java.util.List;

public interface ICustomerService {

    //Contrato de CRUD
    Customer saveCustomer(CustomerDto customerDto);
    List<CustomerDto> getCustomers();
    Customer getCustomer(Long id);
    CustomerDto updateCustomer(Long id, CustomerDto customerDto);
    void desactivateCustomer(Long id);
    void deleteCustomer(Long id);
}
