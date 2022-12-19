package com.example.mailclientserver;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ScriviEmailController {
    private Socket socket;
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
    private void bottoneInviaEmail() throws IOException {
        if (Objects.equals(ATextField.getText(), "") || Objects.equals(TestoTextArea.getText(), "")){
            CampiErroreLabel.setText("Campi vuoti");
        }
        else {
            Writer out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader inServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.append(ATextField.getText()).append("\n");
            out.flush();
            String emailAddress = inServer.readLine();
            System.out.println();
            if (emailAddress.equals("Email non valida")) {
                EmailErroreLabel.setText("Email non valida");
            }
            else {
                Stage stage = (Stage) EmailErroreLabel.getScene().getWindow();
                stage.close();
            }
        }
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
        System.out.println("dentro setSocket Sciedfvb");
    }

}
