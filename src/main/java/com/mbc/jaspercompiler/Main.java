// Developed by Thant Zin Aung(MBC)

package com.mbc.jaspercompiler;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

// Developed by Thant Zin Aung(MBC)
public class Main extends Application {
    private static double xOffSet;
    private static double yOffSet;
    public static Stage primaryStage;
    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("views/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("MBC Jasper Report Compiler");
        stage.initStyle(StageStyle.TRANSPARENT);
        makeWindowDraggable(scene, stage);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public static void makeWindowDraggable(Scene scene, Stage stage) {
        scene.setOnMousePressed(event -> {
            xOffSet = event.getSceneX();
            yOffSet = event.getSceneY();
        });
        scene.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX()-xOffSet);
            stage.setY(event.getScreenY()-yOffSet);
        });
    }
}