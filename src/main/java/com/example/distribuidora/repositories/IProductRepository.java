package com.example.distribuidora.repositories;

import com.example.distribuidora.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();

    List<Product> findByCategory(Long categoryId);

    boolean existsBySku(String sku);
}
