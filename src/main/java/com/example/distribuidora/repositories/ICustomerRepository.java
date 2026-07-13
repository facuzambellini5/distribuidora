package com.example.distribuidora.repositories;

import com.example.distribuidora.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ICustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByActiveTrue();
}
