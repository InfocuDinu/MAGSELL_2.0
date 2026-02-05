package com.bakerymanager.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JacksonXmlRootElement(localName = "Invoice")
public class UBLInvoiceDto {
    
    @JacksonXmlProperty(localName = "cbc:ID")
    private String invoiceNumber;
    
    @JacksonXmlProperty(localName = "cbc:IssueDate")
    private String issueDate;
    
    @JacksonXmlProperty(localName = "cbc:IssueTime")
    private String issueTime;
    
    @JacksonXmlProperty(localName = "cbc:DocumentCurrencyCode")
    private String currency;
    
    @JacksonXmlProperty(localName = "cac:AccountingSupplierParty")
    private SupplierParty supplierParty;
    
    @JacksonXmlProperty(localName = "cac:LegalMonetaryTotal")
    private MonetaryTotal monetaryTotal;
    
    @JacksonXmlProperty(localName = "cac:InvoiceLine")
    private List<InvoiceLineDto> invoiceLines;
    
    @Data
    public static class SupplierParty {
        @JacksonXmlProperty(localName = "cac:Party")
        private Party party;
    }
    
    @Data
    public static class Party {
        @JacksonXmlProperty(localName = "cac:PartyName")
        private List<PartyName> partyNames;
        
        @JacksonXmlProperty(localName = "cac:PartyLegalEntity")
        private PartyLegalEntity legalEntity;
    }
    
    @Data
    public static class PartyName {
        @JacksonXmlProperty(localName = "cbc:Name")
        private String name;
    }
    
    @Data
    public static class PartyLegalEntity {
        @JacksonXmlProperty(localName = "cbc:CompanyID")
        private String companyID;
    }
    
    @Data
    public static class MonetaryTotal {
        @JacksonXmlProperty(localName = "cbc:TaxInclusiveAmount")
        private TaxInclusiveAmount taxInclusiveAmount;
    }
    
    @Data
    public static class TaxInclusiveAmount {
        @JacksonXmlProperty(isAttribute = true)
        private BigDecimal value;
    }
    
    @Data
    public static class InvoiceLineDto {
        @JacksonXmlProperty(localName = "cbc:ID")
        private String id;
        
        @JacksonXmlProperty(localName = "cbc:InvoicedQuantity")
        private InvoicedQuantity quantity;
        
        @JacksonXmlProperty(localName = "cac:Price")
        private Price price;
        
        @JacksonXmlProperty(localName = "cac:Item")
        private Item item;
    }
    
    @Data
    public static class InvoicedQuantity {
        @JacksonXmlProperty(isAttribute = true)
        private BigDecimal value;
        
        @JacksonXmlProperty(isAttribute = true, localName = "unitCode")
        private String unitCode;
    }
    
    @Data
    public static class Price {
        @JacksonXmlProperty(localName = "cbc:PriceAmount")
        private PriceAmount priceAmount;
    }
    
    @Data
    public static class PriceAmount {
        @JacksonXmlProperty(isAttribute = true)
        private BigDecimal value;
    }
    
    @Data
    public static class Item {
        @JacksonXmlProperty(localName = "cbc:Name")
        private String name;
        
        @JacksonXmlProperty(localName = "cac:SellersItemIdentification")
        private SellersItemIdentification sellersItemIdentification;
    }
    
    @Data
    public static class SellersItemIdentification {
        @JacksonXmlProperty(localName = "cbc:ID")
        private String id;
    }
}
