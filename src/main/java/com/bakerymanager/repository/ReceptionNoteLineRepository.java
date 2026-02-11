package com.bakerymanager.repository;

import com.bakerymanager.entity.ReceptionNoteLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceptionNoteLineRepository extends JpaRepository<ReceptionNoteLine, Long> {
    
    @Query("SELECT rnl FROM ReceptionNoteLine rnl WHERE rnl.receptionNote.id = :receptionNoteId")
    List<ReceptionNoteLine> findByReceptionNoteId(@Param("receptionNoteId") Long receptionNoteId);
    
    @Query("SELECT rnl FROM ReceptionNoteLine rnl WHERE rnl.hasDiscrepancy = true")
    List<ReceptionNoteLine> findWithDiscrepancies();
}
