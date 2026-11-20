package com.example.distribuidora.services;

import com.example.distribuidora.dtos.AddressRequest;
import com.example.distribuidora.dtos.AddressResponse;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Address;
import com.example.distribuidora.models.Customer;
import com.example.distribuidora.repositories.IAddressRepository;
import com.example.distribuidora.repositories.ICustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final IAddressRepository addressRepo;
    private final ICustomerRepository customerRepo;

    public AddressService(IAddressRepository addressRepo, ICustomerRepository customerRepo) {
        this.addressRepo = addressRepo;
        this.customerRepo = customerRepo;
    }

    @Transactional
    public AddressResponse save(Long customerId, AddressRequest addressRequest) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer", customerId));

        Address address = new Address();
        address.setCustomer(customer);
        applyRequest(address, addressRequest);
        ensureDefaultRules(customerId, address, addressRequest.isDefault());

        return new AddressResponse(addressRepo.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getByCustomer(Long customerId) {
        ensureCustomerExists(customerId);
        return addressRepo.findByCustomerId(customerId).stream().map(AddressResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse get(Long customerId, Long addressId) {
        return new AddressResponse(getAddressEntity(customerId, addressId));
    }

    @Transactional(readOnly = true)
    public Address getAddressEntity(Long customerId, Long addressId) {
        Address address = addressRepo.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address", addressId));
        if (!address.getCustomer().getId().equals(customerId)) {
            throw new EntityNotFoundException("Address", addressId);
        }
        return address;
    }

    @Transactional
    public AddressResponse update(Long customerId, Long addressId, AddressRequest addressRequest) {
        Address address = getAddressEntity(customerId, addressId);
        applyRequest(address, addressRequest);
        ensureDefaultRules(customerId, address, addressRequest.isDefault());
        return new AddressResponse(addressRepo.save(address));
    }

    @Transactional
    public void delete(Long customerId, Long addressId) {
        Address address = getAddressEntity(customerId, addressId);
        addressRepo.delete(address);
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setLabel(request.label());
        address.setStreetAddress(request.streetAddress());
        address.setCity(request.city());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
        address.setDefault(Boolean.TRUE.equals(request.isDefault()));
    }

    private void ensureDefaultRules(Long customerId, Address address, Boolean requestedDefault) {
        List<Address> current = addressRepo.findByCustomerId(customerId);
        boolean willBeDefault = Boolean.TRUE.equals(requestedDefault) || current.isEmpty();
        if (willBeDefault) {
            addressRepo.clearDefaultForCustomer(customerId);
            address.setDefault(true);
        } else if (current.size() == 1 && current.getFirst().getId().equals(address.getId())) {
            throw new BusinessRuleException("A customer must keep one default address while addresses exist.");
        }
    }

    private void ensureCustomerExists(Long customerId) {
        if (!customerRepo.existsById(customerId)) {
            throw new EntityNotFoundException("Customer", customerId);
        }
    }
}

