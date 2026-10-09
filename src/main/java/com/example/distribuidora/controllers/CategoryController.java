package com.example.distribuidora.controllers;

import com.example.distribuidora.dtos.CategoryRequest;
import com.example.distribuidora.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/categories", "/api/category"})
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping({"", "/save"})
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> save(@Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.save(categoryRequest));
    }

    @GetMapping({"", "/get"})
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<?> getCategories(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(categoryService.getCategories(active));
    }

    @GetMapping({"/{id}", "/get/{id}"})
    @PreAuthorize("hasAuthority('products:read')")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PutMapping({"/{id}", "/update/{id}"})
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.ok(categoryService.update(id, categoryRequest));
    }

    @PatchMapping("/desactivate/{id}")
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> desactivate(@PathVariable Long id) {
        categoryService.desactivate(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping({"/{id}/activate", "/activate/{id}"})
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> activate(@PathVariable Long id) {
        categoryService.activate(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    @PreAuthorize("hasAuthority('products:write')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
