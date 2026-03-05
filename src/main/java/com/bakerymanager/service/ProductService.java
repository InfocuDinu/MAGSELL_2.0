package com.bakerymanager.service;

import com.bakerymanager.entity.Product;
import com.bakerymanager.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    
    @Cacheable("productsAll")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    @Cacheable("productsActive")
    public List<Product> getActiveProducts() {
        return productRepository.findByIsActiveTrueOrderByName();
    }
    
    @Cacheable("productsAvailable")
    public List<Product> getAvailableProducts() {
        return productRepository.findAvailableProducts();
    }
    
    @Cacheable(value = "productsById", key = "#id", unless = "#id == null")
    public Optional<Product> getProductById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return productRepository.findById(id);
    }
    
    public Optional<Product> getProductByName(String name) {
        return productRepository.findByName(name);
    }
    
    @Cacheable(value = "productsByBarcode", key = "#barcode", condition = "#barcode != null && !#barcode.trim().isEmpty()")
    public Optional<Product> getProductByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "productsAll", allEntries = true),
        @CacheEvict(value = "productsActive", allEntries = true),
        @CacheEvict(value = "productsAvailable", allEntries = true),
        @CacheEvict(value = "productsLowStock", allEntries = true),
        @CacheEvict(value = "productsById", allEntries = true),
        @CacheEvict(value = "productsByBarcode", allEntries = true)
    })
    public Product saveProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Produsul nu poate fi null");
        }
        return productRepository.save(product);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "productsAll", allEntries = true),
        @CacheEvict(value = "productsActive", allEntries = true),
        @CacheEvict(value = "productsAvailable", allEntries = true),
        @CacheEvict(value = "productsLowStock", allEntries = true),
        @CacheEvict(value = "productsById", allEntries = true),
        @CacheEvict(value = "productsByBarcode", allEntries = true)
    })
    public Product createProduct(String name, BigDecimal salePrice, BigDecimal initialStock) {
        Product product = new Product();
        product.setName(name);
        product.setSalePrice(salePrice);
        product.setPhysicalStock(initialStock != null ? initialStock : BigDecimal.ZERO);
        product.setMinimumStock(BigDecimal.ZERO);
        product.setIsActive(true);
        return productRepository.save(product);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "productsAll", allEntries = true),
        @CacheEvict(value = "productsActive", allEntries = true),
        @CacheEvict(value = "productsAvailable", allEntries = true),
        @CacheEvict(value = "productsLowStock", allEntries = true),
        @CacheEvict(value = "productsById", allEntries = true),
        @CacheEvict(value = "productsByBarcode", allEntries = true)
    })
    public void deleteProduct(Long id) {
        if (id != null) {
            productRepository.deleteById(id);
        }
    }
    
    public List<Product> searchProducts(String searchTerm) {
        return productRepository.searchProducts(searchTerm);
    }
    
    @Cacheable("productsLowStock")
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }
    
    @Caching(evict = {
        @CacheEvict(value = "productsAll", allEntries = true),
        @CacheEvict(value = "productsActive", allEntries = true),
        @CacheEvict(value = "productsAvailable", allEntries = true),
        @CacheEvict(value = "productsLowStock", allEntries = true),
        @CacheEvict(value = "productsById", allEntries = true),
        @CacheEvict(value = "productsByBarcode", allEntries = true)
    })
    public void addStock(Long productId, BigDecimal quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("ID-ul produsului nu poate fi null");
        }
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.addStock(quantity);
        productRepository.save(product);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "productsAll", allEntries = true),
        @CacheEvict(value = "productsActive", allEntries = true),
        @CacheEvict(value = "productsAvailable", allEntries = true),
        @CacheEvict(value = "productsLowStock", allEntries = true),
        @CacheEvict(value = "productsById", allEntries = true),
        @CacheEvict(value = "productsByBarcode", allEntries = true)
    })
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
    
    @Caching(evict = {
        @CacheEvict(value = "productsAll", allEntries = true),
        @CacheEvict(value = "productsActive", allEntries = true),
        @CacheEvict(value = "productsAvailable", allEntries = true),
        @CacheEvict(value = "productsLowStock", allEntries = true),
        @CacheEvict(value = "productsById", allEntries = true),
        @CacheEvict(value = "productsByBarcode", allEntries = true)
    })
    public void deactivateProduct(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID-ul produsului nu poate fi null");
        }
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }
    
    @Caching(evict = {
        @CacheEvict(value = "productsAll", allEntries = true),
        @CacheEvict(value = "productsActive", allEntries = true),
        @CacheEvict(value = "productsAvailable", allEntries = true),
        @CacheEvict(value = "productsLowStock", allEntries = true),
        @CacheEvict(value = "productsById", allEntries = true),
        @CacheEvict(value = "productsByBarcode", allEntries = true)
    })
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
