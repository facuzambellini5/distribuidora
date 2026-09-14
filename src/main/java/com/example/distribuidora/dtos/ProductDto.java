package com.example.distribuidora.dtos;

import com.example.distribuidora.models.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductDto {

    @Length(min = 3, max = 150, message = "Name must be between 3 and 150 characters")
    private String name;

    @Length(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @Length(min = 2, max = 50, message = "SKU must be between 2 and 50 characters")
    private String sku;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;

    //Id de la categoria a la que pertenece el producto (opcional)
    private Long categoryId;

    public ProductDto(Product product) {
        this.name = product.getName();
        this.description = product.getDescription();
        this.sku = product.getSku();
        this.unitPrice = product.getUnitPrice();
        this.categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
    }
}
