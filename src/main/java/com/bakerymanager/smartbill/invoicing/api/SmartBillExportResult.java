package com.bakerymanager.smartbill.invoicing.api;

/**
 * Result of exporting a reception note (NIR) to SmartBill API v2.
 */
public record SmartBillExportResult(
    boolean success,
    String externalDocumentId,
    String message,
    int attempts,
    int httpStatusCode
) {
}
