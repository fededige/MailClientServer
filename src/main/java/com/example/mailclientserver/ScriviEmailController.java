package com.example.mailclientserver;

import com.example.mailclientserver.messaggio.Messaggio;
import com.example.mailclientserver.model.Email;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ScriviEmailController {
    private Socket socket;
    private String senderEmail;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    @FXML
    private TextField ATextField;
    @FXML
    private TextField OggettoTextField;
    @FXML
    private TextArea TestoTextArea;
    @FXML
    private Label EmailErroreLabel;
    @FXML
    private Label CampiErroreLabel;

    @FXML
    private void bottoneInviaEmail() throws IOException, ClassNotFoundException {
        if (Objects.equals(ATextField.getText(), "") || Objects.equals(TestoTextArea.getText(), "")){
            CampiErroreLabel.setText("Campi vuoti");
        }
        else {
            String[] destinatari = ATextField.getText().split(",");
            List<String> listaDestinatari = new ArrayList<>();
            for (String s : destinatari) {
                String temp = s.trim();
                if (!(listaDestinatari.contains(temp)))
                    listaDestinatari.add(temp);
            }
            boolean isValid = true;
            for(String d: listaDestinatari) {
                (this.outputStream).writeObject(new Messaggio(0, d));
                String emailAddress = (String) inputStream.readObject();
                System.out.println(emailAddress);
                if (emailAddress.equals("Email non valida")) {
                    isValid = false;
                    break;
                }
            }
            if(!isValid){
                EmailErroreLabel.setText("Email non valida");
            }else{
                inviaEmail(listaDestinatari,OggettoTextField.getText(),TestoTextArea.getText());
                Stage stage = (Stage) EmailErroreLabel.getScene().getWindow();
                stage.close();
            }
        }
    }

    private void inviaEmail(List<String> destinatari, String Oggetto, String Testo) throws IOException {
        outputStream.writeObject(new Messaggio(1, new Email(this.senderEmail, destinatari, Oggetto, Testo)));
    }

    public void initParameter(Socket socket, String email, ObjectOutputStream outputStream, ObjectInputStream inputStream) {
        this.socket = socket;
        this.senderEmail = email;
        this.outputStream = outputStream;
        this.inputStream = inputStream;
        System.out.println("dentro initParameter ScriviEmailController");
    }

}
