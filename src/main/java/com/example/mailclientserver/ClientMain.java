package com.example.mailclientserver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;

public class ClientMain extends Application {
    Socket socket;
    public void openConnection(String ip, int port){
        while(true) {
            try {
                socket = new Socket(ip, port);
                break;
            } catch(IOException e) {
                System.out.println("Reconnect failed, wait");
                try {
                    Thread.sleep(30*1000);
                } catch(InterruptedException ie) {
                    System.out.println("Interrupted");
                }
            }
        }
    }
    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("sono il client");
        openConnection("127.0.0.1", 4445);
        System.out.println("connesso");


        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ClientView.fxml"));

        stage.setTitle("Mail");
        stage.setScene(new Scene(fxmlLoader.load(), 1280, 700));
        stage.show();
    }

    public static void main(String[] args) throws IOException {
        launch();
    }
}