package com.example.mailclientserver;

import com.example.mailclientserver.messaggio.Messaggio;
import com.example.mailclientserver.model.Client;
import com.example.mailclientserver.model.Email;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.List;

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
    private ListView<Email> listEmail;

    @FXML
    public void initialize(String emailAddress) throws Exception {
        if (this.client != null)
            throw new IllegalStateException("Model can only be initialized once");
        if(emailAddress != null){
            client = new Client(emailAddress);
            updateEmailList();
        }
        else{
            throw new Exception("Email non valida");
        }

        emailAddressLabel.textProperty().bind(client.emailAddressProperty());
        counterEmails.textProperty().bind(client.getEmailsCount().asString());
        listEmail.itemsProperty().bind(client.inboxProperty());
    }

    private void updateEmailList() throws IOException, ClassNotFoundException {
        Messaggio m = new Messaggio(2, client.emailAddressProperty().getValue().split("@")[0]);
        System.out.println("updateEmailList");
        (this.outputStream).writeObject(m);
        System.out.println("updateEmailList dopo write");
        List<Email> emailAddress = (List<Email>) inputStream.readObject(); //ignorare il warning
        System.out.println("updateEmailList dopo read");
        client.setInboxContent(emailAddress);
        System.out.println("updateEmailList dopo");
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
            System.out.println("paginaScriviMail");
            List<Email> emailAddress = (List<Email>) inputStream.readObject(); //ignorare il warning
            System.out.println(emailAddress.size());
            for(Email e : emailAddress){
                System.out.println("ciao");
                System.out.println(e);
            }
            client.setInboxContent(emailAddress);
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public void initParam(Socket socket, ObjectOutputStream outputStream, ObjectInputStream inputStream) {
        System.out.println("initParam");
        this.socket = socket;
        this.outputStream = outputStream;
        this.inputStream = inputStream;
    }
}