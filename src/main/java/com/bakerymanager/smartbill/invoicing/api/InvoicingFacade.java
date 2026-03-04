package com.bakerymanager.smartbill.invoicing.api;

import com.bakerymanager.entity.Invoice;
import com.bakerymanager.entity.InvoiceLine;
import com.bakerymanager.entity.ReceptionNote;
import com.bakerymanager.smartbill.invoicing.api.SmartBillExportResult;
import com.lowagie.text.DocumentException;

import java.io.IOException;
import java.util.List;

/**
 * SmartBill-like module boundary for invoicing and reception note workflows.
 *
 * Controller/UI should depend on this API instead of wiring multiple services directly.
 */
public interface InvoicingFacade {

    List<Invoice> getAllInvoices();

    Invoice saveImportedInvoice(Invoice invoice);

    Invoice saveManualInvoiceWithLines(Invoice invoice, List<InvoiceLine> lines);

    ReceptionNote createReceptionNoteFromInvoice(Long invoiceId, String companyName, String companyAddress);

    List<ReceptionNote> getAllReceptionNotes();

    ReceptionNote saveReceptionNote(ReceptionNote receptionNote);

    ReceptionNote getReceptionNoteForExport(Long receptionNoteId);

    void exportReceptionNotePdf(Long receptionNoteId, String filePath) throws IOException, DocumentException;

    SmartBillExportResult exportReceptionNoteToSmartBill(Long receptionNoteId);
}