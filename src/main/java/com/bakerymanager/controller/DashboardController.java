package com.bakerymanager.controller;

import com.bakerymanager.service.IngredientService;
import com.bakerymanager.service.ProductService;
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

@Controller
public class DashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    private final ProductService productService;
    private final IngredientService ingredientService;
    
    public DashboardController(ProductService productService, IngredientService ingredientService) {
        this.productService = productService;
        this.ingredientService = ingredientService;
    }
    
    @FXML
    private Label productsInStockLabel;
    
    @FXML
    private Label ingredientsInStockLabel;
    
    @FXML
    private Label lowStockLabel;
    
    @FXML
    private Label todaySalesLabel;
    
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
        long productsInStock = productService.countAvailableProducts();
        long ingredientsInStock = ingredientService.countAvailableIngredients();
        long lowStockCount = ingredientService.countLowStockIngredients();
        
        productsInStockLabel.setText(String.valueOf(productsInStock));
        ingredientsInStockLabel.setText(String.valueOf(ingredientsInStock));
        lowStockLabel.setText(String.valueOf(lowStockCount));
        todaySalesLabel.setText("0.00 lei");
        
        activityRecords.add(new ActivityRecord(
            LocalDateTime.now(),
            "System",
            "Dashboard încărcat",
            "Admin"
        ));
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
}
