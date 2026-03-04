package com.bakerymanager.smartbill.invoicing.infrastructure;

import com.bakerymanager.entity.Invoice;
import com.bakerymanager.entity.ReceptionNote;
import com.bakerymanager.entity.ReceptionNoteLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmartBillNirBusinessValidatorTest {

    private final SmartBillNirBusinessValidator validator = new SmartBillNirBusinessValidator();

    @Test
    void shouldFailWhenNirIsDraft() {
        ReceptionNote note = validReceptionNote();
        note.setStatus(ReceptionNote.NirStatus.DRAFT);

        assertThrows(SmartBillBusinessValidationException.class, () -> validator.validate(note));
    }

    @Test
    void shouldFailWhenLineQuantityIsInvalid() {
        ReceptionNote note = validReceptionNote();
        note.getLines().get(0).setReceivedQuantity(BigDecimal.ZERO);

        assertThrows(SmartBillBusinessValidationException.class, () -> validator.validate(note));
    }

    @Test
    void shouldPassForValidReceptionNote() {
        ReceptionNote note = validReceptionNote();
        assertDoesNotThrow(() -> validator.validate(note));
    }

    private ReceptionNote validReceptionNote() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("FAC-001");
        invoice.setSupplierName("Furnizor Test");
        invoice.setSupplierCui("RO123456");
        invoice.setCurrency("RON");

        ReceptionNote note = new ReceptionNote();
        note.setNirNumber("NIR-20260304-0001");
        note.setNirDate(LocalDateTime.now());
        note.setReceptionDate(LocalDateTime.now());
        note.setStatus(ReceptionNote.NirStatus.APPROVED);
        note.setInvoice(invoice);

        ReceptionNoteLine line = new ReceptionNoteLine();
        line.setProductName("Făină");
        line.setUnit("KG");
        line.setReceivedQuantity(new BigDecimal("10.000"));
        line.setInvoicedQuantity(new BigDecimal("10.000"));
        line.setUnitPrice(new BigDecimal("3.50"));
        line.setVatRate(new BigDecimal("19"));
        line.calculateValues();

        note.addLine(line);
        note.calculateTotals();

        return note;
    }
}
