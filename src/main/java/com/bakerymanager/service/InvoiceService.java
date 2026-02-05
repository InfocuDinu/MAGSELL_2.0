package com.bakerymanager.service;

import com.bakerymanager.entity.Invoice;
import com.bakerymanager.entity.InvoiceLine;
import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.repository.InvoiceRepository;
import com.bakerymanager.repository.InvoiceLineRepository;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InvoiceService {
    
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
            // Citire fișier XML (simulare - în realitate ar fi parsare UBL)
            File xmlFile = new File(filePath);
            if (!xmlFile.exists()) {
                throw new IOException("Fișierul nu există: " + filePath);
            }
            
            // Creare factură (simulare date din XML)
            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
            invoice.setSupplierName("Furnizor SPV");
            invoice.setInvoiceDate(LocalDateTime.now());
            invoice.setTotalAmount(BigDecimal.ZERO);
            invoice.setIsSpvImported(true);
            
            // Salvare factură
            Invoice savedInvoice = invoiceRepository.save(invoice);
            
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
            
            System.out.println("Factură SPV importată cu succes: " + savedInvoice.getInvoiceNumber());
            return savedInvoice;
            
        } catch (Exception e) {
            System.err.println("Eroare la importul facturii SPV: " + e.getMessage());
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
            System.out.println("Ingredient găsit prin LIKE: " + ingredients.get(0).getName() + " pentru căutare: " + ingredientName);
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
            System.out.println("Ingredient creat automat: " + saved.getName() + " (necesită revizie)");
            return saved;
        } catch (Exception e) {
            System.err.println("Eroare la crearea ingredientului: " + e.getMessage());
            throw new RuntimeException("Nu s-a putut crea ingredientul: " + ingredientName, e);
        }
    }
    
    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
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
