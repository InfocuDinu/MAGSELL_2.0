package com.bakerymanager.controller;

import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.ProductService;
import com.bakerymanager.service.AlertService;
import com.bakerymanager.service.Alert;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;

@Controller
public class DashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    private final ProductService productService;
    private final IngredientService ingredientService;
    private final AlertService alertService;
    
    public DashboardController(ProductService productService,
                               IngredientService ingredientService,
                               AlertService alertService) {
        this.productService = productService;
        this.ingredientService = ingredientService;
        this.alertService = alertService;
    }
    
    @FXML
    private Label totalProductsLabel;
    
    @FXML
    private Label lowStockLabel;
    
    @FXML
    private Label todaySalesLabel;
    
    @FXML
    private Label totalSalesLabel;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label lastUpdateLabel;
    
    @FXML
    private TableView<ActivityRecord> activityTable;
    
    @FXML
    private TableColumn<ActivityRecord, String> activityDateColumn;
    
    @FXML
    private TableColumn<ActivityRecord, String> activityTypeColumn;
    
    @FXML
    private TableColumn<ActivityRecord, String> activityDescriptionColumn;
    
    @FXML
    private TableColumn<ActivityRecord, String> activityUserColumn;
    
    private ObservableList<ActivityRecord> activityRecords = FXCollections.observableArrayList();
    
    public static class ActivityRecord {
        private LocalDateTime date;
        private String type;
        private String description;
        private String user;
        
        public ActivityRecord(LocalDateTime date, String type, String description, String user) {
            this.date = date;
            this.type = type;
            this.description = description;
            this.user = user;
        }
        
        public String getFormattedDate() {
            return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }
        
        public LocalDateTime getDate() { return date; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public String getUser() { return user; }
    }
    
    @FXML
    public void initialize() {
        setupActivityTable();
        loadDashboardData();
        showOnboardingIfFirstRun();
        logger.info("Dashboard controller initialized");
    }
    
    private void setupActivityTable() {
        activityDateColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("formattedDate"));
        activityTypeColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("type"));
        activityDescriptionColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("description"));
        activityUserColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("user"));
        
        activityTable.setItems(activityRecords);
    }
    
    private void loadDashboardData() {
        try {
            long productsInStock = productService.countAvailableProducts();
            long lowStockCount = ingredientService.countLowStockIngredients();
            
            totalProductsLabel.setText(String.valueOf(productsInStock));
            lowStockLabel.setText(String.valueOf(lowStockCount));
            todaySalesLabel.setText("0.00 lei");
            totalSalesLabel.setText("0.00 lei");
            statusLabel.setText("Sistem operațional");
            lastUpdateLabel.setText("Ultima actualizare: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            
            activityRecords.add(new ActivityRecord(
                LocalDateTime.now(),
                "System",
                "Dashboard încărcat",
                "Admin"
            ));

            loadAlerts();
            
            logger.info("Dashboard data loaded successfully");
        } catch (Exception e) {
            logger.error("Error loading dashboard data", e);
            totalProductsLabel.setText("--");
            lowStockLabel.setText("--");
            todaySalesLabel.setText("-- lei");
            totalSalesLabel.setText("-- lei");
            statusLabel.setText("Eroare la încărcare");
        }
    }
    
    @FXML
    public void addStock() {
        activityRecords.add(0, new ActivityRecord(
            LocalDateTime.now(),
            "Inventory",
            "Acțiune de adăugare stoc",
            "Admin"
        ));
    }
    
    @FXML
    public void quickProduction() {
        activityRecords.add(0, new ActivityRecord(
            LocalDateTime.now(),
            "Production",
            "Producție rapidă inițiată",
            "Admin"
        ));
    }
    
    @FXML
    public void quickSale() {
        activityRecords.add(0, new ActivityRecord(
            LocalDateTime.now(),
            "Sales",
            "Vânzare rapidă procesată",
            "Admin"
        ));
    }
    
    @FXML
    public void dailyReport() {
        activityRecords.add(0, new ActivityRecord(
            LocalDateTime.now(),
            "Reports",
            "Raport zilnic generat",
            "Admin"
        ));
    }

    @FXML
    public void showOnboarding() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Ghid rapid");
        alert.setHeaderText("Bun venit! Iată pașii de bază:");
        alert.setContentText(
            "1) Încarcă produse și rețete în modulul Producție.\n" +
            "2) Generează NIR și completează lot/expirare.\n" +
            "3) Rulează producția sau ordinele de producție.\n" +
            "4) Vânzări în POS + bon fiscal.\n" +
            "5) Verifică rapoarte și alerte zilnic.\n"
        );
        alert.getDialogPane().setPrefWidth(480);
        alert.show();
    }

    private void showOnboardingIfFirstRun() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(DashboardController.class);
            boolean shown = prefs.getBoolean("onboardingShown", false);
            if (!shown) {
                showOnboarding();
                prefs.putBoolean("onboardingShown", true);
            }
        } catch (Exception e) {
            logger.warn("Unable to load onboarding preference", e);
        }
    }

    private void loadAlerts() {
        try {
            var alerts = alertService.getAlerts(LocalDateTime.now().toLocalDate().minusDays(7), LocalDateTime.now().toLocalDate());
            if (!alerts.isEmpty()) {
                statusLabel.setText("Alerte active: " + alerts.size());
            }

            for (Alert alert : alerts) {
                activityRecords.add(0, new ActivityRecord(
                    alert.getTimestamp(),
                    "Alert - " + alert.getSeverity().name(),
                    alert.getMessage(),
                    "System"
                ));
            }
        } catch (Exception e) {
            logger.warn("Error loading alerts", e);
        }
    }
}
