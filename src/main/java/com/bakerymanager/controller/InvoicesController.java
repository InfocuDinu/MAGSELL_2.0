package com.bakerymanager.controller;

import com.bakerymanager.dto.UBLInvoiceDto;
import com.bakerymanager.entity.Invoice;
import com.bakerymanager.service.InvoiceService;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Controller
public class InvoicesController {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoicesController.class);
    
    private final InvoiceService invoiceService;
    
    public InvoicesController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }
    
    @FXML
    private Label totalInvoicesLabel;
    
    @FXML
    private Label spvInvoicesLabel;
    
    @FXML
    private Label totalValueLabel;
    
    @FXML
    private TableView<Invoice> invoicesTable;
    
    @FXML
    private TableColumn<Invoice, String> invoiceNumberColumn;
    
    @FXML
    private TableColumn<Invoice, String> supplierColumn;
    
    @FXML
    private TableColumn<Invoice, String> dateColumn;
    
    @FXML
    private TableColumn<Invoice, BigDecimal> totalColumn;
    
    @FXML
    private TableColumn<Invoice, Boolean> spvColumn;
    
    @FXML
    private TableColumn<Invoice, Void> actionsColumn;
    
    private ObservableList<Invoice> invoices = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        setupTable();
        loadInvoices();
        updateStatistics();
        logger.info("Invoices controller initialized");
    }
    
    private void setupTable() {
        invoiceNumberColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("invoiceNumber"));
        supplierColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("supplierName"));
        dateColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleObjectProperty<>(param.getValue().getInvoiceDate() != null ? 
                param.getValue().getInvoiceDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : ""));
        totalColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("totalAmount"));
        spvColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("isSpvImported"));
        
        invoicesTable.setItems(invoices);
    }
    
    private void loadInvoices() {
        List<Invoice> invoiceList = invoiceService.getAllInvoices();
        invoices.clear();
        invoices.addAll(invoiceList);
    }
    
    private void updateStatistics() {
        totalInvoicesLabel.setText(String.valueOf(invoices.size()));
        
        long spvCount = invoices.stream()
            .mapToLong(invoice -> invoice.getIsSpvImported() ? 1 : 0)
            .sum();
        spvInvoicesLabel.setText(String.valueOf(spvCount));
        
        BigDecimal total = invoices.stream()
            .map(Invoice::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalValueLabel.setText(String.format("%.2f lei", total));
    }
    
    @FXML
    public void importSPVInvoice() {
        try {
            // FileChooser pentru selectare XML
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Importă e-Factură (XML)");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml")
            );
            
            File selectedFile = fileChooser.showOpenDialog(new Stage());
            if (selectedFile == null) {
                return; // Utilizatorul a anulat
            }
            
            // Validate file exists and is readable
            if (!selectedFile.exists() || !selectedFile.canRead()) {
                showError("Fișierul nu există sau nu poate fi citit!");
                logger.error("Selected file does not exist or cannot be read: {}", selectedFile.getPath());
                return;
            }
            
            // Validate file size (prevent DoS attacks with huge files)
            long maxFileSize = 10 * 1024 * 1024; // 10MB
            if (selectedFile.length() > maxFileSize) {
                showError("Fișierul este prea mare (maxim 10MB)!");
                logger.warn("File too large: {} bytes", selectedFile.length());
                return;
            }
            
            // Citim și parsăm XML-ul
            XmlMapper xmlMapper = new XmlMapper();
            UBLInvoiceDto invoiceDto;
            
            try (FileInputStream fis = new FileInputStream(selectedFile)) {
                invoiceDto = xmlMapper.readValue(fis, UBLInvoiceDto.class);
            }
            
            // Convertim DTO-ul în entitatea Invoice
            Invoice invoice = convertDtoToInvoice(invoiceDto, selectedFile.getName());
            
            // Salvăm în baza de date
            invoiceService.saveInvoice(invoice);
            
            // Actualizăm interfața
            loadInvoices();
            updateStatistics();
            
            showSuccessMessage("e-Factura importată cu succes!\n" +
                "Număr: " + invoice.getInvoiceNumber() + "\n" +
                "Furnizor: " + invoice.getSupplierName() + "\n" +
                "Valoare: " + String.format("%.2f lei", invoice.getTotalAmount()));
            logger.info("SPV invoice imported successfully: {}", invoice.getInvoiceNumber());
            
        } catch (IOException e) {
            logger.error("IO error importing SPV invoice", e);
            showError("Eroare la citirea fișierului: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error importing SPV invoice", e);
            showError("Eroare la importarea e-facturii: " + e.getMessage());
        }
    }
    
    private Invoice convertDtoToInvoice(UBLInvoiceDto dto, String fileName) {
        Invoice invoice = new Invoice();
        
        // Număr factură - use UUID for uniqueness
        if (dto.getInvoiceNumber() != null && !dto.getInvoiceNumber().trim().isEmpty()) {
            invoice.setInvoiceNumber(dto.getInvoiceNumber());
        } else {
            String uniqueId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            invoice.setInvoiceNumber("IMP_" + uniqueId);
            logger.warn("Invoice number missing in XML, generated: IMP_{}", uniqueId);
        }
        
        // Data facturii
        if (dto.getIssueDate() != null) {
            try {
                LocalDate invoiceDate = LocalDate.parse(dto.getIssueDate());
                invoice.setInvoiceDate(invoiceDate.atStartOfDay());
            } catch (Exception e) {
                invoice.setInvoiceDate(LocalDateTime.now());
            }
        } else {
            invoice.setInvoiceDate(LocalDateTime.now());
        }
        
        // Furnizor
        if (dto.getSupplierParty() != null && 
            dto.getSupplierParty().getParty() != null) {
            
            StringBuilder supplierName = new StringBuilder();
            
            // Numele furnizorului
            if (dto.getSupplierParty().getParty().getPartyNames() != null) {
                for (UBLInvoiceDto.PartyName partyName : dto.getSupplierParty().getParty().getPartyNames()) {
                    if (partyName.getName() != null) {
                        if (supplierName.length() > 0) {
                            supplierName.append(" ");
                        }
                        supplierName.append(partyName.getName());
                    }
                }
            }
            
            // CUI-ul
            if (dto.getSupplierParty().getParty().getLegalEntity() != null &&
                dto.getSupplierParty().getParty().getLegalEntity().getCompanyID() != null) {
                
                if (supplierName.length() > 0) {
                    supplierName.append(" (CUI: ");
                } else {
                    supplierName.append("CUI: ");
                }
                supplierName.append(dto.getSupplierParty().getParty().getLegalEntity().getCompanyID());
            }
            
            invoice.setSupplierName(supplierName.toString());
        } else {
            invoice.setSupplierName("Furnizor necunoscut");
        }
        
        // Valoarea totală
        if (dto.getMonetaryTotal() != null && 
            dto.getMonetaryTotal().getTaxInclusiveAmount() != null &&
            dto.getMonetaryTotal().getTaxInclusiveAmount().getValue() != null) {
            
            invoice.setTotalAmount(dto.getMonetaryTotal().getTaxInclusiveAmount().getValue());
        } else {
            invoice.setTotalAmount(BigDecimal.ZERO);
        }
        
        // Moneda
        if (dto.getCurrency() != null) {
            invoice.setCurrency(dto.getCurrency());
        } else {
            invoice.setCurrency("RON");
        }
        
        // Marchăm ca import SPV
        invoice.setIsSpvImported(true);
        
        // Data importului
        invoice.setImportDate(LocalDateTime.now());
        
        // Numele fișierului sursă
        invoice.setSourceFileName(fileName);
        
        // Status
        invoice.setStatus("Importată");
        
        // Număr linii din factură
        if (dto.getInvoiceLines() != null) {
            invoice.setNumberOfLines(dto.getInvoiceLines().size());
        } else {
            invoice.setNumberOfLines(0);
        }
        
        return invoice;
    }
    
    @FXML
    public void createManualInvoice() {
        showSuccessMessage("Creare factură manuală - funcționalitate în dezvoltare");
    }
    
    private void showSuccessMessage(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
    
    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Eroare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
