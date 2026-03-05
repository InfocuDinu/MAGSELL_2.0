package com.bakerymanager.controller;

import com.bakerymanager.entity.ProductionScheduleEntry;
import com.bakerymanager.entity.ProductionShift;
import com.bakerymanager.smartbill.production.api.SchedulingFacade;
import com.bakerymanager.smartbill.production.api.dto.SchedulerAlertDto;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
public class SchedulingController {

    private final SchedulingFacade schedulingFacade;

    @FXML
    private TextField shiftNameField;
    @FXML
    private DatePicker shiftDatePicker;
    @FXML
    private Spinner<Integer> shiftStartHourSpinner;
    @FXML
    private Spinner<Integer> shiftStartMinuteSpinner;
    @FXML
    private Spinner<Integer> shiftEndHourSpinner;
    @FXML
    private Spinner<Integer> shiftEndMinuteSpinner;
    @FXML
    private ComboBox<ProductionShift.ResourceType> resourceTypeCombo;
    @FXML
    private TextField resourceIdField;
    @FXML
    private TextField capacityUnitsField;

    @FXML
    private DatePicker scheduleStartDatePicker;
    @FXML
    private DatePicker scheduleEndDatePicker;
    @FXML
    private TableView<ProductionShift> shiftsTable;
    @FXML
    private TableView<ProductionScheduleEntry> scheduleTable;
    @FXML
    private Label utilizationLabel;
    @FXML
    private Label headerUtilizationLabel;
    @FXML
    private ProgressBar utilizationBar;
    @FXML
    private TextArea schedulerInsightsArea;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    public SchedulingController(SchedulingFacade schedulingFacade) {
        this.schedulingFacade = schedulingFacade;
    }

    @FXML
    public void initialize() {
        configureControls();
        configureTables();
        refreshAll();
    }

