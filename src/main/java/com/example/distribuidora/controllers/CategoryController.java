package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.CategoryRequest;
import com.example.distribuidora.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> save(@Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.save(categoryRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<?> getCategories(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(categoryService.getCategories(active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.ok(categoryService.update(id, categoryRequest));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        categoryService.activate(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
