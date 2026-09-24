package com.example.distribuidora.services;

import com.example.distribuidora.dtos.CategoryRequest;
import com.example.distribuidora.dtos.CategoryResponse;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Category;
import com.example.distribuidora.repositories.ICategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private ICategoryRepository categoryRepo;

    public CategoryResponse save(CategoryRequest categoryRequest){

        if (categoryRequest == null) {
            throw new IllegalArgumentException("CategoryRequest can´t be null");
        }

        Category category = new Category();
        category.setName(categoryRequest.name());

        Long parentId = categoryRequest.parentCategoryId();
        if (parentId != null) {
            Category parentCategory = categoryRepo.findById(parentId)
                    .orElseThrow(() -> new EntityNotFoundException("Category", parentId));
            category.setParentCategory(parentCategory);
        } else {
            category.setParentCategory(null);
        }

        Category saved = categoryRepo.save(category);
        return new CategoryResponse(saved);
    }

    //TODO VER CÓMO DEVOLVER LAS CATEGORÍAS: ES DECIR, VER SI DEVOLVER TODAS O DE MANERA MÁS ORDENADA LAS CATEGORÍAS HIJAS CON LAS PADRES
    public List<CategoryResponse> getCategories() {
        return categoryRepo.findAll()
                .stream()
                .map(CategoryResponse::new)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        return categoryRepo.findById(id).map(CategoryResponse::new).orElseThrow(() -> new EntityNotFoundException("Category", id));
    }

    public CategoryResponse update(Long id, CategoryRequest categoryRequest) {

        if (categoryRequest == null) {
            throw new IllegalArgumentException("CategoryRequest can´t be null");
        }

        Category category = categoryRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category", id));
        category.setName(categoryRequest.name());

        Long parentId = categoryRequest.parentCategoryId();
        if (parentId != null) {
            Category parentCategory = categoryRepo.findById(parentId)
                    .orElseThrow(() -> new EntityNotFoundException("Category", parentId));
            category.setParentCategory(parentCategory);
        } else {
            category.setParentCategory(null);
        }

        Category saved = categoryRepo.save(category);
        return new CategoryResponse(saved);
    }

    public void desactivate(Long id) {
        Category category = categoryRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Category", id));
        category.setActive(false);
        categoryRepo.save(category);
    }

    public void activate(Long id) {
        Category category = categoryRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Category", id));
        category.setActive(true);
        categoryRepo.save(category);
    }

    public void delete(Long id) {
        Category category = categoryRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Category", id));
        categoryRepo.delete(category);
    }
}
