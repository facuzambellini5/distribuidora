package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.CategoryRequest;
import com.example.distribuidora.dtos.CategoryResponse;
import com.example.distribuidora.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categorías", description = "Gestión de categorías de productos.")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Crear categoría", description = "Requiere permiso products:write.")
    @PostMapping
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<CategoryResponse> save(@Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.save(categoryRequest));
    }

    @Operation(summary = "Listar categorías", description = "Requiere permiso products:read.")
    @GetMapping
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<List<CategoryResponse>> getCategories(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(categoryService.getCategories(active));
    }

    @Operation(summary = "Obtener categoría por ID", description = "Requiere permiso products:read.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @Operation(summary = "Actualizar categoría", description = "Requiere permiso products:write.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.ok(categoryService.update(id, categoryRequest));
    }

    @Operation(summary = "Activar categoría", description = "Requiere permiso products:write.")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        categoryService.activate(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Desactivar categoría", description = "Requiere permiso products:write.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
