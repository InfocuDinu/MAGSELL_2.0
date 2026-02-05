package com.bakerymanager.repository;

import com.bakerymanager.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    List<Sale> findBySaleDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Sale> findByOperator(String operator);
    
    List<Sale> findByPaymentMethod(String paymentMethod);
    
    Optional<Sale> findByInvoiceNumber(String invoiceNumber);
    
    @Query("SELECT s FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate ORDER BY s.saleDate DESC")
    List<Sale> findSalesByDateRange(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate")
    BigDecimal getTotalSalesByDateRange(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.saleDate >= :startDate AND s.saleDate <= :endDate")
    Long getSalesCountByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM Sale s WHERE DATE(s.saleDate) = DATE('now') ORDER BY s.saleDate DESC")
    List<Sale> findTodaySales();
    
    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE DATE(s.saleDate) = DATE('now')")
    BigDecimal getTodayTotalSales();
    
    List<Sale> findTop10ByOrderBySaleDateDesc();
}
