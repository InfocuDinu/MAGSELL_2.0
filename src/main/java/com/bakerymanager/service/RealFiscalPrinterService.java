package com.bakerymanager.service;

import com.bakerymanager.entity.Sale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Real fiscal printer integration stub.
 * Enable with Spring profile: fiscal-real
 */
@Service
@Profile("fiscal-real")
public class RealFiscalPrinterService implements FiscalPrinterService {

    private static final Logger logger = LoggerFactory.getLogger(RealFiscalPrinterService.class);

    private boolean initialized;
    private String lastError;

    @Override
    public boolean printReceipt(Sale sale) {
        lastError = "Real fiscal printer not configured.";
        logger.warn(lastError);
        return false;
    }

    @Override
    public boolean printNonFiscal(String content) {
        lastError = "Real fiscal printer not configured.";
        logger.warn(lastError);
        return false;
    }

    @Override
    public boolean isReady() {
        return initialized;
    }

    @Override
    public String getStatus() {
        return initialized ? "Real Fiscal Printer - Ready" : "Real Fiscal Printer - Not configured";
    }

    @Override
    public boolean initialize() {
        initialized = false;
        lastError = "Real fiscal printer not configured.";
        logger.warn(lastError);
        return false;
    }

    @Override
    public void close() {
        initialized = false;
    }

    @Override
    public String getLastError() {
        return lastError;
    }
}
