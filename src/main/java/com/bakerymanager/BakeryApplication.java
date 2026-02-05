package com.bakerymanager;

import com.bakerymanager.config.SpringFXMLLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@SpringBootApplication
@Component
public class BakeryApplication extends Application {
    
    private static ConfigurableApplicationContext context;
    private Parent root;
    
    @Override
    public void init() throws Exception {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(BakeryApplication.class);
        builder.headless(false);
        context = builder.run();
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(context::getBean);
        loader.setLocation(getClass().getResource("/fxml/main_view.fxml"));
        root = loader.load();
        
        primaryStage.setTitle("BakeryManager Pro");
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.show();
        
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            context.close();
        });
    }
    
    @Override
    public void stop() throws Exception {
        context.close();
        Platform.exit();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
    public static ConfigurableApplicationContext getApplicationContext() {
        return context;
    }
}
