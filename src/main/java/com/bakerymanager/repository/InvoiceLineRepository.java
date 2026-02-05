package com.bakerymanager.repository;

import com.bakerymanager.entity.InvoiceLine;
import com.bakerymanager.entity.Invoice;
import com.bakerymanager.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
    
    List<InvoiceLine> findByInvoice(Invoice invoice);
    
    List<InvoiceLine> findByIngredient(Ingredient ingredient);
    
    @Query("SELECT il FROM InvoiceLine il WHERE il.invoice.id = :invoiceId")
    List<InvoiceLine> findByInvoiceId(@Param("invoiceId") Long invoiceId);
    
    @Query("SELECT il FROM InvoiceLine il WHERE il.ingredient.id = :ingredientId ORDER BY il.invoice.invoiceDate DESC")
    List<InvoiceLine> findByIngredientId(@Param("ingredientId") Long ingredientId);
    
    @Query("SELECT il FROM InvoiceLine il WHERE il.productName LIKE %:searchTerm%")
    List<InvoiceLine> findByProductNameContaining(@Param("searchTerm") String searchTerm);
}
