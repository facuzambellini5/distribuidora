package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.ProductRequest;
import com.example.distribuidora.dtos.ProductResponse;
import com.example.distribuidora.services.interfaces.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Productos", description = "Gestión del catálogo de productos y precios (sin stock central).")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final IProductService productService;

    public ProductController(IProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Crear producto", description = "Requiere permiso products:write.")
    @PostMapping
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<ProductResponse> saveProduct(@Valid @RequestBody ProductRequest productRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.saveProduct(productRequest));
    }

    @Operation(summary = "Listar productos", description = "Requiere permiso products:read.")
    @GetMapping
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<List<ProductResponse>> getProducts(@RequestParam(required = false) Boolean active,
                                                             @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(productService.getProducts(active, categoryId));
    }

    @Operation(summary = "Obtener producto por ID", description = "Requiere permiso products:read.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @Operation(summary = "Actualizar producto y precio", description = "Requiere permiso products:write.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
                                                         @Valid @RequestBody ProductRequest productRequest) {
        return ResponseEntity.ok(productService.updateProduct(id, productRequest));
    }

    @Operation(summary = "Activar producto", description = "Requiere permiso products:write.")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<Void> activateProduct(@PathVariable Long id) {
        productService.activateProduct(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Desactivar producto", description = "Requiere permiso products:write.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
