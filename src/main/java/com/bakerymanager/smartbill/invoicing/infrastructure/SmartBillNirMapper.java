package com.bakerymanager.smartbill.invoicing.infrastructure;

import com.bakerymanager.entity.ReceptionNote;
import com.bakerymanager.entity.ReceptionNoteLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SmartBillNirMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Map<String, Object> toSmartBillRequest(ReceptionNote receptionNote, String companyVatCode) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("companyVatCode", companyVatCode);
        payload.put("documentType", "NIR");
        payload.put("documentNumber", receptionNote.getNirNumber());
        payload.put("documentDate", receptionNote.getNirDate().toLocalDate().format(DATE_FORMATTER));
        payload.put("currency", receptionNote.getInvoice().getCurrency() != null ? receptionNote.getInvoice().getCurrency() : "RON");

        Map<String, Object> supplier = new HashMap<>();
        supplier.put("name", receptionNote.getInvoice().getSupplierName());
        supplier.put("vatCode", receptionNote.getInvoice().getSupplierCui());
        payload.put("supplier", supplier);

        Map<String, Object> totals = new HashMap<>();
        totals.put("withoutVat", safeScale(receptionNote.getTotalValueWithoutVAT()));
        totals.put("vat", safeScale(receptionNote.getTotalVAT()));
        totals.put("total", safeScale(receptionNote.getTotalValue()));
        payload.put("totals", totals);

        payload.put("lines", mapLines(receptionNote.getLines()));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceNumber", receptionNote.getInvoice().getInvoiceNumber());
        metadata.put("receptionDate", receptionNote.getReceptionDate() != null
            ? receptionNote.getReceptionDate().toLocalDate().format(DATE_FORMATTER)
            : null);
        metadata.put("status", receptionNote.getStatus().name());
        metadata.put("hasDiscrepancies", Boolean.TRUE.equals(receptionNote.getHasDiscrepancies()));
        metadata.put("discrepancyNotes", receptionNote.getDiscrepanciesNotes());
        payload.put("metadata", metadata);

        return payload;
    }

    private List<Map<String, Object>> mapLines(List<ReceptionNoteLine> lines) {
        return lines.stream()
            .map(this::mapLine)
            .collect(Collectors.toList());
    }

    private Map<String, Object> mapLine(ReceptionNoteLine line) {
        Map<String, Object> mapped = new HashMap<>();
        mapped.put("name", line.getProductName());
        mapped.put("code", line.getProductCode());
        mapped.put("unit", line.getUnit());
        mapped.put("batch", line.getBatchCode());
        mapped.put("expiryDate", line.getExpiryDate() != null ? line.getExpiryDate().format(DATE_FORMATTER) : null);
        mapped.put("quantity", safeScale(line.getReceivedQuantity(), 3));
        mapped.put("unitPrice", safeScale(line.getUnitPrice()));
        mapped.put("vatRate", safeScale(line.getVatRate()));
        mapped.put("valueWithoutVat", safeScale(line.getValueWithoutVAT()));
        mapped.put("vatAmount", safeScale(line.getVatAmount()));
        mapped.put("totalValue", safeScale(line.getTotalValue()));
        return mapped;
    }

    private BigDecimal safeScale(BigDecimal value) {
        return safeScale(value, 2);
    }

    private BigDecimal safeScale(BigDecimal value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(scale);
        }
        return value.setScale(scale, java.math.RoundingMode.HALF_UP);
    }
}
