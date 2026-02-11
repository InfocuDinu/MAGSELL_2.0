package com.bakerymanager.service;

import com.bakerymanager.entity.Invoice;
import com.bakerymanager.entity.InvoiceLine;
import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.repository.InvoiceRepository;
import com.bakerymanager.repository.InvoiceLineRepository;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
    
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final IngredientService ingredientService;
    private final XmlMapper xmlMapper;
    
    public InvoiceService(InvoiceRepository invoiceRepository, 
                         InvoiceLineRepository invoiceLineRepository,
                         IngredientService ingredientService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.ingredientService = ingredientService;
        this.xmlMapper = new XmlMapper();
    }
    
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }
    
    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }
    
    public Optional<Invoice> getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }
    
    @Transactional
    public Invoice importUBLInvoice(String filePath) throws IOException {
        try {
            // Validare fișier XML
            File xmlFile = new File(filePath);
            if (!xmlFile.exists() || !xmlFile.canRead()) {
                logger.error("File does not exist or cannot be read: {}", filePath);
                throw new IOException("Fișierul nu există sau nu poate fi citit: " + filePath);
            }
            
            // Generare număr unic pentru factură folosind UUID
            String invoiceNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            // Creare factură (simulare date din XML)
            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber(invoiceNumber);
            invoice.setSupplierName("Furnizor SPV");
            invoice.setInvoiceDate(LocalDateTime.now());
            invoice.setTotalAmount(BigDecimal.ZERO);
            invoice.setIsSpvImported(true);
            
            // Salvare factură
            Invoice savedInvoice = invoiceRepository.save(invoice);
            logger.info("Invoice imported successfully: {}", invoiceNumber);
            
            // Procesare linii factură (simulare)
            List<String> mockIngredients = List.of("Făină", "Zahăr", "Drojdie", "Ouă", "Lapte");
            
            for (String ingredientName : mockIngredients) {
                Ingredient ingredient = findOrCreateIngredient(ingredientName);
                
                InvoiceLine line = new InvoiceLine();
                line.setInvoice(savedInvoice);
                line.setIngredient(ingredient);
                line.setQuantity(BigDecimal.valueOf(Math.random() * 100 + 10).setScale(2, java.math.RoundingMode.HALF_UP));
                line.setUnitPrice(BigDecimal.valueOf(Math.random() * 10 + 1).setScale(2, java.math.RoundingMode.HALF_UP));
                line.setTotalPrice(line.getQuantity().multiply(line.getUnitPrice()));
                
                invoiceLineRepository.save(line);
                
                // Actualizare total factură
                savedInvoice.setTotalAmount(savedInvoice.getTotalAmount().add(line.getTotalPrice()));
            }
            
            // Salvare finală cu total actualizat
            savedInvoice = invoiceRepository.save(savedInvoice);
            
            logger.info("SPV invoice imported successfully: {}", savedInvoice.getInvoiceNumber());
            return savedInvoice;
            
        } catch (Exception e) {
            logger.error("Error importing SPV invoice from file: {}", filePath, e);
            throw new IOException("Eroare la importul facturii SPV: " + e.getMessage(), e);
        }
    }
    
    private Ingredient findOrCreateIngredient(String ingredientName) {
        // Încercare găsire exactă
        List<Ingredient> ingredients = ingredientService.findByName(ingredientName);
        
        if (!ingredients.isEmpty()) {
            return ingredients.get(0);
        }
        
        // Încercare căutare cu LIKE (nume conține)
        ingredients = ingredientService.findByNameContainingIgnoreCase(ingredientName);
        
        if (!ingredients.isEmpty()) {
            logger.info("Ingredient found via LIKE search: {} for search term: {}", 
                ingredients.get(0).getName(), ingredientName);
            return ingredients.get(0);
        }
        
        // Creare automată cu flag de revizie
        Ingredient newIngredient = new Ingredient();
        newIngredient.setName(ingredientName);
        newIngredient.setCurrentStock(BigDecimal.ZERO);
        newIngredient.setMinimumStock(BigDecimal.ZERO);
        newIngredient.setLastPurchasePrice(BigDecimal.ZERO);
        newIngredient.setUnitOfMeasure(Ingredient.UnitOfMeasure.KG);
        newIngredient.setNotes("CREAT AUTOMAT LA IMPORT SPV - NECESITĂ REVIZIE");
        
        try {
            Ingredient saved = ingredientService.saveIngredient(newIngredient);
            logger.warn("Ingredient created automatically (requires review): {}", saved.getName());
            return saved;
        } catch (Exception e) {
            logger.error("Error creating ingredient: {}", ingredientName, e);
            throw new RuntimeException("Nu s-a putut crea ingredientul: " + ingredientName, e);
        }
    }
    
    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }
    
    @Transactional
    public InvoiceLine saveInvoiceLine(InvoiceLine invoiceLine) {
        return invoiceLineRepository.save(invoiceLine);
    }
    
    @Transactional
    public Invoice saveInvoiceWithLines(Invoice invoice, List<InvoiceLine> lines) {
        // Save invoice first
        Invoice savedInvoice = invoiceRepository.save(invoice);
        
        // Save each line
        for (InvoiceLine line : lines) {
            line.setInvoice(savedInvoice);
            line.calculateTotal();
            invoiceLineRepository.save(line);
        }
        
        // Update invoice line count and total
        savedInvoice.setNumberOfLines(lines.size());
        BigDecimal total = lines.stream()
            .map(InvoiceLine::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        savedInvoice.setTotalAmount(total);
        
        return invoiceRepository.save(savedInvoice);
    }
    
    public List<InvoiceLine> getInvoiceLines(Long invoiceId) {
        return invoiceLineRepository.findByInvoiceId(invoiceId);
    }
    
    public Invoice createManualInvoice(String invoiceNumber, String supplierName, 
                                     List<InvoiceLine> invoiceLines) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setSupplierName(supplierName);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setIsSpvImported(false);
        
        Invoice savedInvoice = invoiceRepository.save(invoice);
        
        for (InvoiceLine line : invoiceLines) {
            line.setInvoice(savedInvoice);
            invoiceLineRepository.save(line);
        }
        
        savedInvoice.setInvoiceLines(invoiceLines);
        savedInvoice.calculateTotal();
        
        return invoiceRepository.save(savedInvoice);
    }
    
    public List<Invoice> searchInvoices(String searchTerm) {
        return invoiceRepository.searchInvoices(searchTerm);
    }
    
    public List<Invoice> getSpvInvoices() {
        return invoiceRepository.findByIsSpvImportedTrue();
    }
    
    private void updateIngredientStocks(List<InvoiceLine> invoiceLines) {
        for (InvoiceLine line : invoiceLines) {
            if (line.getIngredient() != null) {
                ingredientService.addStock(line.getIngredient().getId(), line.getQuantity());
                ingredientService.updatePurchasePrice(line.getIngredient().getId(), line.getUnitPrice());
            }
        }
    }
}
