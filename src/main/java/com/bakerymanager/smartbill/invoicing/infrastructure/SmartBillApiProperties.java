package com.bakerymanager.smartbill.invoicing.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "smartbill.api")
public class SmartBillApiProperties {

    private String baseUrl = "https://ws.smartbill.ro/SBORO/api";
    private String stockDocumentPath = "/stock/v2/documents";
    private String email;
    private String token;
    private String companyVatCode;
    private int retryMaxAttempts = 3;
    private long retryInitialDelayMs = 1000;
    private long timeoutMs = 10000;

    public boolean isConfigured() {
        return isNotBlank(baseUrl)
            && isNotBlank(stockDocumentPath)
            && isNotBlank(email)
            && isNotBlank(token)
            && isNotBlank(companyVatCode);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getStockDocumentPath() {
        return stockDocumentPath;
    }

    public void setStockDocumentPath(String stockDocumentPath) {
        this.stockDocumentPath = stockDocumentPath;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCompanyVatCode() {
        return companyVatCode;
    }

    public void setCompanyVatCode(String companyVatCode) {
        this.companyVatCode = companyVatCode;
    }

    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public void setRetryMaxAttempts(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    public long getRetryInitialDelayMs() {
        return retryInitialDelayMs;
    }

    public void setRetryInitialDelayMs(long retryInitialDelayMs) {
        this.retryInitialDelayMs = retryInitialDelayMs;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
