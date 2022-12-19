package com.example.mailclientserver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

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
    public void start(Stage stage) throws Exception {
        System.out.println("sono il client");
        openConnection("127.0.0.1", 4445);
        System.out.println("connesso");
        Writer out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader inServer =  new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("inserisci mail");
        Scanner in = new Scanner(System.in);
        String emailAddress = in.nextLine();
        out.append(emailAddress).append("\n");
        out.flush();
        emailAddress = inServer.readLine();
        System.out.println(emailAddress);
        if(!(emailAddress.equals("Email non valida"))){
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ClientView.fxml"));
            stage.setTitle("Mail");
            stage.setScene(new Scene(fxmlLoader.load(), 1280, 700));
            stage.show();
            ClientController clientController = fxmlLoader.getController();
            System.out.println(clientController);
            clientController.initialize(emailAddress);
            clientController.setSocket(socket);
        }
    }

    public static void main(String[] args) throws IOException {
        launch();
    }
}