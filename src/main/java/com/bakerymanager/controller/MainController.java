package com.bakerymanager.controller;

import com.bakerymanager.entity.User;
import com.bakerymanager.exception.AuthorizationException;
import com.bakerymanager.service.AuthorizationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class MainController {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final AuthorizationService authorizationService;

    public MainController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }
    
    @FXML
    private Label dateTimeLabel;
    
    @FXML
    private Label userLabel;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label connectionStatusLabel;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button inventoryButton;

    @FXML
    private Button productionButton;

    @FXML
    private Button schedulingButton;

    @FXML
    private Button posButton;

    @FXML
    private Button invoicesButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Button settingsButton;
    
    @FXML
    private StackPane contentPane;
    
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    
    @FXML
    public void initialize() {
        updateDateTime();
        startClock();
        configureRbacUi();
        logger.info("Main controller initialized successfully");
    }

    private void configureRbacUi() {
        User current = authorizationService.currentUser().orElse(null);
        if (current != null) {
            userLabel.setText(current.getFullName() + " (" + current.getRole().getDisplayName() + ")");
        } else {
            userLabel.setText("N/A");
        }

        boolean operatorOrAbove = authorizationService.hasOperatorAccess();
        boolean managerOrAdmin = authorizationService.hasAnyRole(User.Role.MANAGER, User.Role.ADMIN);
        boolean admin = authorizationService.hasAnyRole(User.Role.ADMIN);

        dashboardButton.setDisable(!operatorOrAbove);
        posButton.setDisable(!operatorOrAbove);
        inventoryButton.setDisable(!operatorOrAbove);

        productionButton.setDisable(!managerOrAdmin);
        schedulingButton.setDisable(!managerOrAdmin);
        invoicesButton.setDisable(!managerOrAdmin);
        reportsButton.setDisable(!managerOrAdmin);
        settingsButton.setDisable(!admin);
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
        if (!tryRequireOperatorOrAbove("NAV_DASHBOARD", "dashboard")) {
            return;
        }
        try {
            loadView("/fxml/dashboard.fxml");
            statusLabel.setText("Dashboard încărcat");
        } catch (IOException e) {
            logger.error("Error loading dashboard", e);
            statusLabel.setText("Eroare la încărcare dashboard");
        }
    }
    
    @FXML
    public void showInventory() {
        if (!tryRequireOperatorOrAbove("NAV_INVENTORY", "inventory")) {
            return;
        }
        try {
            loadView("/fxml/inventory.fxml");
            statusLabel.setText("Modul Gestiune Stocuri încărcat");
        } catch (IOException e) {
            logger.error("Error loading inventory", e);
            showError("Eroare la încărcarea modulului de gestiune");
        }
    }
    
    @FXML
    public void showProduction() {
        if (!tryRequireManagerOrAdmin("NAV_PRODUCTION", "production")) {
            return;
        }
        try {
            loadView("/fxml/production.fxml");
            statusLabel.setText("Modul Producție încărcat");
        } catch (IOException e) {
            logger.error("Error loading production", e);
            showError("Eroare la încărcarea modulului de producție");
        }
    }
    
    @FXML
    public void showPOS() {
        if (!tryRequireOperatorOrAbove("NAV_POS", "pos")) {
            return;
        }
        try {
            loadView("/fxml/pos.fxml");
            statusLabel.setText("Punct de Vânzare încărcat");
        } catch (IOException e) {
            logger.error("Error loading POS", e);
            showError("Eroare la încărcarea modulului POS");
        }
    }
    
    @FXML
    public void showInvoices() {
        if (!tryRequireManagerOrAdmin("NAV_INVOICES", "invoices")) {
            return;
        }
        try {
            loadView("/fxml/invoices.fxml");
            statusLabel.setText("Modul Facturi SPV încărcat");
        } catch (IOException e) {
            logger.error("Error loading invoices", e);
            showError("Eroare la încărcarea modulului de facturi");
        }
    }
    
    @FXML
    public void showReports() {
        if (!tryRequireManagerOrAdmin("NAV_REPORTS", "reports")) {
            return;
        }
        try {
            loadView("/fxml/reports.fxml");
            statusLabel.setText("Modul Rapoarte încărcat");
        } catch (IOException e) {
            logger.error("Error loading reports", e);
            showError("Eroare la încărcarea modulului de rapoarte");
        }
    }
    
    @FXML
    public void showScheduling() {
        if (!tryRequireManagerOrAdmin("NAV_SCHEDULING", "scheduling")) {
            return;
        }
        try {
            loadView("/fxml/scheduling.fxml");
            statusLabel.setText("Planificator Producție încărcat");
        } catch (IOException e) {
            logger.error("Error loading scheduling", e);
            showError("Eroare la încărcarea modulului de planificare");
        }
    }
    
    @FXML
    public void showSettings() {
        if (!tryRequireAdmin("NAV_SETTINGS", "settings")) {
            return;
        }
        try {
            loadView("/fxml/settings.fxml");
            statusLabel.setText("Modul Setări încărcat");
        } catch (IOException e) {
            logger.error("Error loading settings", e);
            showError("Eroare la încărcarea modulului de setări");
        }
    }
    
    @FXML
    public void exitApplication() {
        logger.info("Application exit requested by user");
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

    private boolean tryRequireOperatorOrAbove(String action, String resource) {
        try {
            authorizationService.requireOperatorOrAbove(action, resource);
            return true;
        } catch (AuthorizationException ex) {
            showError(ex.getMessage());
            return false;
        }
    }

    private boolean tryRequireManagerOrAdmin(String action, String resource) {
        try {
            authorizationService.requireAnyRole(action, resource, User.Role.ADMIN, User.Role.MANAGER);
            return true;
        } catch (AuthorizationException ex) {
            showError(ex.getMessage());
            return false;
        }
    }

    private boolean tryRequireAdmin(String action, String resource) {
        try {
            authorizationService.requireAnyRole(action, resource, User.Role.ADMIN);
            return true;
        } catch (AuthorizationException ex) {
            showError(ex.getMessage());
            return false;
        }
    }
}
