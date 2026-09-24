package com.example.distribuidora.services.interfaces;

import com.example.distribuidora.dtos.ProductDto;
import com.example.distribuidora.models.Product;

import java.util.List;

public interface IProductService {

    //Contrato de CRUD
    Product saveProduct(ProductDto productDto);
    List<Product> getProducts();
    Product getProduct(Long id);
    List<Product> getActiveProducts();
    List<Product> getProductsByCategory(Long categoryId);
    ProductDto updateProduct(Long id, ProductDto productDto);
    void desactivateProduct(Long id);
    void activateProduct(Long id);
    void deleteProduct(Long id);
}
