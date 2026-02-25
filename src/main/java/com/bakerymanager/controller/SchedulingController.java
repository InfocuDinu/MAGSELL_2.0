package com.bakerymanager.controller;

import com.bakerymanager.entity.*;
import com.bakerymanager.smartbill.production.api.ProductionFacade;
import com.bakerymanager.smartbill.production.api.SchedulingFacade;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SchedulingController {
    
    private final SchedulingFacade schedulingFacade;
    private final ProductionFacade productionFacade;
    
    // UI Components
    private TextField shiftNameField;
    private DatePicker shiftDatePicker;
    private Spinner<Integer> shiftStartHourSpinner;
    private Spinner<Integer> shiftStartMinuteSpinner;
    private Spinner<Integer> shiftEndHourSpinner;
    private Spinner<Integer> shiftEndMinuteSpinner;
    private ComboBox<ProductionShift.ResourceType> resourceTypeCombo;
    private TextField resourceIdField;
    private TextField capacityUnitsField;
    private TableView<ProductionShiftRow> shiftsTable;
    private TableView<ScheduleEntryRow> scheduleTable;
    private Label utilizationLabel;
    private ProgressBar utilizationBar;
    
    public SchedulingController(SchedulingFacade schedulingFacade, ProductionFacade productionFacade) {
        this.schedulingFacade = schedulingFacade;
        this.productionFacade = productionFacade;
    }
    
    public AnchorPane createSchedulingView() {
        AnchorPane root = new AnchorPane();
        root.setPrefSize(1200, 700);
        
        // Split pane: left for shifts, right for scheduling
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.4);
        
        // Left side: Shift Management
        VBox leftPane = createShiftManagementPane();
        
        // Right side: Schedule Overview
        VBox rightPane = createScheduleOverviewPane();
        
        splitPane.getItems().addAll(leftPane, rightPane);
        
        AnchorPane.setTopAnchor(splitPane, 0.0);
        AnchorPane.setBottomAnchor(splitPane, 0.0);
        AnchorPane.setLeftAnchor(splitPane, 0.0);
        AnchorPane.setRightAnchor(splitPane, 0.0);
        
        root.getChildren().add(splitPane);
        return root;
    }
    
    private VBox createShiftManagementPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(15));
        pane.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1;");
        
        // Title
        Label titleLabel = new Label("Administrare Schimburi");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        
        // Shift Creation Form
        TitledPane shiftFormBox = createShiftFormBox();
        
        // Shifts Table
        shiftsTable = createShiftsTable();
        
        ScrollPane tableScrollPane = new ScrollPane(shiftsTable);
        tableScrollPane.setFitToWidth(true);
        tableScrollPane.setPrefHeight(400);
        
        pane.getChildren().addAll(
            titleLabel,
            new Separator(),
            shiftFormBox,
            new Label("Schimburi active:"),
            tableScrollPane
        );
        
        return pane;
    }
    
    private TitledPane createShiftFormBox() {
        TitledPane box = new TitledPane();
        box.setText("Adăugare Schimb Nou");
        box.setCollapsible(true);
        VBox content = new VBox(8);
        content.setPadding(new Insets(10));
        
        // Shift Name
        HBox nameBox = new HBox(5);
        nameBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        nameBox.getChildren().addAll(
            new Label("Nume schimb:"),
            shiftNameField = new TextField()
        );
        shiftNameField.setPrefWidth(150);
        
        // Date and Time
        HBox dateTimeBox = new HBox(10);
        dateTimeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        shiftDatePicker = new DatePicker(LocalDate.now());
        Label startTimeLabel = new Label("Oră start:");
        shiftStartHourSpinner = createHourSpinner(8);
        shiftStartMinuteSpinner = createMinuteSpinner(0);
        
        Label endTimeLabel = new Label("Oră final:");
        shiftEndHourSpinner = createHourSpinner(16);
        shiftEndMinuteSpinner = createMinuteSpinner(0);
        
        dateTimeBox.getChildren().addAll(
            new Label("Data:"), shiftDatePicker,
            startTimeLabel, shiftStartHourSpinner, new Label(":"), shiftStartMinuteSpinner,
            endTimeLabel, shiftEndHourSpinner, new Label(":"), shiftEndMinuteSpinner
        );
        
        // Resource Type and ID
        HBox resourceBox = new HBox(5);
        resourceBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        resourceTypeCombo = new ComboBox<>(FXCollections.observableArrayList(ProductionShift.ResourceType.values()));
        resourceTypeCombo.setPrefWidth(150);
        resourceIdField = new TextField();
        resourceIdField.setPromptText("e.g., OVEN_1");
        resourceIdField.setPrefWidth(100);
        
        resourceBox.getChildren().addAll(
            new Label("Tip resursă:"), resourceTypeCombo,
            new Label("ID resursă:"), resourceIdField
        );
        
        // Capacity
        HBox capacityBox = new HBox(5);
        capacityBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        capacityUnitsField = new TextField("100");
        capacityUnitsField.setPrefWidth(80);
        
        capacityBox.getChildren().addAll(
            new Label("Capacitate (unități):"), capacityUnitsField
        );
        
        // Create Button
        Button createButton = new Button("Creare Schimb");
        createButton.setStyle("-fx-font-size: 11; -fx-padding: 5 20;");
        createButton.setOnAction(e -> handleCreateShift());
        
        content.getChildren().addAll(
            nameBox,
            dateTimeBox,
            resourceBox,
            capacityBox,
            createButton
        );
        
        box.setContent(content);
        return box;
    }
    
    private TableView<ProductionShiftRow> createShiftsTable() {
        TableView<ProductionShiftRow> table = new TableView<>();
        
        TableColumn<ProductionShiftRow, String> nameCol = new TableColumn<>("Nume");
        nameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().shiftName));
        nameCol.setPrefWidth(100);
        
        TableColumn<ProductionShiftRow, String> resourceCol = new TableColumn<>("Resursă");
        resourceCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            param.getValue().resourceType + " (" + param.getValue().resourceId + ")"));
        resourceCol.setPrefWidth(120);
        
        TableColumn<ProductionShiftRow, String> timeRangeCol = new TableColumn<>("Interval Orar");
        timeRangeCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            param.getValue().shiftStart + " - " + param.getValue().shiftEnd));
        timeRangeCol.setPrefWidth(150);
        
        TableColumn<ProductionShiftRow, String> capacityCol = new TableColumn<>("Capacitate");
        capacityCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            param.getValue().capacityUnits + " / " + param.getValue().allocatedUnits));
        capacityCol.setPrefWidth(100);
        
        TableColumn<ProductionShiftRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().status));
        statusCol.setPrefWidth(80);
        
        table.getColumns().addAll(nameCol, resourceCol, timeRangeCol, capacityCol, statusCol);
        table.setPrefHeight(300);
        
        refreshShiftsTable();
        
        return table;
    }
    
    private VBox createScheduleOverviewPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(15));
        
        // Title
        Label titleLabel = new Label("Plan de Producție");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        
        // Date Range for Overview
        HBox dateRangeBox = new HBox(10);
        dateRangeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        DatePicker endDatePicker = new DatePicker(LocalDate.now().plusDays(7));
        Button refreshButton = new Button("Reîncărcare");
        refreshButton.setOnAction(e -> refreshScheduleOverview(startDatePicker.getValue(), endDatePicker.getValue()));
        
        dateRangeBox.getChildren().addAll(
            new Label("De la:"), startDatePicker,
            new Label("Până la:"), endDatePicker,
            refreshButton
        );
        
        // Utilization Indicator
        HBox utilizationBox = new HBox(10);
        utilizationBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 10;");
        utilizationBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        utilizationLabel = new Label("Utilizare: 0%");
        utilizationBar = new ProgressBar(0.0);
        utilizationBar.setPrefWidth(200);
        
        utilizationBox.getChildren().addAll(utilizationLabel, utilizationBar);
        
        // Schedule Table
        scheduleTable = createScheduleTable();
        
        ScrollPane tableScrollPane = new ScrollPane(scheduleTable);
        tableScrollPane.setFitToWidth(true);
        tableScrollPane.setPrefHeight(400);
        
        pane.getChildren().addAll(
            titleLabel,
            new Separator(),
            dateRangeBox,
            utilizationBox,
            new Label("Intrări planificate:"),
            tableScrollPane
        );
        
        return pane;
    }
    
    private TableView<ScheduleEntryRow> createScheduleTable() {
        TableView<ScheduleEntryRow> table = new TableView<>();
        
        TableColumn<ScheduleEntryRow, String> productCol = new TableColumn<>("Produs");
        productCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().productName));
        productCol.setPrefWidth(100);
        
        TableColumn<ScheduleEntryRow, String> quantityCol = new TableColumn<>("Cantitate");
        quantityCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().allocatedQuantity));
        quantityCol.setPrefWidth(80);
        
        TableColumn<ScheduleEntryRow, String> shiftCol = new TableColumn<>("Schimb");
        shiftCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().shiftName));
        shiftCol.setPrefWidth(100);
        
        TableColumn<ScheduleEntryRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().status));
        statusCol.setPrefWidth(80);
        
        TableColumn<ScheduleEntryRow, String> priorityCol = new TableColumn<>("Prioritate");
        priorityCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(param.getValue().priority));
        priorityCol.setPrefWidth(70);
        
        table.getColumns().addAll(productCol, quantityCol, shiftCol, statusCol, priorityCol);
        table.setPrefHeight(300);
        
        return table;
    }
    
    private Spinner<Integer> createHourSpinner(int initialValue) {
        Spinner<Integer> spinner = new Spinner<>(0, 23, initialValue);
        spinner.setPrefWidth(60);
        return spinner;
    }
    
    private Spinner<Integer> createMinuteSpinner(int initialValue) {
        Spinner<Integer> spinner = new Spinner<>(0, 59, initialValue, 5);
        spinner.setPrefWidth(60);
        return spinner;
    }
    
    private void handleCreateShift() {
        try {
            String shiftName = shiftNameField.getText();
            if (shiftName.isBlank()) {
                showAlert("Erroră", "Nume schimb obligatoriu");
                return;
            }
            
            LocalDateTime shiftStart = LocalDateTime.of(
                shiftDatePicker.getValue(),
                LocalTime.of(shiftStartHourSpinner.getValue(), shiftStartMinuteSpinner.getValue())
            );
            LocalDateTime shiftEnd = LocalDateTime.of(
                shiftDatePicker.getValue(),
                LocalTime.of(shiftEndHourSpinner.getValue(), shiftEndMinuteSpinner.getValue())
            );
            
            ProductionShift.ResourceType resourceType = resourceTypeCombo.getValue();
            if (resourceType == null) {
                showAlert("Erroră", "Selectează tip resursă");
                return;
            }
            
            String resourceId = resourceIdField.getText();
            if (resourceId.isBlank()) {
                showAlert("Erroră", "ID resursă obligatoriu");
                return;
            }
            
            BigDecimal capacity = new BigDecimal(capacityUnitsField.getText());
            
            schedulingFacade.createShift(shiftName, shiftStart, shiftEnd, resourceType, resourceId, capacity);
            
            // Clear form
            shiftNameField.clear();
            capacityUnitsField.setText("100");
            
            refreshShiftsTable();
            showAlert("Succes", "Schimb creat cu succes");
        } catch (Exception e) {
            showAlert("Erroră", "Eroare la crearea schimbului: " + e.getMessage());
        }
    }
    
    private void refreshShiftsTable() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime start = today.atStartOfDay();
            LocalDateTime end = today.plusDays(30).atTime(23, 59, 59);
            
            List<ProductionShift> shifts = schedulingFacade.findAvailableShifts(
                start,
                end,
                null  // Fetch all resource types
            );
            
            ObservableList<ProductionShiftRow> shiftRows = FXCollections.observableArrayList();
            for (ProductionShift shift : shifts) {
                shiftRows.add(new ProductionShiftRow(shift));
            }
            
            shiftsTable.setItems(shiftRows);
        } catch (Exception e) {
            showAlert("Erroră", "Eroare la reîncărcarea schimburilor: " + e.getMessage());
        }
    }
    
    private void refreshScheduleOverview(LocalDate startDate, LocalDate endDate) {
        try {
            Map<String, Object> overview = schedulingFacade.getSchedulingOverview(startDate, endDate);
            
            double totalUtilization = (double) overview.getOrDefault("totalCapacityUtilization", 0.0);
            utilizationLabel.setText(String.format("Utilizare: %.1f%%", totalUtilization));
            utilizationBar.setProgress(totalUtilization / 100.0);
        } catch (Exception e) {
            showAlert("Erroră", "Eroare la reîncărcarea overview: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner classes for table data
    public static class ProductionShiftRow {
        public String shiftName;
        public String resourceType;
        public String resourceId;
        public String shiftStart;
        public String shiftEnd;
        public String capacityUnits;
        public String allocatedUnits;
        public String status;
        
        public ProductionShiftRow(ProductionShift shift) {
            this.shiftName = shift.getShiftName();
            this.resourceType = shift.getResourceType().getDisplayName();
            this.resourceId = shift.getResourceId();
            this.shiftStart = shift.getShiftStart().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            this.shiftEnd = shift.getShiftEnd().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            this.capacityUnits = shift.getCapacityUnits().toPlainString();
            this.allocatedUnits = shift.getAllocatedUnits().toPlainString();
            this.status = shift.getStatus().getDisplayName();
        }
    }
    
    public static class ScheduleEntryRow {
        public String productName;
        public String allocatedQuantity;
        public String shiftName;
        public String status;
        public String priority;
        
        public ScheduleEntryRow(ProductionScheduleEntry entry) {
            this.productName = entry.getProductionOrderLine() != null &&
                              entry.getProductionOrderLine().getProduct() != null ?
                              entry.getProductionOrderLine().getProduct().getName() : "Unknown";
            this.allocatedQuantity = entry.getAllocatedQuantity().toPlainString();
            this.shiftName = entry.getProductionShift() != null ?
                            entry.getProductionShift().getShiftName() : "Unknown";
            this.status = entry.getStatus().getDisplayName();
            this.priority = entry.getPriority().toString();
        }
    }
}
