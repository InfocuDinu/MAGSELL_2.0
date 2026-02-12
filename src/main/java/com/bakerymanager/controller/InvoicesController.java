package com.bakerymanager.controller;

import com.bakerymanager.dto.UBLInvoiceDto;
import com.bakerymanager.entity.Invoice;
import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.ReceptionNote;
import com.bakerymanager.entity.ReceptionNoteLine;
import com.bakerymanager.service.InvoiceService;
import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.ReceptionNoteService;
import com.bakerymanager.service.PdfService;
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
    private final IngredientService ingredientService;
    private final ReceptionNoteService receptionNoteService;
    private final PdfService pdfService;
    
    public InvoicesController(InvoiceService invoiceService, IngredientService ingredientService,
                             ReceptionNoteService receptionNoteService, PdfService pdfService) {
        this.invoiceService = invoiceService;
        this.ingredientService = ingredientService;
        this.receptionNoteService = receptionNoteService;
        this.pdfService = pdfService;
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
    
    // NIR Table
    @FXML
    private TableView<ReceptionNote> nirTable;
    
    @FXML
    private TableColumn<ReceptionNote, String> nirNumberColumn;
    
    @FXML
    private TableColumn<ReceptionNote, String> nirInvoiceColumn;
    
    @FXML
    private TableColumn<ReceptionNote, String> nirDateColumn;
    
    @FXML
    private TableColumn<ReceptionNote, String> nirStatusColumn;
    
    @FXML
    private TableColumn<ReceptionNote, String> nirSupplierColumn;
    
    @FXML
    private TableColumn<ReceptionNote, BigDecimal> nirTotalColumn;
    
    @FXML
    private TableColumn<ReceptionNote, Void> nirActionsColumn;
    
    private ObservableList<Invoice> invoices = FXCollections.observableArrayList();
    private ObservableList<ReceptionNote> receptionNotes = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        setupTable();
        setupNIRTable();
        loadInvoices();
        loadReceptionNotes();
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
        showManualInvoiceDialog();
    }
    
    private void showManualInvoiceDialog() {
        try {
            // Create stage for comprehensive dialog
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Creare Factură Manuală");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setMinWidth(800);
            dialogStage.setMinHeight(600);
            
            // Main container
            javafx.scene.layout.BorderPane mainPane = new javafx.scene.layout.BorderPane();
            mainPane.setPadding(new javafx.geometry.Insets(15));
            
            // Header section
            javafx.scene.layout.VBox headerBox = new javafx.scene.layout.VBox(10);
            javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("Creare Factură Manuală");
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            headerBox.getChildren().add(titleLabel);
            
            // Invoice header form
            javafx.scene.layout.GridPane headerGrid = new javafx.scene.layout.GridPane();
            headerGrid.setHgap(10);
            headerGrid.setVgap(10);
            headerGrid.setPadding(new javafx.geometry.Insets(10));
            
            javafx.scene.control.TextField invoiceNumberField = new javafx.scene.control.TextField();
            invoiceNumberField.setPromptText("Ex: FAC-2026-001");
            
            javafx.scene.control.TextField supplierNameField = new javafx.scene.control.TextField();
            supplierNameField.setPromptText("Nume furnizor");
            
            javafx.scene.control.TextField supplierCuiField = new javafx.scene.control.TextField();
            supplierCuiField.setPromptText("CUI furnizor (opțional)");
            
            javafx.scene.control.DatePicker invoiceDatePicker = new javafx.scene.control.DatePicker();
            invoiceDatePicker.setValue(LocalDate.now());
            
            javafx.scene.control.ComboBox<String> currencyCombo = new javafx.scene.control.ComboBox<>();
            currencyCombo.getItems().addAll("RON", "EUR", "USD");
            currencyCombo.setValue("RON");
            
            headerGrid.add(new javafx.scene.control.Label("Număr Factură:"), 0, 0);
            headerGrid.add(invoiceNumberField, 1, 0);
            headerGrid.add(new javafx.scene.control.Label("Furnizor:"), 0, 1);
            headerGrid.add(supplierNameField, 1, 1);
            headerGrid.add(new javafx.scene.control.Label("CUI Furnizor:"), 0, 2);
            headerGrid.add(supplierCuiField, 1, 2);
            headerGrid.add(new javafx.scene.control.Label("Data:"), 0, 3);
            headerGrid.add(invoiceDatePicker, 1, 3);
            headerGrid.add(new javafx.scene.control.Label("Monedă:"), 0, 4);
            headerGrid.add(currencyCombo, 1, 4);
            
            headerBox.getChildren().add(headerGrid);
            mainPane.setTop(headerBox);
            
            // Invoice lines section
            javafx.scene.layout.VBox linesBox = new javafx.scene.layout.VBox(10);
            linesBox.setPadding(new javafx.geometry.Insets(10));
            
            javafx.scene.control.Label linesLabel = new javafx.scene.control.Label("Linii Factură (Produse):");
            linesLabel.setStyle("-fx-font-weight: bold;");
            
            // Table for invoice lines
            javafx.scene.control.TableView<com.bakerymanager.entity.InvoiceLine> linesTable = new javafx.scene.control.TableView<>();
            ObservableList<com.bakerymanager.entity.InvoiceLine> invoiceLines = FXCollections.observableArrayList();
            linesTable.setItems(invoiceLines);
            linesTable.setPrefHeight(200);
            
            javafx.scene.control.TableColumn<com.bakerymanager.entity.InvoiceLine, String> productCol = new javafx.scene.control.TableColumn<>("Produs");
            productCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("productName"));
            productCol.setPrefWidth(200);
            
            javafx.scene.control.TableColumn<com.bakerymanager.entity.InvoiceLine, BigDecimal> quantityCol = new javafx.scene.control.TableColumn<>("Cantitate");
            quantityCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
            quantityCol.setPrefWidth(100);
            
            javafx.scene.control.TableColumn<com.bakerymanager.entity.InvoiceLine, BigDecimal> priceCol = new javafx.scene.control.TableColumn<>("Preț Unitar");
            priceCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("unitPrice"));
            priceCol.setPrefWidth(100);
            
            javafx.scene.control.TableColumn<com.bakerymanager.entity.InvoiceLine, BigDecimal> totalCol = new javafx.scene.control.TableColumn<>("Total");
            totalCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("totalPrice"));
            totalCol.setPrefWidth(100);
            
            javafx.scene.control.TableColumn<com.bakerymanager.entity.InvoiceLine, String> typeCol = new javafx.scene.control.TableColumn<>("Tip");
            typeCol.setCellValueFactory(cellData -> {
                com.bakerymanager.entity.InvoiceLine.ProductType type = cellData.getValue().getProductType();
                return new javafx.beans.property.SimpleStringProperty(type != null ? type.getDisplayName() : "");
            });
            typeCol.setPrefWidth(150);
            
            linesTable.getColumns().addAll(productCol, quantityCol, priceCol, totalCol, typeCol);
            
            // Add line button
            javafx.scene.control.Button addLineButton = new javafx.scene.control.Button("➕ Adaugă Produs");
            addLineButton.setOnAction(e -> showAddLineDialog(invoiceLines, linesTable));
            
            // Remove line button
            javafx.scene.control.Button removeLineButton = new javafx.scene.control.Button("❌ Șterge Produs");
            removeLineButton.setDisable(true);
            linesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                removeLineButton.setDisable(newVal == null);
            });
            removeLineButton.setOnAction(e -> {
                com.bakerymanager.entity.InvoiceLine selected = linesTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    invoiceLines.remove(selected);
                }
            });
            
            javafx.scene.layout.HBox lineButtonBox = new javafx.scene.layout.HBox(10);
            lineButtonBox.getChildren().addAll(addLineButton, removeLineButton);
            
            linesBox.getChildren().addAll(linesLabel, linesTable, lineButtonBox);
            mainPane.setCenter(linesBox);
            
            // Total calculation label
            javafx.scene.control.Label totalLabel = new javafx.scene.control.Label("Total: 0.00 RON");
            totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            // Update total when lines change
            invoiceLines.addListener((javafx.collections.ListChangeListener.Change<? extends com.bakerymanager.entity.InvoiceLine> c) -> {
                BigDecimal total = invoiceLines.stream()
                    .map(com.bakerymanager.entity.InvoiceLine::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalLabel.setText(String.format("Total: %.2f %s", total, currencyCombo.getValue()));
            });
            
            // Bottom buttons
            javafx.scene.layout.HBox bottomBox = new javafx.scene.layout.HBox(10);
            bottomBox.setPadding(new javafx.geometry.Insets(10));
            bottomBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            bottomBox.getChildren().add(totalLabel);
            
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            bottomBox.getChildren().add(spacer);
            
            javafx.scene.control.Button saveButton = new javafx.scene.control.Button("💾 Salvează Factură");
            saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
            saveButton.setDisable(true);
            
            javafx.scene.control.Button cancelButton = new javafx.scene.control.Button("✖ Anulează");
            
            // Enable save button when required fields are filled
            javafx.beans.binding.BooleanBinding formValid = invoiceNumberField.textProperty().isEmpty()
                .or(supplierNameField.textProperty().isEmpty());
            saveButton.disableProperty().bind(formValid);
            
            saveButton.setOnAction(e -> {
                try {
                    if (invoiceLines.isEmpty()) {
                        showError("Adăugați cel puțin un produs pe factură!");
                        return;
                    }
                    
                    // Create invoice
                    Invoice invoice = new Invoice();
                    invoice.setInvoiceNumber(invoiceNumberField.getText().trim());
                    invoice.setSupplierName(supplierNameField.getText().trim());
                    invoice.setSupplierCui(supplierCuiField.getText().trim());
                    invoice.setInvoiceDate(invoiceDatePicker.getValue().atStartOfDay());
                    invoice.setCurrency(currencyCombo.getValue());
                    invoice.setIsSpvImported(false);
                    invoice.setStatus("MANUAL");
                    invoice.setImportDate(LocalDateTime.now());
                    
                    // Save invoice with lines
                    Invoice savedInvoice = invoiceService.saveInvoiceWithLines(invoice, invoiceLines);
                    
                    // Reload invoices
                    loadInvoices();
                    updateStatistics();
                    
                    dialogStage.close();
                    
                    showSuccessMessage(String.format(
                        "Factura a fost creată cu succes!\n\n" +
                        "Număr: %s\n" +
                        "Furnizor: %s\n" +
                        "Produse: %d\n" +
                        "Valoare totală: %.2f %s",
                        savedInvoice.getInvoiceNumber(),
                        savedInvoice.getSupplierName(),
                        savedInvoice.getNumberOfLines(),
                        savedInvoice.getTotalAmount(),
                        savedInvoice.getCurrency()
                    ));
                    
                    logger.info("Manual invoice created: {} with {} lines", 
                        savedInvoice.getInvoiceNumber(), savedInvoice.getNumberOfLines());
                    
                } catch (Exception ex) {
                    logger.error("Error saving manual invoice", ex);
                    showError("Eroare la salvarea facturii: " + ex.getMessage());
                }
            });
            
            cancelButton.setOnAction(e -> dialogStage.close());
            
            bottomBox.getChildren().addAll(saveButton, cancelButton);
            mainPane.setBottom(bottomBox);
            
            // Show dialog
            javafx.scene.Scene scene = new javafx.scene.Scene(mainPane);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            
        } catch (Exception e) {
            logger.error("Error creating manual invoice dialog", e);
            showError("Eroare la crearea facturii manuale: " + e.getMessage());
        }
    }
    
    private void showAddLineDialog(ObservableList<com.bakerymanager.entity.InvoiceLine> invoiceLines,
                                     javafx.scene.control.TableView<com.bakerymanager.entity.InvoiceLine> table) {
        javafx.scene.control.Dialog<com.bakerymanager.entity.InvoiceLine> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Adaugă Produs");
        dialog.setHeaderText("Introduceți detaliile produsului");
        
        javafx.scene.control.ButtonType addButtonType = new javafx.scene.control.ButtonType("Adaugă", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, javafx.scene.control.ButtonType.CANCEL);
        
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        javafx.scene.control.TextField productNameField = new javafx.scene.control.TextField();
        productNameField.setPromptText("Nume produs");
        
        javafx.scene.control.TextField quantityField = new javafx.scene.control.TextField();
        quantityField.setPromptText("Cantitate");
        
        javafx.scene.control.TextField priceField = new javafx.scene.control.TextField();
        priceField.setPromptText("Preț unitar");
        
        javafx.scene.control.ComboBox<com.bakerymanager.entity.InvoiceLine.ProductType> typeCombo = new javafx.scene.control.ComboBox<>();
        typeCombo.getItems().addAll(com.bakerymanager.entity.InvoiceLine.ProductType.values());
        typeCombo.setValue(com.bakerymanager.entity.InvoiceLine.ProductType.MATERIE_PRIMA);
        typeCombo.setConverter(new javafx.util.StringConverter<com.bakerymanager.entity.InvoiceLine.ProductType>() {
            @Override
            public String toString(com.bakerymanager.entity.InvoiceLine.ProductType type) {
                return type != null ? type.getDisplayName() : "";
            }
            
            @Override
            public com.bakerymanager.entity.InvoiceLine.ProductType fromString(String string) {
                return typeCombo.getItems().stream()
                    .filter(type -> type.getDisplayName().equals(string))
                    .findFirst()
                    .orElse(null);
            }
        });
        
        grid.add(new javafx.scene.control.Label("Nume Produs:"), 0, 0);
        grid.add(productNameField, 1, 0);
        grid.add(new javafx.scene.control.Label("Cantitate:"), 0, 1);
        grid.add(quantityField, 1, 1);
        grid.add(new javafx.scene.control.Label("Preț Unitar:"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new javafx.scene.control.Label("Tip Produs:"), 0, 3);
        grid.add(typeCombo, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Validation
        javafx.scene.Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        
        productNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            addButton.setDisable(newValue.trim().isEmpty() || 
                                quantityField.getText().trim().isEmpty() ||
                                priceField.getText().trim().isEmpty());
        });
        
        quantityField.textProperty().addListener((observable, oldValue, newValue) -> {
            addButton.setDisable(newValue.trim().isEmpty() || 
                                productNameField.getText().trim().isEmpty() ||
                                priceField.getText().trim().isEmpty());
        });
        
        priceField.textProperty().addListener((observable, oldValue, newValue) -> {
            addButton.setDisable(newValue.trim().isEmpty() || 
                                productNameField.getText().trim().isEmpty() ||
                                quantityField.getText().trim().isEmpty());
        });
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    com.bakerymanager.entity.InvoiceLine line = new com.bakerymanager.entity.InvoiceLine();
                    line.setProductName(productNameField.getText().trim());
                    line.setQuantity(new BigDecimal(quantityField.getText().trim()));
                    line.setUnitPrice(new BigDecimal(priceField.getText().trim()));
                    line.setProductType(typeCombo.getValue());
                    line.calculateTotal();
                    
                    // Find or create ingredient for this product - MUST be saved to database first
                    Ingredient ingredient = findOrCreateIngredient(line.getProductName(), line.getUnitPrice(), typeCombo.getValue());
                    line.setIngredient(ingredient);
                    
                    return line;
                } catch (NumberFormatException e) {
                    showError("Cantitatea și prețul trebuie să fie numere valide!");
                    return null;
                } catch (Exception e) {
                    logger.error("Error creating invoice line", e);
                    showError("Eroare la crearea liniei de factură: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
        
        java.util.Optional<com.bakerymanager.entity.InvoiceLine> result = dialog.showAndWait();
        result.ifPresent(line -> {
            invoiceLines.add(line);
            table.refresh();
        });
    }
    
    /**
     * Find existing ingredient by name or create a new one and save it to database.
     * This ensures the ingredient is persisted before being referenced by InvoiceLine.
     * 
     * @param ingredientName Name of the ingredient
     * @param purchasePrice Purchase price from invoice
     * @param productType Type of product (MATERIE_PRIMA or MARFA)
     * @return Saved (persisted) Ingredient entity
     */
    private Ingredient findOrCreateIngredient(String ingredientName, BigDecimal purchasePrice, 
                                             com.bakerymanager.entity.InvoiceLine.ProductType productType) {
        // Try to find existing ingredient by exact name
        List<Ingredient> ingredients = ingredientService.findByName(ingredientName);
        
        if (!ingredients.isEmpty()) {
            logger.info("Found existing ingredient: {}", ingredientName);
            return ingredients.get(0);
        }
        
        // Try to find by name containing (case-insensitive)
        ingredients = ingredientService.findByNameContainingIgnoreCase(ingredientName);
        
        if (!ingredients.isEmpty()) {
            logger.info("Found ingredient via partial match: {} for search term: {}", 
                ingredients.get(0).getName(), ingredientName);
            return ingredients.get(0);
        }
        
        // Create new ingredient and save to database
        Ingredient newIngredient = new Ingredient();
        newIngredient.setName(ingredientName);
        newIngredient.setCurrentStock(BigDecimal.ZERO);
        newIngredient.setMinimumStock(BigDecimal.ZERO);
        newIngredient.setLastPurchasePrice(purchasePrice);
        newIngredient.setUnitOfMeasure(Ingredient.UnitOfMeasure.BUC);
        
        // Map InvoiceLine.ProductType to Ingredient.ProductType
        if (productType == com.bakerymanager.entity.InvoiceLine.ProductType.MATERIE_PRIMA) {
            newIngredient.setProductType(Ingredient.ProductType.MATERIE_PRIMA);
        } else {
            newIngredient.setProductType(Ingredient.ProductType.MARFA);
        }
        
        newIngredient.setNotes("Creat automat la introducere factură manuală");
        
        try {
            Ingredient savedIngredient = ingredientService.saveIngredient(newIngredient);
            logger.info("Created new ingredient: {} (ID: {})", savedIngredient.getName(), savedIngredient.getId());
            return savedIngredient;
        } catch (Exception e) {
            logger.error("Error creating ingredient: {}", ingredientName, e);
            throw new RuntimeException("Nu s-a putut crea produsul: " + ingredientName + ". " + e.getMessage(), e);
        }
    }
    
    // NIR Management Methods
    
    @FXML
    public void generateNIR() {
        try {
            Invoice selectedInvoice = invoicesTable.getSelectionModel().getSelectedItem();
            
            if (selectedInvoice == null) {
                showError("Vă rugăm să selectați o factură din tabel.");
                return;
            }
            
            // Create NIR from invoice
            ReceptionNote nir = receptionNoteService.createFromInvoice(
                selectedInvoice.getId(),
                "MAGSELL 2.0 - BakeryManager Pro",
                "Str. Exemplu Nr. 1, București"
            );
            
            showSuccessMessage("NIR generat cu succes!\nNumăr NIR: " + nir.getNirNumber());
            logger.info("NIR generated: {} for invoice: {}", nir.getNirNumber(), selectedInvoice.getInvoiceNumber());
            
            // Refresh NIR list
            loadReceptionNotes();
            
        } catch (Exception e) {
            logger.error("Error generating NIR", e);
            showError("Eroare la generarea NIR: " + e.getMessage());
        }
    }
    
    @FXML
    public void loadReceptionNotes() {
        try {
            List<ReceptionNote> notes = receptionNoteService.getAllReceptionNotes();
            receptionNotes.clear();
            receptionNotes.addAll(notes);
            
            if (nirTable != null) {
                nirTable.setItems(receptionNotes);
            }
            
            logger.info("Loaded {} reception notes", notes.size());
        } catch (Exception e) {
            logger.error("Error loading reception notes", e);
            showError("Eroare la încărcarea NIR-urilor: " + e.getMessage());
        }
    }
    
    private void setupNIRTable() {
        if (nirTable == null) {
            return;
        }
        
        // Setup columns
        nirNumberColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nirNumber"));
        nirInvoiceColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleObjectProperty<>(
                param.getValue().getInvoice() != null ? param.getValue().getInvoice().getInvoiceNumber() : ""
            )
        );
        nirDateColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleObjectProperty<>(
                param.getValue().getNirDate() != null ? 
                param.getValue().getNirDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : ""
            )
        );
        nirStatusColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleObjectProperty<>(
                param.getValue().getStatus() != null ? param.getValue().getStatus().name() : ""
            )
        );
        nirSupplierColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleObjectProperty<>(
                param.getValue().getInvoice() != null ? param.getValue().getInvoice().getSupplierName() : ""
            )
        );
        nirTotalColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("totalValue"));
        
        // Setup action buttons column
        nirActionsColumn.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
            private final javafx.scene.control.Button editBtn = new javafx.scene.control.Button("✏️ Edit");
            private final javafx.scene.control.Button pdfBtn = new javafx.scene.control.Button("📄 PDF");
            private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(5, editBtn, pdfBtn);
            
            {
                editBtn.setOnAction(event -> {
                    ReceptionNote nir = getTableView().getItems().get(getIndex());
                    editReceptionNote(nir);
                });
                
                pdfBtn.setOnAction(event -> {
                    ReceptionNote nir = getTableView().getItems().get(getIndex());
                    exportReceptionNotePdf(nir);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        
        nirTable.setItems(receptionNotes);
    }
    
    private void editReceptionNote(ReceptionNote nir) {
        try {
            // Main dialog
            javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Editare NIR - " + nir.getNirNumber());
            dialog.setHeaderText("Editare Notă de Intrare Recepție");
            
            // Create TabPane for organized sections
            javafx.scene.control.TabPane tabPane = new javafx.scene.control.TabPane();
            tabPane.setTabClosingPolicy(javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE);
            
            // ============ TAB 1: NIR Header Information ============
            javafx.scene.control.Tab headerTab = new javafx.scene.control.Tab("Date NIR");
            javafx.scene.layout.GridPane headerGrid = new javafx.scene.layout.GridPane();
            headerGrid.setHgap(10);
            headerGrid.setVgap(10);
            headerGrid.setPadding(new javafx.geometry.Insets(20, 20, 20, 20));
            
            // NIR Number (read-only)
            javafx.scene.control.TextField nirNumberField = new javafx.scene.control.TextField(nir.getNirNumber());
            nirNumberField.setEditable(false);
            nirNumberField.setStyle("-fx-background-color: #f5f5f5;");
            
            // Invoice Number (read-only, from linked invoice)
            javafx.scene.control.TextField invoiceField = new javafx.scene.control.TextField(
                nir.getInvoice() != null ? nir.getInvoice().getInvoiceNumber() : ""
            );
            invoiceField.setEditable(false);
            invoiceField.setStyle("-fx-background-color: #f5f5f5;");
            
            // NIR Date
            javafx.scene.control.DatePicker nirDatePicker = new javafx.scene.control.DatePicker(
                nir.getNirDate() != null ? nir.getNirDate().toLocalDate() : LocalDate.now()
            );
            
            // Status
            javafx.scene.control.ComboBox<ReceptionNote.NirStatus> statusCombo = new javafx.scene.control.ComboBox<>();
            statusCombo.getItems().addAll(ReceptionNote.NirStatus.values());
            statusCombo.setValue(nir.getStatus());
            
            // Company Name
            javafx.scene.control.TextField companyNameField = new javafx.scene.control.TextField(
                nir.getCompanyName() != null ? nir.getCompanyName() : ""
            );
            
            // Company Address
            javafx.scene.control.TextField companyAddressField = new javafx.scene.control.TextField(
                nir.getCompanyAddress() != null ? nir.getCompanyAddress() : ""
            );
            
            // Delivery Note Number
            javafx.scene.control.TextField deliveryNoteField = new javafx.scene.control.TextField(
                nir.getDeliveryNoteNumber() != null ? nir.getDeliveryNoteNumber() : ""
            );
            
            // Reception Date
            javafx.scene.control.DatePicker receptionDatePicker = new javafx.scene.control.DatePicker(
                nir.getReceptionDate() != null ? nir.getReceptionDate().toLocalDate() : LocalDate.now()
            );
            
            // Committee members
            javafx.scene.control.TextField committee1Field = new javafx.scene.control.TextField(
                nir.getCommittee1Name() != null ? nir.getCommittee1Name() : ""
            );
            javafx.scene.control.TextField committee2Field = new javafx.scene.control.TextField(
                nir.getCommittee2Name() != null ? nir.getCommittee2Name() : ""
            );
            javafx.scene.control.TextField committee3Field = new javafx.scene.control.TextField(
                nir.getCommittee3Name() != null ? nir.getCommittee3Name() : ""
            );
            
            // Warehouse Manager
            javafx.scene.control.TextField warehouseManagerField = new javafx.scene.control.TextField(
                nir.getWarehouseManagerName() != null ? nir.getWarehouseManagerName() : ""
            );
            
            // Discrepancies Notes
            javafx.scene.control.TextArea discrepanciesArea = new javafx.scene.control.TextArea(
                nir.getDiscrepanciesNotes() != null ? nir.getDiscrepanciesNotes() : ""
            );
            discrepanciesArea.setPrefRowCount(3);
            
            // Add header fields to grid
            int row = 0;
            headerGrid.add(new javafx.scene.control.Label("Număr NIR:"), 0, row);
            headerGrid.add(nirNumberField, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Factură:"), 0, row);
            headerGrid.add(invoiceField, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Data NIR:"), 0, row);
            headerGrid.add(nirDatePicker, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Status:"), 0, row);
            headerGrid.add(statusCombo, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Companie:"), 0, row);
            headerGrid.add(companyNameField, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Adresă Companie:"), 0, row);
            headerGrid.add(companyAddressField, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Aviz Însoțire:"), 0, row);
            headerGrid.add(deliveryNoteField, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Data Recepție:"), 0, row);
            headerGrid.add(receptionDatePicker, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Membru Comisie 1:"), 0, row);
            headerGrid.add(committee1Field, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Membru Comisie 2:"), 0, row);
            headerGrid.add(committee2Field, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Membru Comisie 3:"), 0, row);
            headerGrid.add(committee3Field, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Gestionar:"), 0, row);
            headerGrid.add(warehouseManagerField, 1, row++);
            
            headerGrid.add(new javafx.scene.control.Label("Observații/Diferențe:"), 0, row);
            headerGrid.add(discrepanciesArea, 1, row++);
            
            headerTab.setContent(new javafx.scene.control.ScrollPane(headerGrid));
            
            // ============ TAB 2: Product Lines ============
            javafx.scene.control.Tab linesTab = new javafx.scene.control.Tab("Produse");
            javafx.scene.layout.VBox linesBox = new javafx.scene.layout.VBox(10);
            linesBox.setPadding(new javafx.geometry.Insets(15));
            
            // Product lines table
            javafx.scene.control.TableView<ReceptionNoteLine> linesTable = new javafx.scene.control.TableView<>();
            javafx.collections.ObservableList<ReceptionNoteLine> lines = javafx.collections.FXCollections.observableArrayList(nir.getLines());
            linesTable.setItems(lines);
            linesTable.setEditable(true);
            linesTable.setPrefHeight(400);
            
            // Columns
            javafx.scene.control.TableColumn<ReceptionNoteLine, String> productCol = new javafx.scene.control.TableColumn<>("Produs");
            productCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("productName"));
            productCol.setPrefWidth(150);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, String> codeCol = new javafx.scene.control.TableColumn<>("Cod");
            codeCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("productCode"));
            codeCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
            codeCol.setOnEditCommit(event -> event.getRowValue().setProductCode(event.getNewValue()));
            codeCol.setPrefWidth(80);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, String> unitCol = new javafx.scene.control.TableColumn<>("UM");
            unitCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("unit"));
            unitCol.setPrefWidth(60);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, BigDecimal> invoicedQtyCol = new javafx.scene.control.TableColumn<>("Cant. Fact.");
            invoicedQtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("invoicedQuantity"));
            invoicedQtyCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.setScale(2, java.math.RoundingMode.HALF_UP).toString());
                }
            });
            invoicedQtyCol.setPrefWidth(80);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, BigDecimal> receivedQtyCol = new javafx.scene.control.TableColumn<>("Cant. Recep.");
            receivedQtyCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("receivedQuantity"));
            receivedQtyCol.setCellFactory(col -> new javafx.scene.control.cell.TextFieldTableCell<>(new javafx.util.converter.BigDecimalStringConverter()));
            receivedQtyCol.setOnEditCommit(event -> {
                event.getRowValue().setReceivedQuantity(event.getNewValue());
                linesTable.refresh();
            });
            receivedQtyCol.setPrefWidth(90);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, BigDecimal> diffCol = new javafx.scene.control.TableColumn<>("Difer.");
            diffCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantityDifference"));
            diffCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("");
                        setStyle("");
                    } else {
                        setText(item.setScale(2, java.math.RoundingMode.HALF_UP).toString());
                        if (item.compareTo(BigDecimal.ZERO) != 0) {
                            setStyle("-fx-background-color: #ffeb3b;"); // Yellow for discrepancy
                        } else {
                            setStyle("");
                        }
                    }
                }
            });
            diffCol.setPrefWidth(70);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, BigDecimal> unitPriceCol = new javafx.scene.control.TableColumn<>("Preț Unit.");
            unitPriceCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("unitPrice"));
            unitPriceCol.setCellFactory(col -> new javafx.scene.control.cell.TextFieldTableCell<>(new javafx.util.converter.BigDecimalStringConverter()));
            unitPriceCol.setOnEditCommit(event -> {
                event.getRowValue().setUnitPrice(event.getNewValue());
                linesTable.refresh();
            });
            unitPriceCol.setPrefWidth(80);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, BigDecimal> vatRateCol = new javafx.scene.control.TableColumn<>("TVA %");
            vatRateCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("vatRate"));
            vatRateCol.setCellFactory(col -> new javafx.scene.control.cell.TextFieldTableCell<>(new javafx.util.converter.BigDecimalStringConverter()));
            vatRateCol.setOnEditCommit(event -> {
                event.getRowValue().setVatRate(event.getNewValue());
                linesTable.refresh();
            });
            vatRateCol.setPrefWidth(70);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, BigDecimal> valueNoVatCol = new javafx.scene.control.TableColumn<>("Val. fără TVA");
            valueNoVatCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("valueWithoutVAT"));
            valueNoVatCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.setScale(2, java.math.RoundingMode.HALF_UP).toString());
                }
            });
            valueNoVatCol.setPrefWidth(90);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, BigDecimal> vatAmountCol = new javafx.scene.control.TableColumn<>("TVA");
            vatAmountCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("vatAmount"));
            vatAmountCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.setScale(2, java.math.RoundingMode.HALF_UP).toString());
                }
            });
            vatAmountCol.setPrefWidth(70);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, BigDecimal> totalCol = new javafx.scene.control.TableColumn<>("Total");
            totalCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("totalValue"));
            totalCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.setScale(2, java.math.RoundingMode.HALF_UP).toString());
                }
            });
            totalCol.setPrefWidth(90);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, BigDecimal> markupCol = new javafx.scene.control.TableColumn<>("Adaos %");
            markupCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("markupPercentage"));
            markupCol.setCellFactory(col -> new javafx.scene.control.cell.TextFieldTableCell<>(new javafx.util.converter.BigDecimalStringConverter()));
            markupCol.setOnEditCommit(event -> {
                event.getRowValue().setMarkupPercentage(event.getNewValue());
                linesTable.refresh();
            });
            markupCol.setPrefWidth(80);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, BigDecimal> salePriceCol = new javafx.scene.control.TableColumn<>("Preț Vânz.");
            salePriceCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("salePrice"));
            salePriceCol.setCellFactory(col -> new javafx.scene.control.cell.TextFieldTableCell<>(new javafx.util.converter.BigDecimalStringConverter()));
            salePriceCol.setOnEditCommit(event -> {
                event.getRowValue().setSalePrice(event.getNewValue());
                linesTable.refresh();
            });
            salePriceCol.setPrefWidth(90);
            
            javafx.scene.control.TableColumn<ReceptionNoteLine, String> notesCol = new javafx.scene.control.TableColumn<>("Observații");
            notesCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("discrepancyNotes"));
            notesCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
            notesCol.setOnEditCommit(event -> event.getRowValue().setDiscrepancyNotes(event.getNewValue()));
            notesCol.setPrefWidth(120);
            
            linesTable.getColumns().addAll(productCol, codeCol, unitCol, invoicedQtyCol, receivedQtyCol, 
                                          diffCol, unitPriceCol, vatRateCol, valueNoVatCol, vatAmountCol, 
                                          totalCol, markupCol, salePriceCol, notesCol);
            
            // Totals labels
            javafx.scene.control.Label totalValueLabel = new javafx.scene.control.Label();
            javafx.scene.control.Label totalVatLabel = new javafx.scene.control.Label();
            javafx.scene.control.Label grandTotalLabel = new javafx.scene.control.Label();
            
            // Update totals function
            Runnable updateTotals = () -> {
                BigDecimal totalNoVat = BigDecimal.ZERO;
                BigDecimal totalVat = BigDecimal.ZERO;
                for (ReceptionNoteLine line : lines) {
                    if (line.getValueWithoutVAT() != null) totalNoVat = totalNoVat.add(line.getValueWithoutVAT());
                    if (line.getVatAmount() != null) totalVat = totalVat.add(line.getVatAmount());
                }
                BigDecimal grandTotal = totalNoVat.add(totalVat);
                
                totalValueLabel.setText("Total fără TVA: " + totalNoVat.setScale(2, java.math.RoundingMode.HALF_UP) + " RON");
                totalVatLabel.setText("TVA: " + totalVat.setScale(2, java.math.RoundingMode.HALF_UP) + " RON");
                grandTotalLabel.setText("TOTAL: " + grandTotal.setScale(2, java.math.RoundingMode.HALF_UP) + " RON");
                grandTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            };
            
            // Initial totals
            updateTotals.run();
            
            // Update totals on table change
            lines.addListener((javafx.collections.ListChangeListener<ReceptionNoteLine>) c -> updateTotals.run());
            
            javafx.scene.layout.HBox totalsBox = new javafx.scene.layout.HBox(20, totalValueLabel, totalVatLabel, grandTotalLabel);
            totalsBox.setPadding(new javafx.geometry.Insets(10));
            totalsBox.setStyle("-fx-background-color: #e0f7fa; -fx-border-color: #00bcd4; -fx-border-width: 2;");
            
            linesBox.getChildren().addAll(
                new javafx.scene.control.Label("Produse din NIR (editabil - faceți dublu-click pe celulă):"),
                linesTable,
                totalsBox
            );
            linesTab.setContent(linesBox);
            
            // Add tabs
            tabPane.getTabs().addAll(headerTab, linesTab);
            
            dialog.getDialogPane().setContent(tabPane);
            dialog.getDialogPane().setPrefSize(1200, 700);
            
            // Add buttons
            dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK,
                javafx.scene.control.ButtonType.CANCEL
            );
            
            // Apply SmartBill CSS style
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            // Process result
            dialog.showAndWait().ifPresent(response -> {
                if (response == javafx.scene.control.ButtonType.OK) {
                    try {
                        // Update NIR header
                        nir.setNirDate(nirDatePicker.getValue().atStartOfDay());
                        nir.setStatus(statusCombo.getValue());
                        nir.setCompanyName(companyNameField.getText());
                        nir.setCompanyAddress(companyAddressField.getText());
                        nir.setDeliveryNoteNumber(deliveryNoteField.getText());
                        nir.setReceptionDate(receptionDatePicker.getValue().atStartOfDay());
                        nir.setCommittee1Name(committee1Field.getText());
                        nir.setCommittee2Name(committee2Field.getText());
                        nir.setCommittee3Name(committee3Field.getText());
                        nir.setWarehouseManagerName(warehouseManagerField.getText());
                        nir.setDiscrepanciesNotes(discrepanciesArea.getText());
                        
                        // Lines are already updated via table editing
                        // Recalculate all line values
                        for (ReceptionNoteLine line : lines) {
                            line.calculateValues();
                            line.calculateDifference();
                            line.calculateSalePrice();
                        }
                        
                        // Update totals
                        nir.calculateTotals();
                        nir.checkDiscrepancies();
                        
                        // Save to database
                        receptionNoteService.saveReceptionNote(nir);
                        
                        // Refresh table
                        loadReceptionNotes();
                        
                        showSuccessMessage("NIR actualizat cu succes!");
                        logger.info("NIR updated: {} with {} lines", nir.getNirNumber(), lines.size());
                        
                    } catch (Exception e) {
                        logger.error("Error updating NIR", e);
                        showError("Eroare la salvarea NIR: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("Error editing NIR", e);
            showError("Eroare la editarea NIR: " + e.getMessage());
        }
    }
    
    private void viewReceptionNote(ReceptionNote nir) {
        // For backward compatibility, redirect to edit
        editReceptionNote(nir);
    }
    
    private void exportReceptionNotePdf(ReceptionNote nir) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvează NIR PDF");
            fileChooser.setInitialFileName(nir.getNirNumber() + ".pdf");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            
            File file = fileChooser.showSaveDialog(nirTable.getScene().getWindow());
            
            if (file != null) {
                pdfService.generateReceptionNotePdf(nir, file.getAbsolutePath());
                showSuccessMessage("NIR exportat cu succes în: " + file.getAbsolutePath());
                logger.info("NIR exported to PDF: {}", file.getAbsolutePath());
            }
            
        } catch (Exception e) {
            logger.error("Error exporting NIR to PDF", e);
            showError("Eroare la exportarea NIR: " + e.getMessage());
        }
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
