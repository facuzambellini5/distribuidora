package com.example.distribuidora.dtos;

public record CategoryRequest(
        String name,
        Long parentCategoryId
) {
}
