package com.bakerymanager.repository;

import com.bakerymanager.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    List<Invoice> findBySupplierNameContainingIgnoreCase(String supplierName);
    
    List<Invoice> findByIsSpvImportedTrue();
    
    @Query("SELECT i FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate ORDER BY i.invoiceDate DESC")
    List<Invoice> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT i FROM Invoice i WHERE YEAR(i.invoiceDate) = :year AND MONTH(i.invoiceDate) = :month ORDER BY i.invoiceDate DESC")
    List<Invoice> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
    
    @Query("SELECT i FROM Invoice i WHERE i.supplierName LIKE %:searchTerm% OR i.invoiceNumber LIKE %:searchTerm%")
    List<Invoice> searchInvoices(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.isSpvImported = true")
    long countSpvInvoices();
}
