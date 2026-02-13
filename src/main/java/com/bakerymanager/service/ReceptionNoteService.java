package com.bakerymanager.service;

import com.bakerymanager.entity.*;
import com.bakerymanager.repository.ReceptionNoteRepository;
import com.bakerymanager.repository.ReceptionNoteLineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReceptionNoteService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReceptionNoteService.class);
    
    private final ReceptionNoteRepository receptionNoteRepository;
    private final ReceptionNoteLineRepository receptionNoteLineRepository;
    private final InvoiceService invoiceService;
    
    public ReceptionNoteService(ReceptionNoteRepository receptionNoteRepository,
                               ReceptionNoteLineRepository receptionNoteLineRepository,
                               InvoiceService invoiceService) {
        this.receptionNoteRepository = receptionNoteRepository;
        this.receptionNoteLineRepository = receptionNoteLineRepository;
        this.invoiceService = invoiceService;
    }
    
    /**
     * Create a Reception Note (NIR) from an Invoice
     */
    @Transactional
    public ReceptionNote createFromInvoice(Long invoiceId, String companyName, String companyAddress) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId)
            .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        
        ReceptionNote nirNote = new ReceptionNote();
        nirNote.setInvoice(invoice);
        nirNote.setCompanyName(companyName != null ? companyName : "MAGSELL 2.0 - BakeryManager Pro");
        nirNote.setCompanyAddress(companyAddress);
        nirNote.setNirDate(LocalDateTime.now());
        nirNote.setReceptionDate(LocalDateTime.now());
        nirNote.setStatus(ReceptionNote.NirStatus.DRAFT);
        nirNote.generateNirNumber();
        
        // Create lines from invoice lines
        List<InvoiceLine> invoiceLines = invoiceService.getInvoiceLines(invoiceId);
        for (InvoiceLine invoiceLine : invoiceLines) {
            ReceptionNoteLine nirLine = new ReceptionNoteLine();
            nirLine.setProductName(invoiceLine.getIngredient().getName());
            nirLine.setProductCode(null); // Can be set later
            nirLine.setUnit(invoiceLine.getIngredient().getUnitOfMeasure().name());
            nirLine.setInvoicedQuantity(invoiceLine.getQuantity());
            nirLine.setReceivedQuantity(invoiceLine.getQuantity()); // Default: same as invoiced
            nirLine.setUnitPrice(invoiceLine.getUnitPrice());
            nirLine.setVatRate(BigDecimal.valueOf(19)); // Default VAT rate
            nirLine.calculateValues();
            nirLine.calculateDifference();
            
            nirNote.addLine(nirLine);
        }
        
        nirNote.calculateTotals();
        nirNote.checkDiscrepancies();
        
        ReceptionNote saved = receptionNoteRepository.save(nirNote);
        logger.info("Created reception note {} from invoice {}", saved.getNirNumber(), invoice.getInvoiceNumber());
        
        return saved;
    }
    
    /**
     * Save or update a Reception Note
     */
    @Transactional
    public ReceptionNote saveReceptionNote(ReceptionNote receptionNote) {
        receptionNote.calculateTotals();
        receptionNote.checkDiscrepancies();
        ReceptionNote saved = receptionNoteRepository.save(receptionNote);
        logger.info("Saved reception note: {}", saved.getNirNumber());
        return saved;
    }
    
    /**
     * Get reception note by ID
     */
    public ReceptionNote getReceptionNoteById(Long id) {
        return receptionNoteRepository.findById(id).orElse(null);
    }
    
    /**
     * Get reception note by NIR number
     */
    public ReceptionNote getReceptionNoteByNumber(String nirNumber) {
        return receptionNoteRepository.findByNirNumber(nirNumber).orElse(null);
    }
    
    /**
     * Get all reception notes
     */
    public List<ReceptionNote> getAllReceptionNotes() {
        return receptionNoteRepository.findAllByOrderByNirDateDesc();
    }
    
    /**
     * Get reception notes by status
     */
    public List<ReceptionNote> getReceptionNotesByStatus(ReceptionNote.NirStatus status) {
        return receptionNoteRepository.findByStatus(status);
    }
    
    /**
     * Get reception notes by invoice
     */
    public List<ReceptionNote> getReceptionNotesByInvoice(Long invoiceId) {
        return receptionNoteRepository.findByInvoiceId(invoiceId);
    }
    
    /**
     * Get reception notes with discrepancies
     */
    public List<ReceptionNote> getReceptionNotesWithDiscrepancies() {
        return receptionNoteRepository.findWithDiscrepancies();
    }
    
    /**
     * Get reception notes by date range
     */
    public List<ReceptionNote> getReceptionNotesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return receptionNoteRepository.findByNirDateBetween(startDate, endDate);
    }
    
    /**
     * Approve a reception note
     */
    @Transactional
    public ReceptionNote approveReceptionNote(Long id, String committee1, String committee2, String committee3) {
        ReceptionNote nirNote = getReceptionNoteById(id);
        if (nirNote == null) {
            throw new IllegalArgumentException("Reception note not found: " + id);
        }
        
        if (!nirNote.canApprove()) {
            throw new IllegalStateException("Reception note cannot be approved in current state");
        }
        
        nirNote.setCommittee1Name(committee1);
        nirNote.setCommittee2Name(committee2);
        nirNote.setCommittee3Name(committee3);
        nirNote.setStatus(ReceptionNote.NirStatus.APPROVED);
        
        ReceptionNote saved = receptionNoteRepository.save(nirNote);
        logger.info("Approved reception note: {}", saved.getNirNumber());
        
        return saved;
    }
    
    /**
     * Sign a reception note (final step)
     */
    @Transactional
    public ReceptionNote signReceptionNote(Long id, String warehouseManager) {
        ReceptionNote nirNote = getReceptionNoteById(id);
        if (nirNote == null) {
            throw new IllegalArgumentException("Reception note not found: " + id);
        }
        
        if (!nirNote.canSign()) {
            throw new IllegalStateException("Reception note cannot be signed in current state");
        }
        
        nirNote.setWarehouseManagerName(warehouseManager);
        nirNote.setStatus(ReceptionNote.NirStatus.SIGNED);
        
        ReceptionNote saved = receptionNoteRepository.save(nirNote);
        logger.info("Signed reception note: {}", saved.getNirNumber());
        
        return saved;
    }
    
    /**
     * Update received quantities for a reception note
     */
    @Transactional
    public ReceptionNote updateReceivedQuantities(Long id, List<ReceptionNoteLine> updatedLines) {
        ReceptionNote nirNote = getReceptionNoteById(id);
        if (nirNote == null) {
            throw new IllegalArgumentException("Reception note not found: " + id);
        }
        
        for (ReceptionNoteLine updatedLine : updatedLines) {
            Optional<ReceptionNoteLine> existingLine = nirNote.getLines().stream()
                .filter(l -> l.getId().equals(updatedLine.getId()))
                .findFirst();
            
            if (existingLine.isPresent()) {
                ReceptionNoteLine line = existingLine.get();
                line.setReceivedQuantity(updatedLine.getReceivedQuantity());
                line.setDiscrepancyNotes(updatedLine.getDiscrepancyNotes());
                line.calculateValues();
                line.calculateDifference();
            }
        }
        
        nirNote.calculateTotals();
        nirNote.checkDiscrepancies();
        
        ReceptionNote saved = receptionNoteRepository.save(nirNote);
        logger.info("Updated quantities for reception note: {}", saved.getNirNumber());
        
        return saved;
    }
    
    /**
     * Delete a reception note (only if DRAFT)
     */
    @Transactional
    public void deleteReceptionNote(Long id) {
        ReceptionNote nirNote = getReceptionNoteById(id);
        if (nirNote == null) {
            throw new IllegalArgumentException("Reception note not found: " + id);
        }
        
        if (nirNote.getStatus() != ReceptionNote.NirStatus.DRAFT) {
            throw new IllegalStateException("Can only delete draft reception notes");
        }
        
        receptionNoteRepository.delete(nirNote);
        logger.info("Deleted reception note: {}", nirNote.getNirNumber());
    }
}
