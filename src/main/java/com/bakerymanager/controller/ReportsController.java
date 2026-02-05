package com.bakerymanager.controller;

import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.ProductService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class ReportsController {
    
    private final ProductService productService;
    private final IngredientService ingredientService;
    
    public ReportsController(ProductService productService, IngredientService ingredientService) {
        this.productService = productService;
        this.ingredientService = ingredientService;
    }
    
    @FXML
    private ComboBox<String> reportTypeCombo;
    
    @FXML
    private DatePicker startDatePicker;
    
    @FXML
    private DatePicker endDatePicker;
    
    @FXML
    private Label reportTitleLabel;
    
    @FXML
    private TextArea reportContentArea;
    
    @FXML
    public void initialize() {
        setupReportTypes();
        System.out.println("Reports controller initialized");
    }
    
    private void setupReportTypes() {
        reportTypeCombo.getItems().addAll(
            "Raport Stocuri",
            "Raport Vânzări",
            "Raport Producție",
            "Raport Costuri",
            "Raport Furnizori",
            "Raport Profitabilitate"
        );
        reportTypeCombo.setValue("Raport Stocuri");
    }
    
    @FXML
    public void generateReport() {
        String reportType = reportTypeCombo.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        StringBuilder report = new StringBuilder();
        
        switch (reportType) {
            case "Raport Stocuri":
                generateStockReport(report);
                break;
            case "Raport Vânzări":
                generateSalesReport(report, startDate, endDate);
                break;
            case "Raport Producție":
                generateProductionReport(report, startDate, endDate);
                break;
            case "Raport Costuri":
                generateCostReport(report);
                break;
            case "Raport Furnizori":
                generateSupplierReport(report);
                break;
            case "Raport Profitabilitate":
                generateProfitabilityReport(report);
                break;
        }
        
        reportTitleLabel.setText(reportType + " - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        reportContentArea.setText(report.toString());
    }
    
    private void generateStockReport(StringBuilder report) {
        report.append("=== RAPORT STOCURI ===\n");
        report.append("Generat la: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");
        
        report.append("PRODUSE:\n");
        report.append("----------------------------------------\n");
        productService.getAvailableProducts().forEach(product -> {
            report.append(String.format("%-30s %8.2f %s\n", 
                product.getName(), 
                product.getPhysicalStock(), 
                "buc"));
        });
        
        report.append("\nINGREDIENTE:\n");
        report.append("----------------------------------------\n");
        ingredientService.getAllIngredients().forEach(ingredient -> {
            report.append(String.format("%-30s %8.2f %s\n", 
                ingredient.getName(), 
                ingredient.getCurrentStock(), 
                ingredient.getUnitOfMeasure().getDisplayName()));
        });
        
        report.append("\nSTOCURI SCĂZUTE:\n");
        report.append("----------------------------------------\n");
        ingredientService.getLowStockIngredients().forEach(ingredient -> {
            report.append(String.format("%-30s %8.2f %s (min: %8.2f)\n", 
                ingredient.getName(), 
                ingredient.getCurrentStock(),
                ingredient.getUnitOfMeasure().getDisplayName(),
                ingredient.getMinimumStock()));
        });
    }
    
    private void generateSalesReport(StringBuilder report, LocalDate startDate, LocalDate endDate) {
        report.append("=== RAPORT VÂNZĂRI ===\n");
        report.append("Perioada: ");
        if (startDate != null && endDate != null) {
            report.append(startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                  .append(" - ")
                  .append(endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        } else {
            report.append("Nespecificat");
        }
        report.append("\n\n");
        report.append("Raportul vânzărilor este în dezvoltare.\n");
        report.append("Funcționalități viitoare:\n");
        report.append("- Total vânzări pe perioadă\n");
        report.append("- Top produse vândute\n");
        report.append("- Vânzări pe categorii\n");
        report.append("- Grafice evoluție vânzări\n");
    }
    
    private void generateProductionReport(StringBuilder report, LocalDate startDate, LocalDate endDate) {
        report.append("=== RAPORT PRODUCȚIE ===\n");
        report.append("Perioada: ");
        if (startDate != null && endDate != null) {
            report.append(startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                  .append(" - ")
                  .append(endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        } else {
            report.append("Nespecificat");
        }
        report.append("\n\n");
        report.append("Raportul producției este în dezvoltare.\n");
        report.append("Funcționalități viitoare:\n");
        report.append("- Cantități produse pe perioadă\n");
        report.append("- Consum ingrediente\n");
        report.append("- Eficiență producție\n");
        report.append("- Costuri producție\n");
    }
    
    private void generateCostReport(StringBuilder report) {
        report.append("=== RAPORT COSTURI ===\n");
        report.append("Generat la: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");
        
        BigDecimal totalStockValue = ingredientService.getAllIngredients().stream()
            .filter(ing -> ing.getCurrentStock() != null && ing.getLastPurchasePrice() != null)
            .map(ing -> ing.getCurrentStock().multiply(ing.getLastPurchasePrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        report.append("VALOARE TOTALĂ STOC INGREDIENTE: ").append(String.format("%.2f lei", totalStockValue)).append("\n\n");
        
        report.append("DETALII COSTURI INGREDIENTE:\n");
        report.append("----------------------------------------\n");
        ingredientService.getAllIngredients().forEach(ingredient -> {
            if (ingredient.getCurrentStock() != null && ingredient.getLastPurchasePrice() != null) {
                BigDecimal value = ingredient.getCurrentStock().multiply(ingredient.getLastPurchasePrice());
                report.append(String.format("%-30s %8.2f %s @ %8.2f = %8.2f lei\n", 
                    ingredient.getName(), 
                    ingredient.getCurrentStock(),
                    ingredient.getUnitOfMeasure().getDisplayName(),
                    ingredient.getLastPurchasePrice(),
                    value));
            }
        });
    }
    
    private void generateSupplierReport(StringBuilder report) {
        report.append("=== RAPORT FURNIZORI ===\n");
        report.append("Generat la: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");
        report.append("Raportul furnizorilor este în dezvoltare.\n");
        report.append("Funcționalități viitoare:\n");
        report.append("- Lista furnizori activi\n");
        report.append("- Valoare achiziții per furnizor\n");
        report.append("- Istoric facturi per furnizor\n");
        report.append("- Termeni de plată\n");
    }
    
    private void generateProfitabilityReport(StringBuilder report) {
        report.append("=== RAPORT PROFITABILITATE ===\n");
        report.append("Generat la: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");
        report.append("Raportul de profitabilitate este în dezvoltare.\n");
        report.append("Funcționalități viitoare:\n");
        report.append("- Profit per produs\n");
        report.append("- Marje de profit\n");
        report.append("- Analiză costuri vs venituri\n");
        report.append("- Proiecții profitabilitate\n");
    }
    
    @FXML
    public void exportPDF() {
        showInfo("Export PDF - funcționalitate în dezvoltare");
    }
    
    @FXML
    public void printReport() {
        showInfo("Printare raport - funcționalitate în dezvoltare");
    }
    
    @FXML
    public void sendEmail() {
        showInfo("Trimitere email - funcționalitate în dezvoltare");
    }
    
    private void showInfo(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Informații");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
