package com.bakerymanager.repository;

import com.bakerymanager.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByName(String name);
    
    Optional<Product> findByBarcode(String barcode);
    
    List<Product> findByNameContainingIgnoreCase(String name);
    
    List<Product> findByIsActiveTrue();
    
    List<Product> findByIsActiveTrueOrderByName();
    
    @Query("SELECT p FROM Product p WHERE p.physicalStock <= p.minimumStock AND p.isActive = true")
    List<Product> findLowStockProducts();
    
    @Query("SELECT p FROM Product p WHERE p.physicalStock > 0 AND p.isActive = true ORDER BY p.name")
    List<Product> findAvailableProducts();
    
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR p.barcode = :searchTerm")
    List<Product> searchProducts(@Param("searchTerm") String searchTerm);
}
