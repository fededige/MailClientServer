package com.example.mailclientserver;

import com.example.mailclientserver.model.Client;
import com.example.mailclientserver.model.Email;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ClientController {
    private Socket socket;
    private Client client;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    @FXML
    private Label emailAddressLabel;
    @FXML
    private Label counterEmails;

    @FXML
    public void initialize(String emailAddress) throws Exception {
        if (this.client != null)
            throw new IllegalStateException("Model can only be initialized once");
        if(emailAddress != null){
            client = new Client(emailAddress);
        }
        else{
            throw new Exception("Email non valida");
        }

        emailAddressLabel.textProperty().bind(client.emailAddressProperty());
        counterEmails.textProperty().bind(client.getEmailsCount());
    }

    @FXML
    public void paginaScriviMail() {
        Parent root;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ScriviEmail.fxml"));
            Stage newStage = new Stage();
            newStage.setTitle("Scrivi mail");
            newStage.setScene(new Scene(fxmlLoader.load(), 700, 500));
            ScriviEmailController scriviEmailController = fxmlLoader.getController();
            scriviEmailController.initParameter(socket, client.emailAddressProperty().getValue(), this.outputStream, this.inputStream); //magari fare iterfaccia Controller con tutti i metodi in comune e necessari
            newStage.initModality(Modality.APPLICATION_MODAL);
            newStage.showAndWait();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void initParm(Socket socket, ObjectOutputStream outputStream, ObjectInputStream inputStream) {
        this.socket = socket;
        this.outputStream = outputStream;
        this.inputStream = inputStream;
    }
}