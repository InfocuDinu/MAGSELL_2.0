package com.bakerymanager.service;

import com.bakerymanager.entity.Sale;
import com.bakerymanager.entity.SaleItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Mock implementation of Fiscal Printer Service
 * This is a placeholder implementation for development/testing
 * Replace with actual fiscal printer driver for production use
 * 
 * Romanian fiscal printer standards:
 * - Must comply with ANAF regulations
 * - Support DATECS, TREMOL, or other certified printers
 * - Generate XML reports as required by law
 */
@Service
public class MockFiscalPrinterService implements FiscalPrinterService {
    
    private static final Logger logger = LoggerFactory.getLogger(MockFiscalPrinterService.class);
    
    private boolean initialized = false;
    private String lastError = null;
    
    public MockFiscalPrinterService() {
        // Auto-initialize for mock service
        initialize();
    }
    
    @Override
    public boolean printReceipt(Sale sale) {
        if (!isReady()) {
            lastError = "Printer not initialized";
            logger.error("Cannot print receipt: {}", lastError);
            return false;
        }
        
        try {
            // Build receipt content
            StringBuilder receipt = new StringBuilder();
            receipt.append("========================================\n");
            receipt.append("       MAGSELL 2.0 - PATISERIE        \n");
            receipt.append("========================================\n");
            receipt.append("\n");
            receipt.append("BON FISCAL\n");
            receipt.append("Nr: ").append(sale.getInvoiceNumber()).append("\n");
            receipt.append("Data: ").append(sale.getSaleDate().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))).append("\n");
            receipt.append("----------------------------------------\n");
            
            // Items
            if (sale.getSaleItems() != null) {
                for (SaleItem item : sale.getSaleItems()) {
                    receipt.append(String.format("%-20s %5.2f x %7.2f\n",
                        truncate(item.getProductName(), 20),
                        item.getQuantity(),
                        item.getUnitPrice()));
                    receipt.append(String.format("%35s %8.2f\n", "", item.getTotalPrice()));
                }
            }
            
            receipt.append("----------------------------------------\n");
            receipt.append(String.format("TOTAL:%30.2f LEI\n", sale.getTotalAmount()));
            receipt.append("\n");
            receipt.append("Plată: ").append(sale.getPaymentMethod()).append("\n");
            
            if (sale.getCashReceived() != null && sale.getCashReceived().compareTo(sale.getTotalAmount()) > 0) {
                receipt.append(String.format("Primit:%29.2f LEI\n", sale.getCashReceived()));
                receipt.append(String.format("Rest:%32.2f LEI\n", sale.getChangeAmount()));
            }
            
            receipt.append("\n");
            receipt.append("========================================\n");
            receipt.append("    Mulțumim pentru achiziție!        \n");
            receipt.append("========================================\n");
            
            // Mock printing - just log the receipt
            logger.info("FISCAL RECEIPT PRINTED (MOCK):\n{}", receipt);
            
            return true;
            
        } catch (Exception e) {
            lastError = "Error printing receipt: " + e.getMessage();
            logger.error("Error printing fiscal receipt", e);
            return false;
        }
    }
    
    @Override
    public boolean printNonFiscal(String content) {
        if (!isReady()) {
            lastError = "Printer not initialized";
            return false;
        }
        
        logger.info("NON-FISCAL PRINT (MOCK):\n{}", content);
        return true;
    }
    
    @Override
    public boolean isReady() {
        return initialized;
    }
    
    @Override
    public String getStatus() {
        if (initialized) {
            return "Mock Fiscal Printer - Ready (Development Mode)";
        } else {
            return "Mock Fiscal Printer - Not Initialized";
        }
    }
    
    @Override
    public boolean initialize() {
        try {
            logger.info("Initializing Mock Fiscal Printer Service");
            logger.warn("WARNING: Using MOCK fiscal printer - replace with real driver for production!");
            initialized = true;
            lastError = null;
            return true;
        } catch (Exception e) {
            lastError = "Initialization failed: " + e.getMessage();
            logger.error("Failed to initialize mock printer", e);
            return false;
        }
    }
    
    @Override
    public void close() {
        logger.info("Closing Mock Fiscal Printer Service");
        initialized = false;
    }
    
    @Override
    public String getLastError() {
        return lastError;
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
    }
}
