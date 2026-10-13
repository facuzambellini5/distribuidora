package com.example.distribuidora.services;

import com.example.distribuidora.dtos.AddressRequest;
import com.example.distribuidora.dtos.CustomerRequest;
import com.example.distribuidora.dtos.CustomerResponse;
import com.example.distribuidora.exceptions.DuplicateResourceException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Address;
import com.example.distribuidora.models.Customer;
import com.example.distribuidora.repositories.ICustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService{

    private final ICustomerRepository customerRepo;

    public CustomerService(ICustomerRepository customerRepo) {
        this.customerRepo = customerRepo;
    }

    @Transactional
    public CustomerResponse saveCustomer(CustomerRequest customerRequest) {
        validateUniqueCustomer(customerRequest.email(), customerRequest.taxId(), null);
        Customer customer = new Customer();
        applyRequest(customer, customerRequest);

        return new CustomerResponse(customerRepo.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomers(Boolean active) {
        List<Customer> customers = Boolean.TRUE.equals(active) ? customerRepo.findByActiveTrue() : customerRepo.findAll();
        return customers.stream().map(CustomerResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {
        return new CustomerResponse(getCustomerEntity(id));
    }

    @Transactional(readOnly = true)
    public Customer getCustomerEntity(Long id) {
        return customerRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Customer", id));
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest customerRequest) {
        Customer customer = getCustomerEntity(id);
        validateUniqueCustomer(customerRequest.email(), customerRequest.taxId(), id);
        applyRequest(customer, customerRequest);
        return new CustomerResponse(customerRepo.save(customer));
    }

    @Transactional
    public void desactivateCustomer(Long id) {
        Customer customer = getCustomerEntity(id);
        customer.setActive(false);
        customerRepo.save(customer);
    }

    @Transactional
    public void activateCustomer(Long id) {
        Customer customer = getCustomerEntity(id);
        customer.setActive(true);
        customerRepo.save(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        desactivateCustomer(id);
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setBusinessName(request.businessName());
        customer.setTaxId(request.taxId());
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());

        if (request.addresses() != null) {
            customer.getAddresses().clear();
            List<Address> addresses = new ArrayList<>();
            boolean hasDefault = request.addresses().stream().anyMatch(a -> Boolean.TRUE.equals(a.isDefault()));
            for (int i = 0; i < request.addresses().size(); i++) {
                Address address = toAddress(request.addresses().get(i), customer);
                address.setDefault(Boolean.TRUE.equals(request.addresses().get(i).isDefault()) || (!hasDefault && i == 0));
                addresses.add(address);
            }
            customer.getAddresses().addAll(addresses);
        }
    }

    private Address toAddress(AddressRequest request, Customer customer) {
        Address address = new Address();
        address.setCustomer(customer);
        address.setLabel(request.label());
        address.setStreetAddress(request.streetAddress());
        address.setCity(request.city());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
        address.setDefault(Boolean.TRUE.equals(request.isDefault()));
        return address;
    }

    private void validateUniqueCustomer(String email, String taxId, Long currentId) {
        if (email != null && !email.isBlank()) {
            boolean existsEmail = currentId == null
                    ? customerRepo.existsByEmail(email)
                    : customerRepo.existsByEmailAndIdNot(email, currentId);
            if (existsEmail) {
                throw new DuplicateResourceException("Customer email already exists: " + email);
            }
        }
        if (taxId != null && !taxId.isBlank()) {
            boolean existsTaxId = currentId == null
                    ? customerRepo.existsByTaxId(taxId)
                    : customerRepo.existsByTaxIdAndIdNot(taxId, currentId);
            if (existsTaxId) {
                throw new DuplicateResourceException("Customer tax id already exists: " + taxId);
            }
        }
    }
}
