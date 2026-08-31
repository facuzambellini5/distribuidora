package com.example.distribuidora.services;

import com.example.distribuidora.dtos.CustomerDto;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Customer;
import com.example.distribuidora.repositories.ICustomerRepository;
import com.example.distribuidora.services.interfaces.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService implements ICustomerService {

    @Autowired
    private ICustomerRepository customerRepo;

    //TODO AGREGAR GUARDAR DIRECCIÓN
    @Override
    public Customer saveCustomer(CustomerDto customerDto){

        Customer customer = new Customer();
        customer.setBusinessName(customerDto.getBusinessName());
        customer.setTaxId(customerDto.getTaxId());
        customer.setName(customerDto.getName());
        customer.setPhone(customerDto.getPhone());
        customer.setEmail(customerDto.getEmail());

        return customerRepo.save(customer);
    }

    @Override
    public List<Customer> getCustomers() {
        return customerRepo.findAll();
    }

    @Override
    public Customer getCustomer(Long id) {
        //Lanza exception si no se encuentra
        return customerRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Customer", id));
    }

    @Override
    public List<Customer> getActiveCustomers() {
        return customerRepo.findByActiveTrue();
    }


    @Override
    public CustomerDto updateCustomer(Long id, CustomerDto customerDto) {

        //Obtener Customer desde el metodo getCustomer (en donde se lanza exception si no se encuentra)
        Customer customer = this.getCustomer(id);

        //Validar cada campo para evitar que no se asignen valores null
        if (customerDto.getBusinessName() != null) customer.setBusinessName(customerDto.getBusinessName());
        if (customerDto.getTaxId() != null) customer.setTaxId(customerDto.getTaxId());
        if (customerDto.getName() != null) customer.setName(customerDto.getName());
        if (customerDto.getPhone() != null) customer.setPhone(customerDto.getPhone());
        if (customerDto.getEmail() != null) customer.setEmail(customerDto.getEmail());

        return new CustomerDto(customerRepo.save(customer));
    }

    @Override
    public void desactivateCustomer(Long id) {
        Customer customer = this.getCustomer(id);

        customer.setActive(false);
        customerRepo.save(customer);
    }

    @Override
    public void activateCustomer(Long id) {
        Customer customer = this.getCustomer(id);

        customer.setActive(true);
        customerRepo.save(customer);
    }

    @Override
    public void deleteCustomer(Long id) {
        customerRepo.deleteById(id);
    }
}
