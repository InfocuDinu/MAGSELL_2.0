package com.bakerymanager.service;

import com.bakerymanager.entity.ProductionReport;
import com.bakerymanager.entity.RecipeItem;
import com.bakerymanager.entity.ReceptionNote;
import com.bakerymanager.entity.ReceptionNoteLine;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);
    
    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font FONT_SUBTITLE = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font FONT_NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font FONT_SMALL = new Font(Font.HELVETICA, 8, Font.NORMAL);
    private static final Font FONT_BOLD = new Font(Font.HELVETICA, 10, Font.BOLD);
    
    /**
     * Generate PDF for Production Report
     */
    public void generateProductionReportPdf(ProductionReport report, String filePath) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();
            
            // Company Header
            addTitle(document, "MAGSELL 2.0 - BakeryManager Pro");
            addParagraph(document, " ", FONT_NORMAL);
            
            // Document Title
            addTitle(document, "RAPORT DE PRODUCȚIE");
            addParagraph(document, " ", FONT_NORMAL);
            
            // Production Details
            addSubtitle(document, "Detalii Producție:");
            addParagraph(document, "Data/Ora: " + formatDateTime(report.getProductionDate()), FONT_NORMAL);
            addParagraph(document, "Produs: " + report.getProduct().getName(), FONT_NORMAL);
            addParagraph(document, "Cantitate Produsă: " + formatBigDecimal(report.getQuantityProduced()), FONT_NORMAL);
            
            if (report.getProduct().getBatchNumber() != null) {
                addParagraph(document, "Număr Lot: " + report.getProduct().getBatchNumber(), FONT_NORMAL);
            }
            
            addParagraph(document, "Status: " + report.getStatus(), FONT_NORMAL);
            addParagraph(document, " ", FONT_NORMAL);
            
            // Ingredients Table
            if (report.getProduct().getRecipeItems() != null && !report.getProduct().getRecipeItems().isEmpty()) {
                addSubtitle(document, "Ingrediente Utilizate:");
                
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{3, 1.5f, 1, 1.5f, 1.5f});
                
                // Table Header
                addTableHeader(table, new String[]{"Ingredient", "Cantitate Necesară", "UM", "Stoc Disponibil", "Status"});
                
                // Table Rows
                for (RecipeItem item : report.getProduct().getRecipeItems()) {
                    table.addCell(createCell(item.getIngredient().getName(), FONT_NORMAL));
                    table.addCell(createCell(formatBigDecimal(item.getRequiredQuantity()), FONT_NORMAL));
                    table.addCell(createCell(item.getIngredient().getUnitOfMeasure().name(), FONT_NORMAL));
                    table.addCell(createCell(formatBigDecimal(item.getIngredient().getCurrentStock()), FONT_NORMAL));
                    
                    BigDecimal required = item.getRequiredQuantity().multiply(report.getQuantityProduced());
                    boolean sufficient = item.getIngredient().getCurrentStock().compareTo(required) >= 0;
                    table.addCell(createCell(sufficient ? "✓ Suficient" : "✗ Insuficient", FONT_SMALL));
                }
                
                document.add(table);
                addParagraph(document, " ", FONT_NORMAL);
                addParagraph(document, "Total Ingrediente: " + report.getProduct().getRecipeItems().size(), FONT_BOLD);
            }
            
            // Notes
            if (report.getNotes() != null && !report.getNotes().isEmpty()) {
                addParagraph(document, " ", FONT_NORMAL);
                addSubtitle(document, "Observații:");
                addParagraph(document, report.getNotes(), FONT_NORMAL);
            }
            
            // Footer
            addParagraph(document, " ", FONT_NORMAL);
            addParagraph(document, " ", FONT_NORMAL);
            addParagraph(document, "Document generat: " + formatDateTime(LocalDateTime.now()), FONT_SMALL);
            
            logger.info("Production report PDF generated successfully: {}", filePath);
            
        } finally {
            document.close();
        }
    }
    
    /**
     * Generate PDF for Reception Note (NIR)
     */
    public void generateReceptionNotePdf(ReceptionNote nirNote, String filePath) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();
            
            // Company Header
            addTitle(document, nirNote.getCompanyName());
            if (nirNote.getCompanyAddress() != null) {
                addParagraph(document, nirNote.getCompanyAddress(), FONT_NORMAL);
            }
            addParagraph(document, " ", FONT_NORMAL);
            
            // Document Title
            addTitle(document, "NOTĂ DE INTRARE RECEPȚIE");
            addParagraph(document, " ", FONT_NORMAL);
            
            // NIR Details
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 1});
            
            // Left column
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph("Număr NIR: " + nirNote.getNirNumber(), FONT_BOLD));
            leftCell.addElement(new Paragraph("Data: " + formatDate(nirNote.getNirDate()), FONT_NORMAL));
            leftCell.addElement(new Paragraph(" ", FONT_NORMAL));
            leftCell.addElement(new Paragraph("Furnizor: " + nirNote.getInvoice().getSupplierName(), FONT_NORMAL));
            if (nirNote.getInvoice().getSupplierCui() != null) {
                leftCell.addElement(new Paragraph("CUI: " + nirNote.getInvoice().getSupplierCui(), FONT_NORMAL));
            }
            headerTable.addCell(leftCell);
            
            // Right column
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.addElement(new Paragraph("Factură: " + nirNote.getInvoice().getInvoiceNumber(), FONT_NORMAL));
            rightCell.addElement(new Paragraph("Data facturii: " + formatDate(nirNote.getInvoice().getInvoiceDate()), FONT_NORMAL));
            if (nirNote.getDeliveryNoteNumber() != null) {
                rightCell.addElement(new Paragraph("Aviz: " + nirNote.getDeliveryNoteNumber(), FONT_NORMAL));
            }
            rightCell.addElement(new Paragraph("Data recepție: " + formatDate(nirNote.getReceptionDate()), FONT_NORMAL));
            headerTable.addCell(rightCell);
            
            document.add(headerTable);
            addParagraph(document, " ", FONT_NORMAL);
            
            // Products Table
            addSubtitle(document, "Identificarea Mărfurilor:");
            
            PdfPTable productsTable = new PdfPTable(8);
            productsTable.setWidthPercentage(100);
            productsTable.setWidths(new float[]{3, 1, 1, 1, 1, 1.5f, 1, 1.5f});
            
            // Table Header
            addTableHeader(productsTable, new String[]{
                "Denumire", "Cod", "UM", "Cant. Facturată", "Cant. Recepționată", 
                "Preț Unitar", "Val. fără TVA", "TVA", "Val. Totală"
            });
            
            // Table Rows
            for (ReceptionNoteLine line : nirNote.getLines()) {
                productsTable.addCell(createCell(line.getProductName(), FONT_SMALL));
                productsTable.addCell(createCell(line.getProductCode() != null ? line.getProductCode() : "-", FONT_SMALL));
                productsTable.addCell(createCell(line.getUnit(), FONT_SMALL));
                productsTable.addCell(createCell(formatBigDecimal(line.getInvoicedQuantity()), FONT_SMALL));
                
                // Highlight differences
                String receivedQty = formatBigDecimal(line.getReceivedQuantity());
                if (line.getHasDiscrepancy()) {
                    PdfPCell cell = createCell(receivedQty + " ⚠", FONT_SMALL);
                    cell.setBackgroundColor(new Color(255, 255, 200)); // Light yellow
                    productsTable.addCell(cell);
                } else {
                    productsTable.addCell(createCell(receivedQty, FONT_SMALL));
                }
                
                productsTable.addCell(createCell(formatBigDecimal(line.getUnitPrice()), FONT_SMALL));
                productsTable.addCell(createCell(formatBigDecimal(line.getValueWithoutVAT()), FONT_SMALL));
                productsTable.addCell(createCell(line.getVatRate() + "%", FONT_SMALL));
                productsTable.addCell(createCell(formatBigDecimal(line.getTotalValue()), FONT_SMALL));
            }
            
            document.add(productsTable);
            addParagraph(document, " ", FONT_NORMAL);
            
            // Totals
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(40);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            totalsTable.addCell(createCell("Total fără TVA:", FONT_BOLD));
            totalsTable.addCell(createCell(formatBigDecimal(nirNote.getTotalValueWithoutVAT()) + " LEI", FONT_NORMAL));
            
            totalsTable.addCell(createCell("TVA:", FONT_BOLD));
            totalsTable.addCell(createCell(formatBigDecimal(nirNote.getTotalVAT()) + " LEI", FONT_NORMAL));
            
            totalsTable.addCell(createCell("TOTAL:", FONT_BOLD));
            totalsTable.addCell(createCell(formatBigDecimal(nirNote.getTotalValue()) + " LEI", FONT_BOLD));
            
            document.add(totalsTable);
            addParagraph(document, " ", FONT_NORMAL);
            
            // Discrepancies
            if (nirNote.getHasDiscrepancies()) {
                addSubtitle(document, "Constatări și Diferențe:");
                addParagraph(document, nirNote.getDiscrepanciesNotes() != null ? nirNote.getDiscrepanciesNotes() : 
                    "Există diferențe între cantitățile facturate și cele recepționate (vezi tabel).", FONT_NORMAL);
                addParagraph(document, " ", FONT_NORMAL);
            }
            
            // Signatures
            addSubtitle(document, "Comisia de Recepție:");
            PdfPTable signaturesTable = new PdfPTable(3);
            signaturesTable.setWidthPercentage(100);
            signaturesTable.setWidths(new float[]{1, 1, 1});
            
            // Committee member 1
            if (nirNote.getCommittee1Name() != null) {
                PdfPCell cell1 = new PdfPCell();
                cell1.setBorder(Rectangle.NO_BORDER);
                cell1.addElement(new Paragraph(nirNote.getCommittee1Name(), FONT_NORMAL));
                cell1.addElement(new Paragraph("Semnătură: _____________", FONT_SMALL));
                signaturesTable.addCell(cell1);
            } else {
                signaturesTable.addCell(createCell("", FONT_NORMAL));
            }
            
            // Committee member 2
            if (nirNote.getCommittee2Name() != null) {
                PdfPCell cell2 = new PdfPCell();
                cell2.setBorder(Rectangle.NO_BORDER);
                cell2.addElement(new Paragraph(nirNote.getCommittee2Name(), FONT_NORMAL));
                cell2.addElement(new Paragraph("Semnătură: _____________", FONT_SMALL));
                signaturesTable.addCell(cell2);
            } else {
                signaturesTable.addCell(createCell("", FONT_NORMAL));
            }
            
            // Committee member 3
            if (nirNote.getCommittee3Name() != null) {
                PdfPCell cell3 = new PdfPCell();
                cell3.setBorder(Rectangle.NO_BORDER);
                cell3.addElement(new Paragraph(nirNote.getCommittee3Name(), FONT_NORMAL));
                cell3.addElement(new Paragraph("Semnătură: _____________", FONT_SMALL));
                signaturesTable.addCell(cell3);
            } else {
                signaturesTable.addCell(createCell("", FONT_NORMAL));
            }
            
            document.add(signaturesTable);
            addParagraph(document, " ", FONT_NORMAL);
            
            // Warehouse Manager
            if (nirNote.getWarehouseManagerName() != null) {
                addSubtitle(document, "Gestionar:");
                addParagraph(document, nirNote.getWarehouseManagerName(), FONT_NORMAL);
                addParagraph(document, "Semnătură: _____________________", FONT_NORMAL);
            }
            
            // Footer
            addParagraph(document, " ", FONT_NORMAL);
            addParagraph(document, "Document generat: " + formatDateTime(LocalDateTime.now()), FONT_SMALL);
            addParagraph(document, "Status: " + nirNote.getStatus(), FONT_SMALL);
            
            logger.info("Reception note PDF generated successfully: {}", filePath);
            
        } finally {
            document.close();
        }
    }
    
    // Helper methods
    
    private void addTitle(Document document, String text) throws DocumentException {
        Paragraph title = new Paragraph(text, FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
    }
    
    private void addSubtitle(Document document, String text) throws DocumentException {
        Paragraph subtitle = new Paragraph(text, FONT_SUBTITLE);
        subtitle.setSpacingBefore(10);
        subtitle.setSpacingAfter(5);
        document.add(subtitle);
    }
    
    private void addParagraph(Document document, String text, Font font) throws DocumentException {
        Paragraph paragraph = new Paragraph(text, font);
        document.add(paragraph);
    }
    
    private void addTableHeader(PdfPTable table, String[] headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FONT_BOLD));
            cell.setBackgroundColor(new Color(204, 204, 204)); // Gray background
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }
    
    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(3);
        return cell;
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "-";
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
    
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "-";
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
    
    private String formatBigDecimal(BigDecimal value) {
        if (value == null) return "0";
        return value.stripTrailingZeros().toPlainString();
    }
}
