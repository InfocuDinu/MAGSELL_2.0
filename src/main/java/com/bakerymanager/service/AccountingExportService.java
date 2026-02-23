package com.bakerymanager.service;

import com.bakerymanager.entity.Sale;
import com.bakerymanager.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AccountingExportService {

    private final SaleRepository saleRepository;

    public AccountingExportService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public void exportSalesCsv(LocalDateTime startDate, LocalDateTime endDate, String filePath) throws IOException {
        List<Sale> sales = saleRepository.findBySaleDateBetween(startDate, endDate);

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("Invoice,Date,Total,PaymentMethod,Customer,Operator\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Sale sale : sales) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s\n",
                    safe(sale.getInvoiceNumber()),
                    sale.getSaleDate() != null ? sale.getSaleDate().format(formatter) : "",
                    safe(sale.getTotalAmount()),
                    safe(sale.getPaymentMethod()),
                    safe(sale.getCustomerName()),
                    safe(sale.getOperator())
                ));
            }
        }
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).replace(",", " ");
    }
}
