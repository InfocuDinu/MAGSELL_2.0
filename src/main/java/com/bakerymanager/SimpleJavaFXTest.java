package com.bakerymanager;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class SimpleJavaFXTest extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Hello JavaFX!");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 300, 250);
        
        primaryStage.setTitle("JavaFX Test");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("JavaFX application started successfully!");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
