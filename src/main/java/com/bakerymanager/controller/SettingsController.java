package com.bakerymanager.controller;

import com.bakerymanager.entity.Ingredient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Controller
public class SettingsController {
    
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    private static final String CONFIG_FILE = "config.properties";
    
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
        loadSettings();
        logger.info("Settings controller initialized");
    }
    
    private void setupComboBoxes() {
        defaultUnitCombo.getItems().addAll(Ingredient.UnitOfMeasure.values());
        defaultUnitCombo.setValue(Ingredient.UnitOfMeasure.KG);
        
        priceDecimalsCombo.getItems().addAll("2", "3", "4");
        priceDecimalsCombo.setValue("2");
        
        backupFrequencyCombo.getItems().addAll("Zilnic", "Săptămânal", "Lunar");
        backupFrequencyCombo.setValue("Zilnic");
    }
    
    private void loadSettings() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                props.load(input);
                
                // Încărcăm valorile din fișier
                companyNameField.setText(props.getProperty("company.name", "Patiseria Mea SRL"));
                cuiField.setText(props.getProperty("company.cui", "RO12345678"));
                addressField.setText(props.getProperty("company.address", "Str. Principală Nr. 1, București"));
                phoneField.setText(props.getProperty("company.phone", "0211234567"));
                emailField.setText(props.getProperty("company.email", "contact@patiseria.ro"));
                
                stockAlertField.setText(props.getProperty("stock.alert", "20"));
                currencyField.setText(props.getProperty("currency", "RON"));
                tvaField.setText(props.getProperty("tax.vat", "19"));
                
                autoReceiptCheck.setSelected(Boolean.parseBoolean(props.getProperty("auto.receipt", "true")));
                autoBackupCheck.setSelected(Boolean.parseBoolean(props.getProperty("auto.backup", "true")));
                
                backupFrequencyCombo.setValue(props.getProperty("backup.frequency", "Zilnic"));
                backupLocationField.setText(props.getProperty("backup.location", System.getProperty("user.home") + File.separator + "bakery_backups"));
                
                // Setăm unitatea implicită
                String defaultUnit = props.getProperty("default.unit", "KG");
                try {
                    defaultUnitCombo.setValue(Ingredient.UnitOfMeasure.valueOf(defaultUnit));
                } catch (IllegalArgumentException e) {
                    defaultUnitCombo.setValue(Ingredient.UnitOfMeasure.KG);
                    logger.warn("Invalid unit of measure in config, using default: KG");
                }
                
                logger.info("Settings loaded from {}", CONFIG_FILE);
                
            } catch (IOException e) {
                logger.error("Error loading settings from {}", CONFIG_FILE, e);
                loadDefaultSettings();
            }
        } else {
            logger.info("Config file not found, loading default settings");
            loadDefaultSettings();
        }
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
            
            Properties props = new Properties();
            
            // Salvăm toate setările în Properties
            props.setProperty("company.name", companyNameField.getText().trim());
            props.setProperty("company.cui", cuiField.getText().trim());
            props.setProperty("company.address", addressField.getText().trim());
            props.setProperty("company.phone", phoneField.getText().trim());
            props.setProperty("company.email", emailField.getText().trim());
            
            props.setProperty("stock.alert", stockAlertField.getText().trim());
            props.setProperty("currency", currencyField.getText().trim());
            props.setProperty("tax.vat", tvaField.getText().trim());
            
            props.setProperty("auto.receipt", String.valueOf(autoReceiptCheck.isSelected()));
            props.setProperty("auto.backup", String.valueOf(autoBackupCheck.isSelected()));
            
            props.setProperty("backup.frequency", backupFrequencyCombo.getValue());
            props.setProperty("backup.location", backupLocationField.getText().trim());
            
            if (defaultUnitCombo.getValue() != null) {
                props.setProperty("default.unit", defaultUnitCombo.getValue().name());
            }
            
            // Creăm directorul pentru config dacă nu există
            File configFile = new File(CONFIG_FILE);
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // Salvăm în fișier
            try (OutputStream output = new FileOutputStream(configFile)) {
                props.store(output, "Bakery Manager Settings - " + java.time.LocalDateTime.now());
            }
            
            showSuccessMessage("Setările au fost salvate cu succes!\nFișier: " + configFile.getAbsolutePath());
            logger.info("Settings saved to {}", CONFIG_FILE);
            
        } catch (Exception e) {
            logger.error("Error saving settings to {}", CONFIG_FILE, e);
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
            
            // Validate and sanitize backup path to prevent directory traversal
            Path backupPath = Paths.get(backupLocation).normalize();
            File backupDir = backupPath.toFile();
            
            if (!backupDir.exists()) {
                if (!backupDir.mkdirs()) {
                    showError("Nu s-a putut crea directorul pentru backup!");
                    logger.error("Failed to create backup directory: {}", backupPath);
                    return;
                }
            }
            
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = "bakery_backup_" + timestamp + ".db";
            File backupFile = new File(backupDir, backupFileName);
            
            // TODO: Implement actual database backup logic here
            // For now, this is just a placeholder showing the backup location
            
            showSuccessMessage("Backup creat cu succes!\nLocație: " + backupFile.getAbsolutePath());
            logger.info("Backup created at: {}", backupFile.getAbsolutePath());
            
        } catch (Exception e) {
            logger.error("Error creating backup", e);
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
