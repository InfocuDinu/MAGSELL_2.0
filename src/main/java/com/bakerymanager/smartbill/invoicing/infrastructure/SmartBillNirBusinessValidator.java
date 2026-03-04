package com.bakerymanager.smartbill.invoicing.infrastructure;

import com.bakerymanager.entity.ReceptionNote;
import com.bakerymanager.entity.ReceptionNoteLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class SmartBillNirBusinessValidator {

    public void validate(ReceptionNote receptionNote) {
        List<String> errors = new ArrayList<>();

        if (receptionNote == null) {
            throw new SmartBillBusinessValidationException("NIR lipsește.");
        }

        if (receptionNote.getStatus() == ReceptionNote.NirStatus.DRAFT) {
            errors.add("NIR trebuie să fie minim aprobat (nu DRAFT).");
        }

        if (receptionNote.getInvoice() == null) {
            errors.add("NIR trebuie să aibă factură asociată.");
        } else {
            if (isBlank(receptionNote.getInvoice().getInvoiceNumber())) {
                errors.add("Factura asociată nu are număr.");
            }
            if (isBlank(receptionNote.getInvoice().getSupplierName())) {
                errors.add("Factura asociată nu are furnizor.");
            }
        }

        if (receptionNote.getLines() == null || receptionNote.getLines().isEmpty()) {
            errors.add("NIR trebuie să conțină cel puțin o linie.");
        } else {
            int index = 1;
            for (ReceptionNoteLine line : receptionNote.getLines()) {
                if (isBlank(line.getProductName())) {
                    errors.add("Linia " + index + " nu are denumire produs.");
                }
                if (line.getReceivedQuantity() == null || line.getReceivedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Linia " + index + " are cantitate recepționată invalidă.");
                }
                if (line.getUnitPrice() == null || line.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Linia " + index + " are preț unitar invalid.");
                }
                if (line.getVatRate() == null || line.getVatRate().compareTo(BigDecimal.ZERO) < 0
                    || line.getVatRate().compareTo(BigDecimal.valueOf(100)) > 0) {
                    errors.add("Linia " + index + " are cotă TVA invalidă.");
                }
                if (isBlank(line.getUnit())) {
                    errors.add("Linia " + index + " nu are unitate de măsură.");
                }
                index++;
            }
        }

        if (receptionNote.getTotalValue() == null || receptionNote.getTotalValue().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Totalul NIR trebuie să fie mai mare decât 0.");
        }

        if (!errors.isEmpty()) {
            throw new SmartBillBusinessValidationException(String.join(" ", errors));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
