package com.example.mailclientserver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        stage.setTitle("Mail");
        stage.setScene(new Scene(fxmlLoader.load(), 1280, 700));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}