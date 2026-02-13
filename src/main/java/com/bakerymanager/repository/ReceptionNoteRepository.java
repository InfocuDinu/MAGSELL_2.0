package com.bakerymanager.repository;

import com.bakerymanager.entity.ReceptionNote;
import com.bakerymanager.entity.ReceptionNote.NirStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceptionNoteRepository extends JpaRepository<ReceptionNote, Long> {
    
    Optional<ReceptionNote> findByNirNumber(String nirNumber);
    
    List<ReceptionNote> findByStatus(NirStatus status);
    
    @Query("SELECT rn FROM ReceptionNote rn WHERE rn.invoice.id = :invoiceId")
    List<ReceptionNote> findByInvoiceId(@Param("invoiceId") Long invoiceId);
    
    @Query("SELECT rn FROM ReceptionNote rn WHERE rn.nirDate BETWEEN :startDate AND :endDate ORDER BY rn.nirDate DESC")
    List<ReceptionNote> findByNirDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT rn FROM ReceptionNote rn WHERE rn.hasDiscrepancies = true ORDER BY rn.nirDate DESC")
    List<ReceptionNote> findWithDiscrepancies();
    
    List<ReceptionNote> findAllByOrderByNirDateDesc();
}
