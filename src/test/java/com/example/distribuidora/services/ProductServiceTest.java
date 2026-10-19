package com.example.distribuidora.services;

import com.example.distribuidora.dtos.ProductRequest;
import com.example.distribuidora.dtos.ProductResponse;
import com.example.distribuidora.enums.ProductUnit;
import com.example.distribuidora.models.Product;
import com.example.distribuidora.repositories.ICategoryRepository;
import com.example.distribuidora.repositories.IProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private IProductRepository productRepo;
    @Mock
    private ICategoryRepository categoryRepo;
    @InjectMocks
    private ProductService productService;

    @Test
    void saveProductStoresUnit() {
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.saveProduct(
                new ProductRequest("Harina", null, null, new BigDecimal("1500.00"), ProductUnit.KILOGRAM, null));

        assertEquals(ProductUnit.KILOGRAM, response.unit());
    }

    @Test
    void updateProductChangesUnit() {
        Product existing = new Product();
        existing.setId(1L);
        existing.setName("Aceite");
        existing.setUnitPrice(new BigDecimal("900.00"));
        existing.setUnit(ProductUnit.UNIT);
        when(productRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.updateProduct(
                1L, new ProductRequest("Aceite", null, null, new BigDecimal("900.00"), ProductUnit.LITER, null));

        assertEquals(ProductUnit.LITER, response.unit());
    }
}
