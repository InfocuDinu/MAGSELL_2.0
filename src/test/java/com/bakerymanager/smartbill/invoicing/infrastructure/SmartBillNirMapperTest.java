package com.bakerymanager.smartbill.invoicing.infrastructure;

import com.bakerymanager.entity.Invoice;
import com.bakerymanager.entity.ReceptionNote;
import com.bakerymanager.entity.ReceptionNoteLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartBillNirMapperTest {

    private final SmartBillNirMapper mapper = new SmartBillNirMapper();

    @Test
    void shouldMapReceptionNoteToSmartBillPayload() {
        ReceptionNote note = buildReceptionNote();

        Map<String, Object> payload = mapper.toSmartBillRequest(note, "RO99999999");

        assertEquals("RO99999999", payload.get("companyVatCode"));
        assertEquals("NIR", payload.get("documentType"));
        assertEquals(note.getNirNumber(), payload.get("documentNumber"));

        Object linesObject = payload.get("lines");
        assertTrue(linesObject instanceof List<?>);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) linesObject;
        assertEquals(1, lines.size());
        assertEquals("Drojdia", lines.get(0).get("name"));
        assertEquals("LOT-01", lines.get(0).get("batch"));

        @SuppressWarnings("unchecked")
        Map<String, Object> totals = (Map<String, Object>) payload.get("totals");
        assertNotNull(totals);
        assertEquals(new BigDecimal("100.00"), totals.get("withoutVat"));
        assertEquals(new BigDecimal("19.00"), totals.get("vat"));
        assertEquals(new BigDecimal("119.00"), totals.get("total"));
    }

    private ReceptionNote buildReceptionNote() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("FAC-777");
        invoice.setSupplierName("Supplier SRL");
        invoice.setSupplierCui("RO1111");
        invoice.setCurrency("RON");

        ReceptionNote note = new ReceptionNote();
        note.setInvoice(invoice);
        note.setNirNumber("NIR-777");
        note.setNirDate(LocalDateTime.of(2026, 3, 4, 10, 0));
        note.setReceptionDate(LocalDateTime.of(2026, 3, 4, 10, 0));
        note.setStatus(ReceptionNote.NirStatus.SIGNED);

        ReceptionNoteLine line = new ReceptionNoteLine();
        line.setProductName("Drojdia");
        line.setProductCode("DRJ");
        line.setUnit("KG");
        line.setBatchCode("LOT-01");
        line.setExpiryDate(LocalDate.of(2026, 6, 1));
        line.setInvoicedQuantity(new BigDecimal("10.000"));
        line.setReceivedQuantity(new BigDecimal("10.000"));
        line.setUnitPrice(new BigDecimal("10.00"));
        line.setVatRate(new BigDecimal("19"));
        line.calculateValues();

        note.addLine(line);

        note.setTotalValueWithoutVAT(new BigDecimal("100.00"));
        note.setTotalVAT(new BigDecimal("19.00"));
        note.setTotalValue(new BigDecimal("119.00"));

        return note;
    }
}
