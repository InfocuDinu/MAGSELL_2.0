package com.bakerymanager.controller;

import com.bakerymanager.entity.User;
import com.bakerymanager.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class LoginController {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    
    private final UserService userService;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button loginButton;
    
    private boolean loginSuccessful = false;
    private Stage loginStage;
    
    public LoginController(UserService userService) {
        this.userService = userService;
    }
    
    @FXML
    public void initialize() {
        // Initialize default users if needed
        userService.initializeDefaultUsers();
        
        // Enable login on Enter key
        passwordField.setOnAction(event -> handleLogin());
        
        logger.info("Login controller initialized");
    }
    
    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Introduceți utilizator și parolă", true);
            return;
        }
        
        Optional<User> userOpt = userService.authenticate(username, password);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            loginSuccessful = true;
            showStatus("Autentificare reușită! Bun venit, " + user.getFullName(), false);
            logger.info("Login successful for user: {}", username);
            
            // Close login window after short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> {
                        if (loginStage != null) {
                            loginStage.close();
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } else {
            showStatus("Utilizator sau parolă incorectă!", true);
            passwordField.clear();
            passwordField.requestFocus();
        }
    }
    
    @FXML
    public void handleCancel() {
        loginSuccessful = false;
        if (loginStage != null) {
            loginStage.close();
        }
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: red;");
        } else {
            statusLabel.setStyle("-fx-text-fill: green;");
        }
    }
    
    public void setStage(Stage stage) {
        this.loginStage = stage;
    }
    
    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }
}
