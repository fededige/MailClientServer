package com.example.mailclientserver;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.application.Application;

import java.io.IOException;

public class ServerMain extends Application{
    public void start(Stage stage) throws IOException {
        System.out.println("dentro start");
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ServerView.fxml"));
        stage.setTitle("Server");
        stage.setScene(new Scene(fxmlLoader.load(), 800, 600));
        stage.show();
        System.out.println("fine start");
    }
    public static void main(String[] args){
        launch();
    }
}
