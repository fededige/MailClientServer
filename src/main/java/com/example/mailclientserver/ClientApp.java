package com.example.mailclientserver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

public class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("ClientView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 700);
        ClientController controller = fxmlLoader.getController();
        try {
            controller.init(getEmailAddress(), stage);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        stage.setTitle("MAIL-CLIENT");
        stage.setScene(scene);
        stage.show();
    }

    private String getEmailAddress() {
        System.out.println("inserisci mail");
        Scanner in = new Scanner(System.in);
        String userIn = in.nextLine();
        if(Objects.equals(userIn, "")){
            return "pippobaudo@mail.com";
        }
        return userIn;
    }

    public static void main(String[] args) {
        launch();
    }
}