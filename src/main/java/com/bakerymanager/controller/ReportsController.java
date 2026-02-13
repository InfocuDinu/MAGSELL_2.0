package com.bakerymanager.controller;

import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.ProductService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class ReportsController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportsController.class);
    
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
        logger.info("Reports controller initialized");
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
        try {
            String reportType = reportTypeCombo.getValue();
            String reportContent = reportContentArea.getText();
            
            if (reportContent == null || reportContent.trim().isEmpty()) {
                showInfo("Generați mai întâi un raport!");
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
            
            productService.getAvailableProducts().forEach(product -> {
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
            
            ingredientService.getAllIngredients().forEach(ingredient -> {
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
            
            ingredientService.getAllIngredients().forEach(ingredient -> {
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
