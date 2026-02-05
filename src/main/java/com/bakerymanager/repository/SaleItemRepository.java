package com.bakerymanager.repository;

import com.bakerymanager.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    
    List<SaleItem> findBySaleId(Long saleId);
    
    List<SaleItem> findByProductId(Long productId);
    
    List<SaleItem> findByProductNameContaining(String productName);
    
    @Query("SELECT si FROM SaleItem si WHERE si.sale.id = :saleId ORDER BY si.createdAt ASC")
    List<SaleItem> findItemsBySaleId(@Param("saleId") Long saleId);
    
    @Query("SELECT si.product.name, SUM(si.quantity), SUM(si.totalPrice) " +
           "FROM SaleItem si " +
           "WHERE si.sale.saleDate >= :startDate AND si.sale.saleDate <= :endDate " +
           "GROUP BY si.product.name " +
           "ORDER BY SUM(si.totalPrice) DESC")
    List<Object[]> getTopSellingProductsByDateRange(@Param("startDate") java.time.LocalDateTime startDate,
                                                     @Param("endDate") java.time.LocalDateTime endDate);
    
    @Query("SELECT COUNT(DISTINCT si.product.id) FROM SaleItem si " +
           "WHERE si.sale.saleDate >= :startDate AND si.sale.saleDate <= :endDate")
    Long getUniqueProductsSoldByDateRange(@Param("startDate") java.time.LocalDateTime startDate,
                                          @Param("endDate") java.time.LocalDateTime endDate);
}
