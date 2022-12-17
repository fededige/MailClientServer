package com.example.mailclientserver;

import com.example.mailclientserver.model.Client;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.SyncFailedException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ClientController {
    private Client client;
    @FXML
    private Label emailAddressLabel;
    @FXML
    private Label counterEmails;

    @FXML
    public void initialize() throws Exception {
        //System.out.println(getClass().getResource("ClientView.fxml"));
        if (this.client != null)
            throw new IllegalStateException("Model can only be initialized once");
        System.out.println("inserisci mail");
        Scanner in = new Scanner(System.in);
        String emailAddress = checkEmail(in.nextLine());
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
        System.out.println("ciaociao");
        Parent root;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ClientView.fxml"));
            Stage newStage = new Stage();
            newStage.setTitle("Scrivi mail");
            newStage.setScene(new Scene(fxmlLoader.load(), 700, 500));
            newStage.initModality(Modality.APPLICATION_MODAL);
            newStage.showAndWait();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String checkEmail(String emailAddress) {
        String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        return Pattern.compile(regexPattern).matcher(emailAddress).matches() ? emailAddress : null;
    }
}