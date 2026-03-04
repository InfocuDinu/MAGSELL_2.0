package com.bakerymanager.smartbill.invoicing.application;

import com.bakerymanager.entity.ReceptionNote;
import com.bakerymanager.service.ReceptionNoteService;
import com.bakerymanager.smartbill.invoicing.api.SmartBillExportResult;
import com.bakerymanager.smartbill.invoicing.infrastructure.SmartBillApiClient;
import com.bakerymanager.smartbill.invoicing.infrastructure.SmartBillApiProperties;
import com.bakerymanager.smartbill.invoicing.infrastructure.SmartBillNirBusinessValidator;
import com.bakerymanager.smartbill.invoicing.infrastructure.SmartBillNirMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class SmartBillNirExportService {

    private final ReceptionNoteService receptionNoteService;
    private final SmartBillNirBusinessValidator businessValidator;
    private final SmartBillNirMapper mapper;
    private final SmartBillApiClient apiClient;
    private final SmartBillApiProperties properties;

    public SmartBillNirExportService(ReceptionNoteService receptionNoteService,
                                     SmartBillNirBusinessValidator businessValidator,
                                     SmartBillNirMapper mapper,
                                     SmartBillApiClient apiClient,
                                     SmartBillApiProperties properties) {
        this.receptionNoteService = receptionNoteService;
        this.businessValidator = businessValidator;
        this.mapper = mapper;
        this.apiClient = apiClient;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public SmartBillExportResult exportReceptionNote(Long receptionNoteId) {
        ReceptionNote receptionNote = receptionNoteService.getReceptionNoteForPdf(receptionNoteId);
        businessValidator.validate(receptionNote);

        Map<String, Object> payload = mapper.toSmartBillRequest(receptionNote, properties.getCompanyVatCode());
        SmartBillApiClient.SmartBillApiCallResult callResult = apiClient.sendNirDocument(payload);

        return new SmartBillExportResult(
            true,
            callResult.documentId(),
            callResult.message(),
            callResult.attempts(),
            callResult.statusCode()
        );
    }
}
