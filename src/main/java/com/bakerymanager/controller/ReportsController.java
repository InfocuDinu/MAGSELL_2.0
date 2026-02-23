package com.bakerymanager.controller;

import com.bakerymanager.smartbill.reports.api.ReportingFacade;
import com.bakerymanager.entity.SaleItem;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.entity.Product;
import com.bakerymanager.service.AlertService;
import com.bakerymanager.service.Alert;
import com.bakerymanager.service.CustomerService;
import com.bakerymanager.service.CustomOrderService;
import com.bakerymanager.service.ForecastService;
import com.bakerymanager.service.AccountingExportService;
import com.bakerymanager.service.UnitConversionService;
import com.bakerymanager.entity.Customer;
import com.bakerymanager.entity.CustomOrder;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReportsController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportsController.class);
    
    private final ReportingFacade reportingFacade;
    private final AlertService alertService;
    private final CustomerService customerService;
    private final CustomOrderService customOrderService;
    private final ForecastService forecastService;
    private final AccountingExportService accountingExportService;
    private final UnitConversionService unitConversionService;
    
    public ReportsController(ReportingFacade reportingFacade,
                             AlertService alertService,
                             CustomerService customerService,
                             CustomOrderService customOrderService,
                             ForecastService forecastService,
                             AccountingExportService accountingExportService,
                             UnitConversionService unitConversionService) {
        this.reportingFacade = reportingFacade;
        this.alertService = alertService;
        this.customerService = customerService;
        this.customOrderService = customOrderService;
        this.forecastService = forecastService;
        this.accountingExportService = accountingExportService;
        this.unitConversionService = unitConversionService;
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
        logger.info("Reports controller initialized");
    }
    
    private void setupReportTypes() {
        reportTypeCombo.getItems().addAll(
            "Raport Stocuri",
            "Raport Loturi Expirare",
            "Raport Mișcări Stoc",
            "Raport Consum Standard vs Real",
            "Raport Alertare Inteligentă",
            "Raport Precomenzi",
            "Raport Loyalty",
            "Raport Forecast Cerere",
            "Export Contabil CSV",
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
            case "Raport Loturi Expirare":
                generateExpiringBatchesReport(report, startDate, endDate);
                break;
            case "Raport Mișcări Stoc":
                generateStockMovementsReport(report, startDate, endDate);
                break;
            case "Raport Consum Standard vs Real":
                generateConsumptionVarianceReport(report, startDate, endDate);
                break;
            case "Raport Alertare Inteligentă":
                generateAlertingReport(report, startDate, endDate);
                break;
            case "Raport Precomenzi":
                generatePreordersReport(report, startDate, endDate);
                break;
            case "Raport Loyalty":
                generateLoyaltyReport(report);
                break;
            case "Raport Forecast Cerere":
                generateForecastReport(report, startDate, endDate);
                break;
            case "Export Contabil CSV":
                generateAccountingExportInfo(report, startDate, endDate);
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
        reportingFacade.getAvailableProducts().forEach(product -> {
            report.append(String.format("%-30s %8.2f %s\n", 
                product.getName(), 
                product.getPhysicalStock(), 
                "buc"));
        });
        
        report.append("\nINGREDIENTE:\n");
        report.append("----------------------------------------\n");
        reportingFacade.getAllIngredients().forEach(ingredient -> {
            report.append(String.format("%-30s %8.2f %s\n", 
                ingredient.getName(), 
                ingredient.getCurrentStock(), 
                ingredient.getUnitOfMeasure().getDisplayName()));
        });
        
        report.append("\nSTOCURI SCĂZUTE:\n");
        report.append("----------------------------------------\n");
        reportingFacade.getLowStockIngredients().forEach(ingredient -> {
            report.append(String.format("%-30s %8.2f %s (min: %8.2f)\n", 
                ingredient.getName(), 
                ingredient.getCurrentStock(),
                ingredient.getUnitOfMeasure().getDisplayName(),
                ingredient.getMinimumStock()));
        });
    }

    private void generateExpiringBatchesReport(StringBuilder report, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start.plusDays(30);

        report.append("=== RAPORT LOTURI CU EXPIRARE ===\n");
        report.append("Perioada: ")
            .append(start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append(" - ")
            .append(end.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append("\n\n");

        reportingFacade.getExpiringBatches(start, end).forEach(batch -> {
            String ingredientName = batch.getIngredient() != null ? batch.getIngredient().getName() : "";
            String expiry = batch.getExpiryDate() != null
                ? batch.getExpiryDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                : "N/A";

            report.append(String.format("%-25s Lot:%-10s Exp:%-12s Qty:%8.3f\n",
                ingredientName,
                batch.getBatchCode() != null ? batch.getBatchCode() : "-",
                expiry,
                batch.getQuantity() != null ? batch.getQuantity() : BigDecimal.ZERO
            ));
        });
    }

    private void generateStockMovementsReport(StringBuilder report, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay().minusSeconds(1) : LocalDateTime.now();

        report.append("=== RAPORT MIȘCĂRI STOC ===\n");
        report.append("Perioada: ")
            .append(start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append(" - ")
            .append(end.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append("\n\n");

        reportingFacade.getStockMovements(start, end).forEach(movement -> {
            String ingredientName = movement.getIngredient() != null ? movement.getIngredient().getName() : "";
            String dateStr = movement.getMovementDate() != null
                ? movement.getMovementDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                : "";
            String batch = movement.getBatch() != null ? movement.getBatch().getBatchCode() : "-";

            report.append(String.format("%s | %-20s | %-10s | %8.3f %s | Lot:%s\n",
                dateStr,
                ingredientName,
                movement.getMovementType(),
                movement.getQuantity() != null ? movement.getQuantity() : BigDecimal.ZERO,
                movement.getUnit() != null ? movement.getUnit() : "",
                batch
            ));
        });
    }

    private void generateConsumptionVarianceReport(StringBuilder report, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        report.append("=== RAPORT CONSUM STANDARD VS REAL ===\n");
        report.append("Perioada: ")
            .append(start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append(" - ")
            .append(end.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append("\n\n");

        reportingFacade.getProductionConsumptions(start, end).forEach(consumption -> {
            String ingredientName = consumption.getIngredient() != null ? consumption.getIngredient().getName() : "";
            BigDecimal standardQty = consumption.getStandardQuantity() != null ? consumption.getStandardQuantity() : BigDecimal.ZERO;
            BigDecimal actualQty = consumption.getActualQuantity() != null ? consumption.getActualQuantity() : BigDecimal.ZERO;
            BigDecimal variance = actualQty.subtract(standardQty);
            String unit = consumption.getUnit() != null ? consumption.getUnit() : "";

            report.append(String.format("%-25s | Std: %8.3f | Real: %8.3f | Var: %+8.3f %s\n",
                ingredientName,
                standardQty,
                actualQty,
                variance,
                unit
            ));
        });
    }

    private void generateAlertingReport(StringBuilder report, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        report.append("=== RAPORT ALERTARE INTELIGENTĂ ===\n");
        report.append("Perioada: ")
            .append(start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append(" - ")
            .append(end.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append("\n\n");

        List<Alert> alerts = alertService.getAlerts(start, end);
        if (alerts.isEmpty()) {
            report.append("Nu există alerte în perioada selectată.\n");
            return;
        }

        for (Alert alert : alerts) {
            report.append(String.format("[%s] %s | %s\n",
                alert.getSeverity(),
                alert.getType(),
                alert.getMessage()
            ));
        }
    }

    private void generatePreordersReport(StringBuilder report, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = (startDate != null ? startDate : LocalDate.now()).atStartOfDay();
        LocalDateTime end = (endDate != null ? endDate : LocalDate.now().plusDays(7)).plusDays(1).atStartOfDay().minusSeconds(1);

        report.append("=== RAPORT PRECOMENZI ===\n");
        report.append("Perioada: ")
            .append(start.toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append(" - ")
            .append(end.toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append("\n\n");

        List<CustomOrder> orders = customOrderService.getOrdersDueBetween(start, end);
        if (orders.isEmpty()) {
            report.append("Nu există precomenzi în interval.\n");
            return;
        }

        for (CustomOrder order : orders) {
            report.append(String.format("%s | %s | %s | %s | %s\n",
                order.getCustomer() != null ? order.getCustomer().getName() : "",
                order.getProductName(),
                order.getQuantity(),
                order.getDueDate() != null ? order.getDueDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "",
                order.getStatus()
            ));
        }
    }

    private void generateLoyaltyReport(StringBuilder report) {
        report.append("=== RAPORT LOYALTY ===\n");
        report.append("Generat la: ")
            .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
            .append("\n\n");

        List<Customer> topCustomers = customerService.getTopCustomers();
        if (topCustomers.isEmpty()) {
            report.append("Nu există clienți înregistrați.\n");
            return;
        }

        for (Customer customer : topCustomers) {
            report.append(String.format("%-25s | Puncte: %4d | Total: %s\n",
                customer.getName(),
                customer.getLoyaltyPoints(),
                customer.getTotalPurchases()
            ));
        }
    }

    private void generateForecastReport(StringBuilder report, LocalDate startDate, LocalDate endDate) {
        report.append("=== RAPORT FORECAST CERERE ===\n");
        report.append("Perioada analizată: ")
            .append((startDate != null ? startDate : LocalDate.now().minusDays(14)).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append(" - ")
            .append((endDate != null ? endDate : LocalDate.now()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append("\n\n");

        List<ForecastService.ForecastItem> forecast = forecastService.getForecast(startDate, endDate);
        if (forecast.isEmpty()) {
            report.append("Nu există date suficiente pentru forecast.\n");
            return;
        }

        for (ForecastService.ForecastItem item : forecast) {
            report.append(String.format("%-25s | Medie/zi: %6.3f | Recomandat: %6.3f\n",
                item.getProduct().getName(),
                item.getAverageDaily(),
                item.getSuggestedProduction()
            ));
        }
    }

    private void generateAccountingExportInfo(StringBuilder report, LocalDate startDate, LocalDate endDate) {
        report.append("=== EXPORT CONTABIL CSV ===\n");
        report.append("Acest raport se exportă ca fișier CSV.\n");
        report.append("Selectați perioada și apăsați Export PDF (CSV).\n");
        report.append("Perioada selectată: ")
            .append(startDate != null ? startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "-")
            .append(" - ")
            .append(endDate != null ? endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "-")
            .append("\n");
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
        
        BigDecimal totalStockValue = reportingFacade.getAllIngredients().stream()
            .filter(ing -> ing.getCurrentStock() != null && ing.getLastPurchasePrice() != null)
            .map(ing -> ing.getCurrentStock().multiply(ing.getLastPurchasePrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        report.append("VALOARE TOTALĂ STOC INGREDIENTE: ").append(String.format("%.2f lei", totalStockValue)).append("\n\n");
        
        report.append("DETALII COSTURI INGREDIENTE:\n");
        report.append("----------------------------------------\n");
        reportingFacade.getAllIngredients().forEach(ingredient -> {
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
        LocalDate startDate = startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now().minusDays(7);
        LocalDate endDate = endDatePicker.getValue() != null ? endDatePicker.getValue() : LocalDate.now();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusSeconds(1);

        report.append("=== RAPORT PROFITABILITATE ===\n");
        report.append("Perioada: ")
            .append(startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append(" - ")
            .append(endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            .append("\n\n");

        List<SaleItem> saleItems = reportingFacade.getSaleItems(start, end);
        if (saleItems.isEmpty()) {
            report.append("Nu există vânzări în perioada selectată.\n");
            return;
        }

        Map<Long, ProfitMetrics> byProduct = new HashMap<>();
        Map<String, ProfitMetrics> byCategory = new HashMap<>();

        for (SaleItem item : saleItems) {
            Product product = item.getProduct();
            if (product == null) {
                continue;
            }

            BigDecimal revenue = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;

            BigDecimal unitCost = calculateUnitCost(product);
            BigDecimal cost = unitCost.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal profit = revenue.subtract(cost);

            ProfitMetrics metrics = byProduct.computeIfAbsent(product.getId(), id -> new ProfitMetrics(product.getName()));
            metrics.add(revenue, cost, profit, qty);

            String category = product.getCategory() != null && !product.getCategory().isBlank()
                ? product.getCategory()
                : "Necategorizat";
            ProfitMetrics catMetrics = byCategory.computeIfAbsent(category, ProfitMetrics::new);
            catMetrics.add(revenue, cost, profit, qty);
        }

        report.append("PROFITABILITATE PE PRODUS:\n");
        report.append("----------------------------------------\n");
        byProduct.values().forEach(metrics -> report.append(metrics.formatLine()));

        report.append("\nPROFITABILITATE PE CATEGORIE:\n");
        report.append("----------------------------------------\n");
        byCategory.values().forEach(metrics -> report.append(metrics.formatLine()));
    }

    private BigDecimal calculateUnitCost(Product product) {
        List<RecipeItem> recipeItems = reportingFacade.getRecipeByProduct(product);
        if (recipeItems == null || recipeItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (RecipeItem item : recipeItems) {
            BigDecimal required = item.getRequiredQuantity() != null ? item.getRequiredQuantity() : BigDecimal.ZERO;
            BigDecimal price = item.getIngredient() != null && item.getIngredient().getLastPurchasePrice() != null
                ? item.getIngredient().getLastPurchasePrice()
                : BigDecimal.ZERO;
            BigDecimal normalizedRequired = toIngredientUnit(item, required);
            total = total.add(normalizedRequired.multiply(price));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toIngredientUnit(RecipeItem recipeItem, BigDecimal quantity) {
        if (quantity == null) {
            return BigDecimal.ZERO;
        }
        if (recipeItem == null || recipeItem.getIngredient() == null) {
            return quantity;
        }
        String ingredientUnit = recipeItem.getIngredient().getUnitOfMeasure() != null
            ? recipeItem.getIngredient().getUnitOfMeasure().name()
            : null;
        String recipeUnit = recipeItem.getUnit();
        if (ingredientUnit == null || ingredientUnit.isBlank() || recipeUnit == null || recipeUnit.isBlank()) {
            return quantity;
        }
        return unitConversionService.convert(quantity, recipeUnit, ingredientUnit);
    }

    private static class ProfitMetrics {
        private final String label;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;
        private BigDecimal quantity = BigDecimal.ZERO;

        private ProfitMetrics(String label) {
            this.label = label;
        }

        private void add(BigDecimal revenue, BigDecimal cost, BigDecimal profit, BigDecimal quantity) {
            this.revenue = this.revenue.add(revenue);
            this.cost = this.cost.add(cost);
            this.profit = this.profit.add(profit);
            this.quantity = this.quantity.add(quantity);
        }

        private String formatLine() {
            BigDecimal margin = BigDecimal.ZERO;
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                margin = profit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }
            return String.format("%-25s | Qty: %8.2f | Rev: %8.2f | Cost: %8.2f | Profit: %8.2f | Marjă: %6.2f%%\n",
                label,
                quantity,
                revenue,
                cost,
                profit,
                margin
            );
        }
    }
    
    @FXML
    public void exportPDF() {
        try {
            String reportType = reportTypeCombo.getValue();
            String reportContent = reportContentArea.getText();
            
            if (reportContent == null || reportContent.trim().isEmpty()) {
                showInfo("Generați mai întâi un raport!");
                return;
            }
            
            if ("Export Contabil CSV".equals(reportType)) {
                exportAccountingCsv();
                return;
            }

            // FileChooser pentru salvare PDF
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvează Raport PDF");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            
            // Nume implicit pentru fișier
            String defaultFileName = reportType.replace(" ", "_") + "_" + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd")) + ".pdf";
            fileChooser.setInitialFileName(defaultFileName);
            
            File selectedFile = fileChooser.showSaveDialog(null);
            if (selectedFile == null) {
                return; // Utilizatorul a anulat
            }
            
            // Creăm PDF-ul
            createPDF(selectedFile, reportType, reportContent);
            
            showInfo("PDF exportat cu succes!\nFișier: " + selectedFile.getAbsolutePath());
            logger.info("PDF exported successfully: {}", selectedFile.getAbsolutePath());
            
        } catch (Exception e) {
            logger.error("Error exporting PDF", e);
            showError("Eroare la exportarea PDF: " + e.getMessage());
        }
    }

    private void exportAccountingCsv() {
        try {
            LocalDate startDate = startDatePicker.getValue() != null ? startDatePicker.getValue() : LocalDate.now().minusDays(30);
            LocalDate endDate = endDatePicker.getValue() != null ? endDatePicker.getValue() : LocalDate.now();
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusSeconds(1);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvează Export Contabil CSV");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            fileChooser.setInitialFileName("export_contabil_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd")) + ".csv");

            File selectedFile = fileChooser.showSaveDialog(null);
            if (selectedFile == null) {
                return;
            }

            accountingExportService.exportSalesCsv(start, end, selectedFile.getAbsolutePath());
            showInfo("Export contabil realizat: " + selectedFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error exporting accounting CSV", e);
            showError("Eroare la export contabil: " + e.getMessage());
        }
    }
    
    private void createPDF(File file, String reportType, String reportContent) throws Exception {
        Document document = new Document();
        FileOutputStream fos = null;
        
        try {
            fos = new FileOutputStream(file);
            PdfWriter.getInstance(document, fos);
        
        document.open();
        
        // Fonturi
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
        
        // Titlu
        Paragraph title = new Paragraph(reportType, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        // Perioada (dacă există)
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        if (startDate != null && endDate != null) {
            Paragraph period = new Paragraph(
                "Perioada: " + startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + 
                " - " + endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), 
                headerFont
            );
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(15);
            document.add(period);
        }
        
        // Data generării
        Paragraph date = new Paragraph(
            "Generat la: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), 
            normalFont
        );
        date.setAlignment(Element.ALIGN_RIGHT);
        date.setSpacingAfter(20);
        document.add(date);
        
        // Tabel cu date (pentru rapoarte de stocuri și costuri)
        if ("Raport Stocuri".equals(reportType) || "Raport Costuri".equals(reportType)) {
            createDataTable(document, reportType);
        } else {
            // Pentru alte rapoarte, adăugăm conținutul ca text
            String[] lines = reportContent.split("\n");
            for (String line : lines) {
                Paragraph paragraph = new Paragraph(line, normalFont);
                paragraph.setSpacingAfter(5);
                document.add(paragraph);
            }
        }
        
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    logger.warn("Error closing file output stream", e);
                }
            }
        }
    }
    
    private void createDataTable(Document document, String reportType) throws Exception {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL);
        
        if ("Raport Stocuri".equals(reportType)) {
            // Tabel produse
            Paragraph productsTitle = new Paragraph("STOC PRODUSE", headerFont);
            productsTitle.setSpacingAfter(10);
            document.add(productsTitle);
            
            PdfPTable productsTable = new PdfPTable(3);
            productsTable.setWidthPercentage(100);
            productsTable.setWidths(new float[]{50f, 25f, 25f});
            
            productsTable.addCell(createCell("Produs", headerFont));
            productsTable.addCell(createCell("Stoc", headerFont));
            productsTable.addCell(createCell("Unitate", headerFont));
            
            reportingFacade.getAvailableProducts().forEach(product -> {
                productsTable.addCell(createCell(product.getName(), normalFont));
                productsTable.addCell(createCell(String.format("%.2f", product.getPhysicalStock()), normalFont));
                productsTable.addCell(createCell("buc", normalFont));
            });
            
            document.add(productsTable);
            document.add(new Paragraph(" ", normalFont)); // Spațiu
            
            // Tabel ingrediente
            Paragraph ingredientsTitle = new Paragraph("STOC INGREDIENTE", headerFont);
            ingredientsTitle.setSpacingAfter(10);
            document.add(ingredientsTitle);
            
            PdfPTable ingredientsTable = new PdfPTable(3);
            ingredientsTable.setWidthPercentage(100);
            ingredientsTable.setWidths(new float[]{50f, 25f, 25f});
            
            ingredientsTable.addCell(createCell("Ingredient", headerFont));
            ingredientsTable.addCell(createCell("Stoc", headerFont));
            ingredientsTable.addCell(createCell("Unitate", headerFont));
            
            reportingFacade.getAllIngredients().forEach(ingredient -> {
                ingredientsTable.addCell(createCell(ingredient.getName(), normalFont));
                ingredientsTable.addCell(createCell(String.format("%.2f", ingredient.getCurrentStock()), normalFont));
                ingredientsTable.addCell(createCell(ingredient.getUnitOfMeasure().getDisplayName(), normalFont));
            });
            
            document.add(ingredientsTable);
            
        } else if ("Raport Costuri".equals(reportType)) {
            PdfPTable costTable = new PdfPTable(5);
            costTable.setWidthPercentage(100);
            costTable.setWidths(new float[]{35f, 15f, 15f, 15f, 20f});
            
            costTable.addCell(createCell("Ingredient", headerFont));
            costTable.addCell(createCell("Cantitate", headerFont));
            costTable.addCell(createCell("Unitate", headerFont));
            costTable.addCell(createCell("Preț", headerFont));
            costTable.addCell(createCell("Valoare", headerFont));
            
            reportingFacade.getAllIngredients().forEach(ingredient -> {
                if (ingredient.getCurrentStock() != null && ingredient.getLastPurchasePrice() != null) {
                    BigDecimal value = ingredient.getCurrentStock().multiply(ingredient.getLastPurchasePrice());
                    
                    costTable.addCell(createCell(ingredient.getName(), normalFont));
                    costTable.addCell(createCell(String.format("%.2f", ingredient.getCurrentStock()), normalFont));
                    costTable.addCell(createCell(ingredient.getUnitOfMeasure().getDisplayName(), normalFont));
                    costTable.addCell(createCell(String.format("%.2f", ingredient.getLastPurchasePrice()), normalFont));
                    costTable.addCell(createCell(String.format("%.2f", value), normalFont));
                }
            });
            
            document.add(costTable);
        }
    }
    
    private com.lowagie.text.pdf.PdfPCell createCell(String content, Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new Paragraph(content, font));
        cell.setPadding(5);
        return cell;
    }
    
    private void showInfo(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Informații");
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
    
    @FXML
    public void printReport() {
        try {
            String reportContent = reportContentArea.getText();
            
            if (reportContent == null || reportContent.trim().isEmpty()) {
                showInfo("Generați mai întâi un raport pentru a-l imprima!");
                return;
            }
            
            // Create a simple print job using JavaFX PrinterJob
            javafx.print.PrinterJob printerJob = javafx.print.PrinterJob.createPrinterJob();
            
            if (printerJob != null) {
                // Show print dialog to user
                boolean proceed = printerJob.showPrintDialog(reportContentArea.getScene().getWindow());
                
                if (proceed) {
                    // Create a temporary TextArea for printing (to avoid affecting the visible one)
                    TextArea printArea = new TextArea(reportContent);
                    printArea.setWrapText(true);
                    printArea.setEditable(false);
                    
                    // Print the content
                    boolean success = printerJob.printPage(printArea);
                    
                    if (success) {
                        printerJob.endJob();
                        showInfo("Raportul a fost trimis la imprimantă!");
                        logger.info("Report printed successfully");
                    } else {
                        showError("Eroare la trimiterea raportului la imprimantă.");
                        logger.error("Failed to print report");
                    }
                } else {
                    logger.info("User cancelled printing");
                }
            } else {
                showError("Nu s-a putut crea job-ul de printare. Verificați că aveți o imprimantă configurată.");
                logger.error("PrinterJob could not be created");
            }
            
        } catch (Exception e) {
            logger.error("Error printing report", e);
            showError("Eroare la printarea raportului: " + e.getMessage());
        }
    }
    
    @FXML
    public void sendEmail() {
        try {
            String reportContent = reportContentArea.getText();
            String reportType = reportTypeCombo.getValue();
            
            if (reportContent == null || reportContent.trim().isEmpty()) {
                showInfo("Generați mai întâi un raport pentru a-l trimite prin email!");
                return;
            }
            
            // This is a placeholder for future email functionality
            // In production, this would integrate with JavaMail API or similar
            showInfo("Funcționalitatea de trimitere email este în dezvoltare.\n\n" +
                     "Funcționalități viitoare:\n" +
                     "- Configurare server SMTP\n" +
                     "- Selectare destinatari\n" +
                     "- Atașare raport PDF\n" +
                     "- Șablon personalizabil pentru email\n\n" +
                     "Pentru moment, folosiți funcția 'Export PDF' și " +
                     "trimiteți manual raportul prin email.");
            
            logger.info("Email send requested for report: {}", reportType);
            
        } catch (Exception e) {
            logger.error("Error in sendEmail", e);
            showError("Eroare: " + e.getMessage());
        }
    }
}