    private void configureControls() {
        shiftDatePicker.setValue(LocalDate.now());
        scheduleStartDatePicker.setValue(LocalDate.now());
        scheduleEndDatePicker.setValue(LocalDate.now().plusDays(7));
        resourceTypeCombo.setItems(FXCollections.observableArrayList(ProductionShift.ResourceType.values()));
        resourceTypeCombo.setValue(ProductionShift.ResourceType.LINE);

        shiftStartHourSpinner.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 8));
        shiftStartMinuteSpinner.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5));
        shiftEndHourSpinner.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 16));
        shiftEndMinuteSpinner.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5));
    }

    @SuppressWarnings("unchecked")
    private void configureTables() {
        List<TableColumn<ProductionShift, ?>> shiftCols = shiftsTable.getColumns();
        if (shiftCols.size() >= 5) {
            ((TableColumn<ProductionShift, String>) shiftCols.get(0)).setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getShiftName()));
            ((TableColumn<ProductionShift, String>) shiftCols.get(1)).setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getResourceType().getDisplayName() + " (" + c.getValue().getResourceId() + ")"));
            ((TableColumn<ProductionShift, String>) shiftCols.get(2)).setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getShiftStart().format(DT) + " - " + c.getValue().getShiftEnd().format(DT)));
            ((TableColumn<ProductionShift, String>) shiftCols.get(3)).setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getAllocatedUnits().toPlainString() + " / " + c.getValue().getCapacityUnits().toPlainString()));
            ((TableColumn<ProductionShift, String>) shiftCols.get(4)).setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getStatus().getDisplayName()));
        }

        List<TableColumn<ProductionScheduleEntry, ?>> entryCols = scheduleTable.getColumns();
        if (entryCols.size() >= 5) {
            ((TableColumn<ProductionScheduleEntry, String>) entryCols.get(0)).setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getProductionOrderLine() != null && c.getValue().getProductionOrderLine().getProduct() != null
                    ? c.getValue().getProductionOrderLine().getProduct().getName() : "N/A"));
            ((TableColumn<ProductionScheduleEntry, String>) entryCols.get(1)).setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getAllocatedQuantity() != null ? c.getValue().getAllocatedQuantity().toPlainString() : "0"));
            ((TableColumn<ProductionScheduleEntry, String>) entryCols.get(2)).setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getProductionShift() != null ? c.getValue().getProductionShift().getShiftName() : "N/A"));
            ((TableColumn<ProductionScheduleEntry, String>) entryCols.get(3)).setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getStatus() != null ? c.getValue().getStatus().getDisplayName() : "N/A"));
            ((TableColumn<ProductionScheduleEntry, String>) entryCols.get(4)).setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getPriority() != null ? String.valueOf(c.getValue().getPriority()) : "-"));
        }
    }

    @FXML
    public void createShift() {
        try {
            if (shiftNameField.getText() == null || shiftNameField.getText().isBlank()) {
                showInfo("Validare", "Nume schimb obligatoriu.");
                return;
            }
            LocalDate d = shiftDatePicker.getValue();
            LocalDateTime start = LocalDateTime.of(d, LocalTime.of(shiftStartHourSpinner.getValue(), shiftStartMinuteSpinner.getValue()));
            LocalDateTime end = LocalDateTime.of(d, LocalTime.of(shiftEndHourSpinner.getValue(), shiftEndMinuteSpinner.getValue()));

            schedulingFacade.createShift(
                shiftNameField.getText().trim(),
                start,
                end,
                resourceTypeCombo.getValue(),
                resourceIdField.getText() != null ? resourceIdField.getText().trim() : "",
                new BigDecimal(capacityUnitsField.getText() != null && !capacityUnitsField.getText().isBlank() ? capacityUnitsField.getText().trim() : "0")
            );
            refreshAll();
            showInfo("Succes", "Schimb creat cu succes.");
        } catch (Exception ex) {
            showError("Eroare creare schimb", ex.getMessage());
        }
    }

    @FXML
    public void refreshScheduler() {
        refreshAll();
    }

    @FXML
    public void autoSchedule() {
        try {
            List<ProductionScheduleEntry> created = schedulingFacade.autoScheduleOrders(scheduleStartDatePicker.getValue(), scheduleEndDatePicker.getValue());
            refreshAll();
            showInfo("Auto-scheduling", "Intrări planificate automat: " + created.size());
        } catch (Exception ex) {
            showError("Eroare auto-scheduling", ex.getMessage());
        }
    }

    @FXML
    public void levelResources() {
        try {
            int moved = schedulingFacade.levelResources(scheduleStartDatePicker.getValue(), scheduleEndDatePicker.getValue());
            refreshAll();
            showInfo("Resource leveling", "Intrări realocate: " + moved);
        } catch (Exception ex) {
            showError("Eroare resource leveling", ex.getMessage());
        }
    }

    @FXML
    public void showAlerts() {
        try {
            List<SchedulerAlertDto> alerts = schedulingFacade.getSchedulerAlerts(scheduleStartDatePicker.getValue(), scheduleEndDatePicker.getValue());
            if (alerts.isEmpty()) {
                schedulerInsightsArea.setText("Nu există alerte pentru intervalul selectat.");
                return;
            }
            StringBuilder sb = new StringBuilder("=== ALERTS ===\n");
            for (SchedulerAlertDto a : alerts) {
                sb.append("[").append(a.severity()).append("] ")
                    .append(a.code()).append(" | ")
                    .append(a.message()).append(" | ")
                    .append(a.resource()).append(" | ")
                    .append(a.at()).append("\n");
            }
            schedulerInsightsArea.setText(sb.toString());
        } catch (Exception ex) {
            showError("Eroare alerting", ex.getMessage());
        }
    }

    @FXML
    public void syncExecution() {
        try {
            Map<String, Object> result = schedulingFacade.reconcileExecution(scheduleStartDatePicker.getValue(), scheduleEndDatePicker.getValue());
            schedulerInsightsArea.setText("=== PLAN VS EXECUTION ===\n" + result);
            refreshAll();
        } catch (Exception ex) {
            showError("Eroare sincronizare execuție", ex.getMessage());
        }
    }

    @FXML
    public void showAdvancedReport() {
        try {
            Map<String, Object> report = schedulingFacade.getAdvancedSchedulingReport(scheduleStartDatePicker.getValue(), scheduleEndDatePicker.getValue());
            schedulerInsightsArea.setText("=== ADVANCED REPORT ===\n" + report);
        } catch (Exception ex) {
            showError("Eroare raportare avansată", ex.getMessage());
        }
    }

    private void refreshAll() {
        LocalDate startDate = scheduleStartDatePicker.getValue() != null ? scheduleStartDatePicker.getValue() : LocalDate.now();
        LocalDate endDate = scheduleEndDatePicker.getValue() != null ? scheduleEndDatePicker.getValue() : startDate.plusDays(7);

        List<ProductionShift> shifts = schedulingFacade.findAvailableShifts(startDate.atStartOfDay(), endDate.atTime(23, 59, 59), null);
        shiftsTable.setItems(FXCollections.observableArrayList(shifts));

        List<ProductionScheduleEntry> entries = schedulingFacade.getScheduleEntries(startDate, endDate);
        scheduleTable.setItems(FXCollections.observableArrayList(entries));

        Map<String, Object> overview = schedulingFacade.getSchedulingOverview(startDate, endDate);
        double util = (double) overview.getOrDefault("totalCapacityUtilization", 0.0);
        String text = String.format("Utilizare: %.1f%%", util);
        utilizationLabel.setText(text);
        if (headerUtilizationLabel != null) {
            headerUtilizationLabel.setText(text);
        }
        utilizationBar.setProgress(Math.max(0.0, Math.min(1.0, util / 100.0)));
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
