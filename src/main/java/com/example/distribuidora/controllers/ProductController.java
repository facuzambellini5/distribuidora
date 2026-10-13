package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.ProductDto;
import com.example.distribuidora.dtos.ProductRequest;
import com.example.distribuidora.services.interfaces.IProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/products", "/api/product"})
public class ProductController {

    private final IProductService productService;

    public ProductController(IProductService productService) {
        this.productService = productService;
    }

    @PostMapping({"", "/save"})
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> saveProduct(@Valid @RequestBody ProductRequest productRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.saveProduct(productRequest));
    }

    @PostMapping("/save-legacy")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> saveProductLegacy(@Valid @RequestBody ProductDto productDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.saveProduct(toRequest(productDto)));
    }

    @GetMapping({"", "/get"})
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<?> getProducts(@RequestParam(required = false) Boolean active,
                                         @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(productService.getProducts(active, categoryId));
    }

    @GetMapping({"/{id}", "/get/{id}"})
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping("/get/active")
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<?> getActiveProducts() {
        return ResponseEntity.ok(productService.getProducts(true, null));
    }

    @GetMapping("/get/category/{categoryId}")
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<?> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProducts(null, categoryId));
    }

    @PutMapping({"/{id}", "/update/{id}"})
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id,
                                            @Valid @RequestBody ProductRequest productRequest) {

        return ResponseEntity.ok(productService.updateProduct(id, productRequest));
    }

    @PatchMapping("/desactivate/{id}")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> desactivateProduct(@PathVariable Long id) {
        productService.desactivateProduct(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping({"/{id}/activate", "/activate/{id}"})
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> activateProduct(@PathVariable Long id) {
        productService.activateProduct(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private ProductRequest toRequest(ProductDto dto) {
        return new ProductRequest(dto.getName(), dto.getDescription(), dto.getSku(), dto.getUnitPrice(), dto.getCategoryId());
    }
}
