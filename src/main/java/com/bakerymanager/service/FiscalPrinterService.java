package com.bakerymanager.service;

import com.bakerymanager.entity.Sale;

/**
 * Interface for fiscal printer integration
 * Supports Romanian fiscal printer protocols
 */
public interface FiscalPrinterService {
    
    /**
     * Print fiscal receipt for a sale
     * 
     * @param sale The sale to print receipt for
     * @return true if printing was successful, false otherwise
     */
    boolean printReceipt(Sale sale);
    
    /**
     * Print non-fiscal document (e.g., reports, invoices)
     * 
     * @param content The content to print
     * @return true if printing was successful
     */
    boolean printNonFiscal(String content);
    
    /**
     * Check if printer is connected and ready
     * 
     * @return true if printer is ready
     */
    boolean isReady();
    
    /**
     * Get printer status information
     * 
     * @return Status description string
     */
    String getStatus();
    
    /**
     * Initialize printer connection
     * 
     * @return true if initialization was successful
     */
    boolean initialize();
    
    /**
     * Close printer connection
     */
    void close();
    
    /**
     * Get last error message
     * 
     * @return Error description or null if no error
     */
    String getLastError();
}
