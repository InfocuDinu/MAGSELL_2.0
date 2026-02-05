package com.bakerymanager.controller;

import com.bakerymanager.entity.Invoice;
import com.bakerymanager.service.InvoiceService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class InvoicesController {
    
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
        System.out.println("Invoices controller initialized");
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
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Informații");
        alert.setHeaderText(null);
        alert.setContentText("Import SPV - funcționalitate în dezvoltare");
        alert.show();
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
