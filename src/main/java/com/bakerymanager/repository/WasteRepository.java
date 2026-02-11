package com.bakerymanager.repository;

import com.bakerymanager.entity.Waste;
import com.bakerymanager.entity.Product;
import com.bakerymanager.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WasteRepository extends JpaRepository<Waste, Long> {
    
    // Find by waste date range
    List<Waste> findByWasteDateBetweenOrderByWasteDateDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find by item type
    List<Waste> findByItemTypeOrderByWasteDateDesc(Waste.ItemType itemType);
    
    // Find by waste reason
    List<Waste> findByReasonOrderByWasteDateDesc(Waste.WasteReason reason);
    
    // Find by product
    List<Waste> findByProductOrderByWasteDateDesc(Product product);
    
    // Find by ingredient
    List<Waste> findByIngredientOrderByWasteDateDesc(Ingredient ingredient);
    
    // Get all waste ordered by date
    List<Waste> findAllByOrderByWasteDateDesc();
    
    // Get total waste cost in date range
    @Query("SELECT SUM(w.estimatedCost) FROM Waste w WHERE w.wasteDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal getTotalWasteCost(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Get waste by reason in date range
    @Query("SELECT w FROM Waste w WHERE w.reason = :reason AND w.wasteDate BETWEEN :startDate AND :endDate ORDER BY w.wasteDate DESC")
    List<Waste> findByReasonAndDateRange(@Param("reason") Waste.WasteReason reason, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
