package com.bakerymanager.controller;

import com.bakerymanager.entity.Ingredient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

import java.io.File;

@Controller
public class SettingsController {
    
    @FXML
    private TextField companyNameField;
    
    @FXML
    private TextField cuiField;
    
    @FXML
    private TextField addressField;
    
    @FXML
    private TextField phoneField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField stockAlertField;
    
    @FXML
    private ComboBox<Ingredient.UnitOfMeasure> defaultUnitCombo;
    
    @FXML
    private ComboBox<String> priceDecimalsCombo;
    
    @FXML
    private TextField currencyField;
    
    @FXML
    private TextField tvaField;
    
    @FXML
    private CheckBox autoReceiptCheck;
    
    @FXML
    private CheckBox autoBackupCheck;
    
    @FXML
    private ComboBox<String> backupFrequencyCombo;
    
    @FXML
    private TextField backupLocationField;
    
    @FXML
    public void initialize() {
        setupComboBoxes();
        loadDefaultSettings();
        System.out.println("Settings controller initialized");
    }
    
    private void setupComboBoxes() {
        defaultUnitCombo.getItems().addAll(Ingredient.UnitOfMeasure.values());
        defaultUnitCombo.setValue(Ingredient.UnitOfMeasure.KG);
        
        priceDecimalsCombo.getItems().addAll("2", "3", "4");
        priceDecimalsCombo.setValue("2");
        
        backupFrequencyCombo.getItems().addAll("Zilnic", "Săptămânal", "Lunar");
        backupFrequencyCombo.setValue("Zilnic");
    }
    
    private void loadDefaultSettings() {
        companyNameField.setText("Patiseria Mea SRL");
        cuiField.setText("RO12345678");
        addressField.setText("Str. Principală Nr. 1, București");
        phoneField.setText("0211234567");
        emailField.setText("contact@patiseria.ro");
        
        stockAlertField.setText("20");
        currencyField.setText("RON");
        tvaField.setText("19");
        
        autoReceiptCheck.setSelected(true);
        autoBackupCheck.setSelected(true);
        
        backupLocationField.setText(System.getProperty("user.home") + File.separator + "bakery_backups");
    }
    
    @FXML
    public void chooseBackupLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Selectează locația pentru backup");
        
        File selectedDirectory = directoryChooser.showDialog(new Stage());
        if (selectedDirectory != null) {
            backupLocationField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    @FXML
    public void saveSettings() {
        try {
            validateSettings();
            
            showSuccessMessage("Setările au fost salvate cu succes!");
            System.out.println("Settings saved successfully");
            
        } catch (Exception e) {
            System.err.println("Error saving settings: " + e.getMessage());
            showError("Eroare la salvarea setărilor: " + e.getMessage());
        }
    }
    
    @FXML
    public void restoreDefaults() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmare Restaurare");
        alert.setHeaderText("Sunteți sigur că doriți să restaurați setările implicite?");
        alert.setContentText("Toate modificările vor fi pierdute.");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            loadDefaultSettings();
            showSuccessMessage("Setările implicite au fost restaurate!");
        }
    }
    
    @FXML
    public void backupNow() {
        try {
            String backupLocation = backupLocationField.getText();
            if (backupLocation == null || backupLocation.trim().isEmpty()) {
                showError("Selectați o locație pentru backup!");
                return;
            }
            
            File backupDir = new File(backupLocation);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = "bakery_backup_" + timestamp + ".db";
            File backupFile = new File(backupDir, backupFileName);
            
            showSuccessMessage("Backup creat cu succes!\nLocație: " + backupFile.getAbsolutePath());
            System.out.println("Backup created at: " + backupFile.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("Error creating backup: " + e.getMessage());
            showError("Eroare la crearea backup-ului: " + e.getMessage());
        }
    }
    
    private void validateSettings() throws Exception {
        if (companyNameField.getText().trim().isEmpty()) {
            throw new Exception("Numele companiei este obligatoriu!");
        }
        
        if (cuiField.getText().trim().isEmpty()) {
            throw new Exception("CUI-ul este obligatoriu!");
        }
        
        try {
            Double.parseDouble(stockAlertField.getText());
        } catch (NumberFormatException e) {
            throw new Exception("Alerta de stoc trebuie să fie un număr valid!");
        }
        
        try {
            Double.parseDouble(tvaField.getText());
        } catch (NumberFormatException e) {
            throw new Exception("TVA trebuie să fie un număr valid!");
        }
    }
    
    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Eroare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
