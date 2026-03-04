package com.bakerymanager.smartbill.invoicing.application;

import com.bakerymanager.entity.Invoice;
import com.bakerymanager.entity.InvoiceLine;
import com.bakerymanager.entity.ReceptionNote;
import com.bakerymanager.service.InvoiceService;
import com.bakerymanager.service.PdfService;
import com.bakerymanager.service.ReceptionNoteService;
import com.bakerymanager.smartbill.invoicing.api.InvoicingFacade;
import com.bakerymanager.smartbill.invoicing.api.SmartBillExportResult;
import com.lowagie.text.DocumentException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class InvoicingFacadeImpl implements InvoicingFacade {

    private final InvoiceService invoiceService;
    private final ReceptionNoteService receptionNoteService;
    private final PdfService pdfService;
    private final SmartBillNirExportService smartBillNirExportService;

    public InvoicingFacadeImpl(InvoiceService invoiceService,
                               ReceptionNoteService receptionNoteService,
                               PdfService pdfService,
                               SmartBillNirExportService smartBillNirExportService) {
        this.invoiceService = invoiceService;
        this.receptionNoteService = receptionNoteService;
        this.pdfService = pdfService;
        this.smartBillNirExportService = smartBillNirExportService;
    }

    @Override
    public List<Invoice> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }

    @Override
    public Invoice saveImportedInvoice(Invoice invoice) {
        return invoiceService.saveInvoice(invoice);
    }

    @Override
    public Invoice saveManualInvoiceWithLines(Invoice invoice, List<InvoiceLine> lines) {
        return invoiceService.saveInvoiceWithLines(invoice, lines);
    }

    @Override
    public ReceptionNote createReceptionNoteFromInvoice(Long invoiceId, String companyName, String companyAddress) {
        return receptionNoteService.createFromInvoice(invoiceId, companyName, companyAddress);
    }

    @Override
    public List<ReceptionNote> getAllReceptionNotes() {
        return receptionNoteService.getAllReceptionNotes();
    }

    @Override
    public ReceptionNote saveReceptionNote(ReceptionNote receptionNote) {
        return receptionNoteService.saveReceptionNote(receptionNote);
    }

    @Override
    public ReceptionNote getReceptionNoteForExport(Long receptionNoteId) {
        return receptionNoteService.getReceptionNoteForPdf(receptionNoteId);
    }

    @Override
    public void exportReceptionNotePdf(Long receptionNoteId, String filePath) throws IOException, DocumentException {
        ReceptionNote receptionNote = getReceptionNoteForExport(receptionNoteId);
        pdfService.generateReceptionNotePdf(receptionNote, filePath);
    }

    @Override
    public SmartBillExportResult exportReceptionNoteToSmartBill(Long receptionNoteId) {
        return smartBillNirExportService.exportReceptionNote(receptionNoteId);
    }
}