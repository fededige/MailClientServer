package com.example.mailclientserver;

import com.example.mailclientserver.messaggio.Messaggio;
import com.example.mailclientserver.model.Email;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

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
    private MenuButton listaClientDropDown;
    private List<String> clientList;
    private List<String> receivers;
    @FXML
    private void bottoneInviaEmail() throws IOException, ClassNotFoundException {
        CampiErroreLabel.setText("");
        EmailErroreLabel.setText("");
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
            boolean exist = true;
            for(String d: listaDestinatari) {
                if(!checkEmail(d)){
                    isValid = false;
                    break;
                }
                (this.outputStream).writeObject(new Messaggio(0, d));
                String emailAddress = (String) inputStream.readObject();
                System.out.println(emailAddress);
                if (emailAddress.equals("Client inesistente")) {
                    exist = false;
                    break;
                }
            }
            if(isValid && exist){
                System.out.println("ciao");
                inviaEmail(listaDestinatari,OggettoTextField.getText(),TestoTextArea.getText());
                Stage stage = (Stage) EmailErroreLabel.getScene().getWindow();
                stage.close();
            }else if(!isValid){
                EmailErroreLabel.setText("Email non valida");
            }
            else{
                EmailErroreLabel.setText("Email inesistente");
            }
        }
    }

//    private void updateEmail() throws IOException {
//        outputStream.writeObject(new Messaggio(2, this.senderEmail.split("@")[0]));
//    }

    private void inviaEmail(List<String> destinatari, String Oggetto, String Testo) throws IOException {
        outputStream.writeObject(new Messaggio(1, new Email(null, this.senderEmail, destinatari, Oggetto, Testo)));
    }

    public void initParameter(Socket socket, String email, ObjectOutputStream outputStream, ObjectInputStream inputStream, List<String> receivers, String subject, String text,List<String> clientList) {
        this.socket = socket;
        this.senderEmail = email;
        this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.receivers = receivers;
        this.clientList = clientList;
        caricaMenuButton();
        if(receivers != null){
            if(receivers.size() < 2) {
                ATextField.setText(receivers.get(0));
            }
            else{
                String receiversText = receivers.get(0);
                for(int i = 1; i < receivers.size(); i++){
                    receiversText += ", " + receivers.get(i);
                }
                ATextField.setText(receiversText);
            }
        }else if(subject!=null && text!=null){
            OggettoTextField.setText(subject);
            TestoTextArea.setText(text);
        }
    }

    private void caricaMenuButton() {
        for(String s : clientList){
            MenuItem m = new MenuItem(s);
            m.setOnAction((event) -> changeSceneOnBtnClick(
                    event, m.getText()));
            (this.listaClientDropDown).getItems().add(m);
        }
    }

    private void changeSceneOnBtnClick(ActionEvent event, String s) {
        this.ATextField.setText(ATextField.getText().isEmpty() ? s : ATextField.getText() + ", " + s);
    }

    private boolean checkEmail(String emailAddress) {
        String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        return Pattern.compile(regexPattern).matcher(emailAddress).matches();
    }
}
