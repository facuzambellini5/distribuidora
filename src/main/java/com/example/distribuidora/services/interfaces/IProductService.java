package com.example.distribuidora.services.interfaces;

import com.example.distribuidora.dtos.ProductRequest;
import com.example.distribuidora.dtos.ProductResponse;

import java.util.List;

public interface IProductService {

    ProductResponse saveProduct(ProductRequest productRequest);
    List<ProductResponse> getProducts(Boolean active, Long categoryId);
    ProductResponse getProduct(Long id);
    ProductResponse updateProduct(Long id, ProductRequest productRequest);
    void desactivateProduct(Long id);
    void activateProduct(Long id);
    void deleteProduct(Long id);
}
