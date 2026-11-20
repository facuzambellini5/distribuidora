package com.example.distribuidora.repositories;

import com.example.distribuidora.models.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IAddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByCustomerId(Long customerId);

    @Modifying
    void deleteByIdAndCustomerId(Long id, Long customerId);

    @Modifying
    @Query("update Address a set a.isDefault = false where a.customer.id = :customerId and a.isDefault = true")
    int clearDefaultForCustomer(@Param("customerId") Long customerId);
}
