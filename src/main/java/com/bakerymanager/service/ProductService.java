package com.bakerymanager.service;

import com.bakerymanager.entity.Product;
import com.bakerymanager.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public List<Product> getActiveProducts() {
        return productRepository.findByIsActiveTrueOrderByName();
    }
    
    public List<Product> getAvailableProducts() {
        return productRepository.findAvailableProducts();
    }
    
    public Optional<Product> getProductById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return productRepository.findById(id);
    }
    
    public Optional<Product> getProductByName(String name) {
        return productRepository.findByName(name);
    }
    
    public Optional<Product> getProductByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }
    
    public Product saveProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Produsul nu poate fi null");
        }
        return productRepository.save(product);
    }
    
    public Product createProduct(String name, BigDecimal salePrice, BigDecimal initialStock) {
        Product product = new Product();
        product.setName(name);
        product.setSalePrice(salePrice);
        product.setPhysicalStock(initialStock != null ? initialStock : BigDecimal.ZERO);
        product.setMinimumStock(BigDecimal.ZERO);
        product.setIsActive(true);
        return productRepository.save(product);
    }
    
    public void deleteProduct(Long id) {
        if (id != null) {
            productRepository.deleteById(id);
        }
    }
    
    public List<Product> searchProducts(String searchTerm) {
        return productRepository.searchProducts(searchTerm);
    }
    
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }
    
    public void addStock(Long productId, BigDecimal quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("ID-ul produsului nu poate fi null");
        }
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.addStock(quantity);
        productRepository.save(product);
    }
    
    public void removeStock(Long productId, BigDecimal quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("ID-ul produsului nu poate fi null");
        }
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        if (!product.hasSufficientStock(quantity)) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }
        
        product.removeStock(quantity);
        productRepository.save(product);
    }
    
    public boolean hasSufficientStock(Long productId, BigDecimal requiredQuantity) {
        if (productId == null) {
            throw new IllegalArgumentException("ID-ul produsului nu poate fi null");
        }
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        return product.hasSufficientStock(requiredQuantity);
    }
    
    public void deactivateProduct(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID-ul produsului nu poate fi null");
        }
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }
    
    public void activateProduct(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID-ul produsului nu poate fi null");
        }
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        product.setIsActive(true);
        productRepository.save(product);
    }
    
    public long countAvailableProducts() {
        return productRepository.countAvailableProducts();
    }
}
