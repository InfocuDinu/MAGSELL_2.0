package com.bakerymanager.repository;

import com.bakerymanager.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    // Find by phone number
    Optional<Customer> findByPhone(String phone);
    
    // Find by email
    Optional<Customer> findByEmail(String email);
    
    // Find by name (case-insensitive)
    List<Customer> findByNameContainingIgnoreCase(String name);
    
    // Find active customers
    List<Customer> findByIsActiveTrueOrderByNameAsc();
    
    // Find customers with loyalty points above threshold
    @Query("SELECT c FROM Customer c WHERE c.loyaltyPoints >= :minPoints ORDER BY c.loyaltyPoints DESC")
    List<Customer> findByLoyaltyPointsGreaterThanEqual(@Param("minPoints") Integer minPoints);
    
    // Find top customers by total purchases
    @Query("SELECT c FROM Customer c WHERE c.isActive = true ORDER BY c.totalPurchases DESC")
    List<Customer> findTopCustomersByPurchases();
    
    // Check if phone exists
    boolean existsByPhone(String phone);
    
    // Check if email exists
    boolean existsByEmail(String email);
}
