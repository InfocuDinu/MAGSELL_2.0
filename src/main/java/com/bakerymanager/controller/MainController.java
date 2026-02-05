package com.bakerymanager.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class MainController {
    
    @FXML
    private Label dateTimeLabel;
    
    @FXML
    private Label userLabel;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label connectionStatusLabel;
    
    @FXML
    private StackPane contentPane;
    
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    
    @FXML
    public void initialize() {
        updateDateTime();
        startClock();
        System.out.println("Main controller initialized successfully");
    }
    
    private void startClock() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1),
                event -> updateDateTime()
            )
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
    }
    
    private void updateDateTime() {
        dateTimeLabel.setText(LocalDateTime.now().format(timeFormatter));
    }
    
    @FXML
    public void showDashboard() {
        try {
            loadView("/fxml/dashboard.fxml");
            statusLabel.setText("Dashboard încărcat");
        } catch (IOException e) {
            System.err.println("Error loading dashboard: " + e.getMessage());
            statusLabel.setText("Eroare la încărcare dashboard");
        }
    }
    
    @FXML
    public void showInventory() {
        try {
            loadView("/fxml/inventory.fxml");
            statusLabel.setText("Modul Gestiune Stocuri încărcat");
        } catch (IOException e) {
            System.err.println("Error loading inventory: " + e.getMessage());
            showError("Eroare la încărcarea modulului de gestiune");
        }
    }
    
    @FXML
    public void showProduction() {
        try {
            loadView("/fxml/production.fxml");
            statusLabel.setText("Modul Producție încărcat");
        } catch (IOException e) {
            System.err.println("Error loading production: " + e.getMessage());
            showError("Eroare la încărcarea modulului de producție");
        }
    }
    
    @FXML
    public void showPOS() {
        try {
            loadView("/fxml/pos.fxml");
            statusLabel.setText("Punct de Vânzare încărcat");
        } catch (IOException e) {
            System.err.println("Error loading POS: " + e.getMessage());
            showError("Eroare la încărcarea modulului POS");
        }
    }
    
    @FXML
    public void showInvoices() {
        try {
            loadView("/fxml/invoices.fxml");
            statusLabel.setText("Modul Facturi SPV încărcat");
        } catch (IOException e) {
            System.err.println("Error loading invoices: " + e.getMessage());
            showError("Eroare la încărcarea modulului de facturi");
        }
    }
    
    @FXML
    public void showReports() {
        try {
            loadView("/fxml/reports.fxml");
            statusLabel.setText("Modul Rapoarte încărcat");
        } catch (IOException e) {
            System.err.println("Error loading reports: " + e.getMessage());
            showError("Eroare la încărcarea modulului de rapoarte");
        }
    }
    
    @FXML
    public void showSettings() {
        try {
            loadView("/fxml/settings.fxml");
            statusLabel.setText("Modul Setări încărcat");
        } catch (IOException e) {
            System.err.println("Error loading settings: " + e.getMessage());
            showError("Eroare la încărcarea modulului de setări");
        }
    }
    
    @FXML
    public void exitApplication() {
        System.out.println("Application exit requested by user");
        javafx.application.Platform.exit();
        System.exit(0);
    }
    
    private void loadView(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(com.bakerymanager.BakeryApplication.getApplicationContext()::getBean);
        Parent view = loader.load();
        contentPane.getChildren().setAll(view);
    }
    
    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Eroare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
