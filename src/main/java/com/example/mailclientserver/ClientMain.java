package com.example.mailclientserver;

import com.example.mailclientserver.messaggio.Messaggio;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientMain extends Application {
    private Socket socket;
    public void openConnection(String ip, int port){
        while(true) {
            try {
                socket = new Socket(ip, port);
                break;
            } catch(IOException e) {
                System.out.println("Reconnect failed, wait");
                try {
                    Thread.sleep(10*1000);
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

        ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
        //System.out.println("dopo out");
        ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
        //System.out.println("dopo in");

        System.out.println("inserisci mail");
        Scanner in = new Scanner(System.in);
        String emailAddress = in.nextLine();
        outStream.writeObject(new Messaggio(0, emailAddress));
        emailAddress = (String)inStream.readObject();
        System.out.println(emailAddress);
        if(!(emailAddress.equals("Client inesistente"))){
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ClientView.fxml"));
            stage.setTitle("Mail");
            stage.setScene(new Scene(fxmlLoader.load(), 1280, 700));
            stage.show();
            ClientController clientController = fxmlLoader.getController();
            System.out.println(clientController);
            clientController.initParam(socket, outStream, inStream);
            clientController.initialize(stage, emailAddress);
        }
    }

    public static void main(String[] args) throws IOException {
        launch();
    }
}