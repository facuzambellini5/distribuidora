package com.example.distribuidora.dtos;

import com.example.distribuidora.models.Category;

public record CategoryResponse(
        Long id,
        String name,
        Long parentCategoryId,
        boolean active
) {

    public CategoryResponse(Category category){
        this(
                category.getId(),
                category.getName(),
                category.getParentCategory() == null
                        ? null
                        : category.getParentCategory().getId(),
                category.isActive()
        );
    }
}
