package com.example.mailclientserver;

import com.example.mailclientserver.messaggio.Messaggio;
import com.example.mailclientserver.model.Client;
import com.example.mailclientserver.model.Email;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.json.JSONObject;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.channels.Pipe;
import java.util.ArrayList;
import java.util.List;

public class ClientController {
    private Socket socket;
    private Client client;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private Email emailSelezionata;
    @FXML
    private Label emailAddressLabel;
    @FXML
    private Label counterEmails;
    @FXML
    private ListView<Email> listEmail;
    @FXML
    private Label DaContent;
    @FXML
    private TextField AContent;
    @FXML
    private TextArea OggettoContent;
    @FXML
    private TextArea TextContent;
    @FXML
    private MenuButton sceltaInviateRicevute;

    @FXML
    public void initialize(String emailAddress) throws Exception {
        if (this.client != null)
            throw new IllegalStateException("Model can only be initialized once");
        if(emailAddress != null){
            client = new Client(emailAddress);
            updateEmailList();
            client.setInboxContent(client.inboxPropertyRicevute());
        }
        else{
            throw new Exception("Email non valida");
        }
        emailSelezionata = null;
        emailAddressLabel.textProperty().bind(client.emailAddressProperty());
        counterEmails.textProperty().bind(client.getEmailsCount().asString());
        listEmail.itemsProperty().bind(client.inboxProperty());
    }

    private void updateEmailList() throws IOException, ClassNotFoundException {
        Messaggio m = new Messaggio(2, client.emailAddressProperty().getValue().split("@")[0]);
        (this.outputStream).writeObject(m);
        List<Email> allEmails = (List<Email>) inputStream.readObject(); //ignorare il warning
        List<Email> receivedEmails = new ArrayList<>();
        List<Email> sentEmails = new ArrayList<>();
        for(Email e : allEmails){
            if (e.getSender().equals(client.emailAddressProperty().getValue())) {
                sentEmails.add(e);
            }else{
                receivedEmails.add(e);
            }
        }
        client.setInboxContentInviate(sentEmails);
        client.setInboxContentRicevute(receivedEmails);
    }


    @FXML
    public void paginaScriviMail() {
        scriviMail(null, null, null);
    }

    public void scriviMail(List<String> receivers, String subject, String text){
        final boolean[] campiPieni = {true};
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ScriviEmail.fxml"));
            Stage newStage = new Stage();
            newStage.setTitle("Scrivi mail");
            newStage.setScene(new Scene(fxmlLoader.load(), 700, 500));
            ScriviEmailController scriviEmailController = fxmlLoader.getController();
            scriviEmailController.initParameter(socket, client.emailAddressProperty().getValue(), this.outputStream, this.inputStream, receivers, subject, text); //magari fare iterfaccia Controller con tutti i metodi in comune e necessari
            newStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    campiPieni[0] = false;
                }
            });
            newStage.initModality(Modality.APPLICATION_MODAL);
            newStage.showAndWait();
            if(campiPieni[0]){
                System.out.println("paginaScriviMail");
                List<Email> emailAddress = (List<Email>) inputStream.readObject(); //ignorare il warning
                updateEmailList();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    @FXML
    public void mostraMail(){
        emailSelezionata = listEmail.getSelectionModel().getSelectedItem();
        updateDetailView();
    }

    private void updateDetailView() {
        if(this.emailSelezionata != null){
            DaContent.setText(emailSelezionata.getSender());
            AContent.setText(emailSelezionata.getReceivers().toString().replace("[", " ").replace("]", " ").trim());
            OggettoContent.setText(emailSelezionata.getSubject());
            TextContent.setText(emailSelezionata.getText());
        }
        else{
            DaContent.setText("");
            AContent.setText("");
            OggettoContent.setText("");
            TextContent.setText("");
        }
    }

    public void initParam(Socket socket, ObjectOutputStream outputStream, ObjectInputStream inputStream) {
        System.out.println("initParam");
        this.socket = socket;
        this.outputStream = outputStream;
        this.inputStream = inputStream;
    }

    @FXML
    public void mostraTuttiDest(){
        Stage stage = (Stage) AContent.getScene().getWindow();
        showTooltip(stage, AContent, AContent.getText());
    }

    public static void showTooltip(Stage owner, Control control, String tooltipText){
        Point2D p = control.localToScene(0, 23.0);

        final Tooltip customTooltip = new Tooltip();
        customTooltip.setWrapText(true);
        customTooltip.setPrefWidth(300);
        customTooltip.setText(tooltipText);

        control.setTooltip(customTooltip);
        customTooltip.setAutoHide(true);

        customTooltip.show(owner, p.getX()
                + control.getScene().getX() + control.getScene().getWindow().getX(), p.getY()
                + control.getScene().getY() + control.getScene().getWindow().getY());

    }

    @FXML
    public void eliminaMail() throws IOException, ClassNotFoundException {
        if(this.emailSelezionata != null){
            (this.outputStream).writeObject(new Messaggio(3, this.emailSelezionata));
            boolean response = (boolean) inputStream.readObject();
            if(response){
                (this.outputStream).writeObject(new Messaggio(3, client.emailAddressProperty().getValue().split("@")[0]));
                //client.setInboxContent((List<Email>) inputStream.readObject());
                aggiornaPagina();
            }
            this.emailSelezionata = null;
            updateDetailView();
        }
    }

    @FXML
    public void rispondiMittente(){
        if(this.emailSelezionata != null){
            List<String> receiver = new ArrayList<>();
            receiver.add(emailSelezionata.getSender());
            scriviMail(receiver, null, null);
        }
    }

    @FXML
    public void rispondiATutti(){
        if(this.emailSelezionata != null){
            List<String> receivers = new ArrayList<>();
            receivers.add(emailSelezionata.getSender());
            for(String e: emailSelezionata.getReceivers()){
                if(!(receivers.contains(e))){
                    receivers.add(e);
                }
            }
            scriviMail(receivers, null, null);
        }
    }

    @FXML
    public void inoltraMail(){
        if(this.emailSelezionata != null){
            scriviMail(null, emailSelezionata.getSubject(), emailSelezionata.getText());
        }
    }

    @FXML
    public void showRicevute(){
        (this.sceltaInviateRicevute).setText("Ricevute");
        client.setInboxContent(client.inboxPropertyRicevute());
    }

    @FXML
    public void showInviate(){
        (this.sceltaInviateRicevute).setText("Inviate");
        client.setInboxContent(client.inboxPropertyInviate());
    }

    @FXML
    public void aggiornaPagina() throws IOException, ClassNotFoundException {
        updateEmailList();
        if(this.sceltaInviateRicevute.getText().equals("Inviate"))
            showInviate();
        else
            showRicevute();
    }
}