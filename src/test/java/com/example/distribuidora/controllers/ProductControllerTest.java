package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.ProductResponse;
import com.example.distribuidora.exceptions.GlobalExceptionHandler;
import com.example.distribuidora.services.interfaces.IProductService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private final IProductService productService = mock(IProductService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProductController(productService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void listProductsReturnsOk() throws Exception {
        when(productService.getProducts(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    void createProductValidatesRequest() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content("""
                                {"name":"","unitPrice":0}
                                """))
                .andExpect(status().isBadRequest());
    }
}
