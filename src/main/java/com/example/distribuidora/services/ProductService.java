package com.example.distribuidora.services;

import com.example.distribuidora.dtos.ProductRequest;
import com.example.distribuidora.dtos.ProductResponse;
import com.example.distribuidora.exceptions.DuplicateResourceException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Category;
import com.example.distribuidora.models.Product;
import com.example.distribuidora.repositories.ICategoryRepository;
import com.example.distribuidora.repositories.IProductRepository;
import com.example.distribuidora.services.interfaces.IProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService implements IProductService {

    private final IProductRepository productRepo;
    private final ICategoryRepository categoryRepo;

    public ProductService(IProductRepository productRepo, ICategoryRepository categoryRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
    }

    @Override
    @Transactional
    public ProductResponse saveProduct(ProductRequest request) {
        validateUniqueSku(request.sku(), null);

        Product product = new Product();
        applyRequest(product, request);

        return new ProductResponse(productRepo.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(Boolean active, Long categoryId) {
        List<Product> products;
        if (categoryId != null) {
            products = productRepo.findByCategoryId(categoryId);
        } else if (Boolean.TRUE.equals(active)) {
            products = productRepo.findByActiveTrue();
        } else {
            products = productRepo.findAll();
        }
        return products.stream().map(ProductResponse::new).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return new ProductResponse(getProductEntity(id));
    }

    @Transactional(readOnly = true)
    public Product getProductEntity(Long id) {
        return productRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Product", id));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = getProductEntity(id);
        validateUniqueSku(request.sku(), id);
        applyRequest(product, request);

        return new ProductResponse(productRepo.save(product));
    }

    @Override
    @Transactional
    public void desactivateProduct(Long id) {
        Product product = getProductEntity(id);
        product.setActive(false);
        productRepo.save(product);
    }

    @Override
    @Transactional
    public void activateProduct(Long id) {
        Product product = getProductEntity(id);
        product.setActive(true);
        productRepo.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        desactivateProduct(id);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepo.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category", categoryId));
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setUnitPrice(request.unitPrice());
        product.setUnit(request.unit());
        product.setCategory(resolveCategory(request.categoryId()));
    }

    private void validateUniqueSku(String sku, Long currentId) {
        if (sku == null || sku.isBlank()) return;
        boolean exists = currentId == null
                ? productRepo.existsBySku(sku)
                : productRepo.existsBySkuAndIdNot(sku, currentId);
        if (exists) {
            throw new DuplicateResourceException("Product sku already exists: " + sku);
        }
    }
}
