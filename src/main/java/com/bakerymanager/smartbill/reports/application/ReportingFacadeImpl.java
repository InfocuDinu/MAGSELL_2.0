package com.bakerymanager.smartbill.reports.application;

import com.bakerymanager.entity.Ingredient;
import com.bakerymanager.entity.Product;
import com.bakerymanager.smartbill.reports.api.ReportingFacade;
import com.bakerymanager.smartbill.reports.domain.ReportingPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportingFacadeImpl implements ReportingFacade {

    private final ReportingPort reportingPort;

    public ReportingFacadeImpl(ReportingPort reportingPort) {
        this.reportingPort = reportingPort;
    }

    @Override
    public List<Product> getAvailableProducts() {
        return reportingPort.getAvailableProducts();
    }

    @Override
    public List<Ingredient> getAllIngredients() {
        return reportingPort.getAllIngredients();
    }

    @Override
    public List<Ingredient> getLowStockIngredients() {
        return reportingPort.getLowStockIngredients();
    }
}
