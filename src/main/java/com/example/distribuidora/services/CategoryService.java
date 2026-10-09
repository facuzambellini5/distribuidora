package com.example.distribuidora.services;

import com.example.distribuidora.dtos.CategoryRequest;
import com.example.distribuidora.dtos.CategoryResponse;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Category;
import com.example.distribuidora.repositories.ICategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final ICategoryRepository categoryRepo;

    public CategoryService(ICategoryRepository categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    @Transactional
    public CategoryResponse save(CategoryRequest categoryRequest) {

        Category category = new Category();
        category.setName(categoryRequest.name());
        category.setParentCategory(resolveParent(categoryRequest.parentCategoryId(), null));

        Category saved = categoryRepo.save(category);
        return new CategoryResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Boolean active) {
        List<Category> categories = Boolean.TRUE.equals(active) ? categoryRepo.findByActiveTrue() : categoryRepo.findAll();
        return categories
                .stream()
                .map(CategoryResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return new CategoryResponse(getCategoryEntity(id));
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntity(Long id) {
        return categoryRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Category", id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest categoryRequest) {
        Category category = getCategoryEntity(id);
        category.setName(categoryRequest.name());
        category.setParentCategory(resolveParent(categoryRequest.parentCategoryId(), id));

        Category saved = categoryRepo.save(category);
        return new CategoryResponse(saved);
    }

    @Transactional
    public void desactivate(Long id) {
        Category category = getCategoryEntity(id);
        category.setActive(false);
        categoryRepo.save(category);
    }

    @Transactional
    public void activate(Long id) {
        Category category = getCategoryEntity(id);
        category.setActive(true);
        categoryRepo.save(category);
    }

    @Transactional
    public void delete(Long id) {
        desactivate(id);
    }

    private Category resolveParent(Long parentId, Long currentId) {
        if (parentId == null) return null;
        if (parentId.equals(currentId)) {
            throw new BusinessRuleException("A category cannot be its own parent.");
        }
        Category parent = getCategoryEntity(parentId);
        if (parent.getParentCategory() != null) {
            throw new BusinessRuleException("Categories support at most two hierarchy levels.");
        }
        return parent;
    }
}
