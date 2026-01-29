package com.inspector.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Main JavaFX Application for the Java UI Inspector GUI
 */
public class InspectorApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        
        // Create scene with light theme by default
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/light.css").toExternalForm());
        
        // Setup stage
        primaryStage.setTitle("Philips Inspector - Hybrid Process Inspector");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        
        // Set icon (optional - would need to create an icon file)
        // primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app-icon.png")));
        
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
