package com.example.distribuidora.services;

import com.example.distribuidora.dtos.ProductDto;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Category;
import com.example.distribuidora.models.Product;
import com.example.distribuidora.repositories.ICategoryRepository;
import com.example.distribuidora.repositories.IProductRepository;
import com.example.distribuidora.services.interfaces.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService implements IProductService {

    @Autowired
    private IProductRepository productRepo;

    @Autowired
    private ICategoryRepository categoryRepo;

    @Override
    public Product saveProduct(ProductDto productDto) {

        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setSku(productDto.getSku());
        product.setUnitPrice(productDto.getUnitPrice());
        product.setCategory(this.resolveCategory(productDto.getCategoryId()));

        return productRepo.save(product);
    }

    @Override
    public List<Product> getProducts() {
        return productRepo.findAll();
    }

    @Override
    public Product getProduct(Long id) {
        //Lanza exception si no se encuentra
        return productRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Product", id));
    }

    @Override
    public List<Product> getActiveProducts() {
        return productRepo.findByActiveTrue();
    }

    @Override
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepo.findByCategory(categoryId);
    }

    @Override
    public ProductDto updateProduct(Long id, ProductDto productDto) {

        //Obtener Product desde el metodo getProduct (en donde se lanza exception si no se encuentra)
        Product product = this.getProduct(id);

        //Validar cada campo para evitar que no se asignen valores null
        if (productDto.getName() != null) product.setName(productDto.getName());
        if (productDto.getDescription() != null) product.setDescription(productDto.getDescription());
        if (productDto.getSku() != null) product.setSku(productDto.getSku());
        if (productDto.getUnitPrice() != null) product.setUnitPrice(productDto.getUnitPrice());
        if (productDto.getCategoryId() != null) product.setCategory(this.resolveCategory(productDto.getCategoryId()));

        return new ProductDto(productRepo.save(product));
    }

    @Override
    public void desactivateProduct(Long id) {
        Product product = this.getProduct(id);

        product.setActive(false);
        productRepo.save(product);
    }

    @Override
    public void activateProduct(Long id) {
        Product product = this.getProduct(id);

        product.setActive(true);
        productRepo.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        productRepo.deleteById(id);
    }

    //Busca la categoria por id y lanza exception si no existe; si no viene id, el producto queda sin categoria
    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepo.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category", categoryId));
    }
}
