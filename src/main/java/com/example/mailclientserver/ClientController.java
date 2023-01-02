package com.example.mailclientserver;

import com.example.mailclientserver.messaggio.Messaggio;
import com.example.mailclientserver.model.Client;
import com.example.mailclientserver.model.Email;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientController {
    private Socket socket;
    private Client client;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private Email emailSelezionata;
    private List<String> clientList;
    private String ip;
    private int port;
    private boolean connessi;
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
    private Label dataEmailLabel;
    @FXML
    private Label labelStato;
    @FXML
    private ImageView pallinoStato;


    public void riconnessioneSocket(){
        try{
            if(outputStream != null)
                outputStream.close();
            if(inputStream != null)
                inputStream.close();
            if(!socket.isClosed()) {
                System.out.println("Closing socket");
                this.socket.close();
            }
        } catch (IOException e){
            System.out.println("Errore socket.close in riconnessioneSocket");
        }
        if(connessi){
            connessi = false;
            labelStato.setText("Offline");
            Image image = new Image("D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/resources/img/pallino_disconnesso.png");
            pallinoStato.setImage(image);
            System.out.println("prima disconnessione");
            Thread t1 = new Thread(() -> {
                while(true) {
                    try {
                        this.socket = new Socket(this.ip, this.port);
                        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
                        this.inputStream = new ObjectInputStream(socket.getInputStream());
                        connessi = true;
                        Platform.runLater(()-> {
                            labelStato.setText("Online");
                            Image im = new Image("D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/resources/img/pallino_connesso.png");
                            pallinoStato.setImage(im);
                        });
                        break;
                    } catch(IOException e) {
                        System.out.println("Reconnect failed, retrying");
                        try {
                            Thread.sleep(5 * 1000);
                        } catch(InterruptedException ie) {
                            System.out.println("Interrupted");
                        }
                    }
                }
            });
            t1.start();
        }
        else
            System.out.println("altre disconnessioni non provo a riconnettermi");
    }
    @FXML
    public void initialize(Stage stage, String emailAddress) throws Exception {
        stage.setOnCloseRequest(t -> {
            System.out.println("shutdown");
            try {
                (this.outputStream).writeObject(new Messaggio(6, null));
                socket.close();
            } catch (SocketException se){
                System.out.println("socket chiuso");
                riconnessioneSocket();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            Platform.exit();
            System.exit(0);
        });
        if (this.client != null)
            throw new IllegalStateException("Model can only be initialized once");
        if(emailAddress != null){
            client = new Client(emailAddress);
            updateEmailList();
            updateClientList();
            client.setInboxContent(client.inboxPropertyRicevute());
        }
        else{
            throw new Exception("Email non valida");
        }
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
            exec.scheduleAtFixedRate(
                new Update(),
                    30, //temp
                30,
                TimeUnit.SECONDS
        );
        emailSelezionata = null;
        emailAddressLabel.textProperty().bind(client.emailAddressProperty());
        counterEmails.textProperty().bind(client.getEmailsCount().asString());
        listEmail.itemsProperty().bind(client.inboxProperty());
    }

    private void updateClientList() {
        try {
            (this.outputStream).writeObject(new Messaggio(4, null));
            this.clientList = (List<String>) inputStream.readObject();
        } catch (SocketException se){
            System.out.println("socket chiuso");
            riconnessioneSocket();
        }catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateEmailList() throws ClassNotFoundException {
        try {
            (this.outputStream).writeObject(new Messaggio(2, client.emailAddressProperty().getValue().split("@")[0]));
            client.setInboxContentInviate((List<Email>) inputStream.readObject());
            (this.outputStream).writeObject(new Messaggio(5, client.emailAddressProperty().getValue().split("@")[0]));
            client.setInboxContentRicevute((List<Email>) inputStream.readObject());
        } catch (IOException e){
            System.out.println("socket chiuso updateEmailList");
            riconnessioneSocket();
        }

    }

    private void showDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nuovo Messaggio");
        alert.setHeaderText(null);
        alert.setContentText("Hai un nuovo Messaggio!");
        alert.showAndWait();
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
            scriviEmailController.initParameter(socket, client.emailAddressProperty().getValue(), this.outputStream, this.inputStream, receivers, subject, text, this.clientList); //magari fare iterfaccia Controller con tutti i metodi in comune e necessari
            newStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    campiPieni[0] = false;
                }
            });
            newStage.initModality(Modality.APPLICATION_MODAL);
            newStage.showAndWait();
            if(campiPieni[0]){
                aggiornaPagina();
            }
        } catch (ClassNotFoundException | IOException e) {
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
            System.out.println(emailSelezionata.getId());
            System.out.println(emailSelezionata.getDate());
            dataEmailLabel.setText(emailSelezionata.getDate());
        }
        else{
            DaContent.setText("");
            AContent.setText("");
            OggettoContent.setText("");
            TextContent.setText("");
            dataEmailLabel.setText("");
        }
    }

    public void initParam(Socket socket, ObjectOutputStream outputStream, ObjectInputStream inputStream, String ip, int port) {
        this.socket = socket;
        this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.port = port;
        this.ip = ip;
        this.connessi = true;
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
    public void eliminaMail() throws ClassNotFoundException {
        try{
            if (this.emailSelezionata != null) {
                (this.outputStream).writeObject(new Messaggio(3, this.emailSelezionata));
                boolean response = (boolean) inputStream.readObject();
                if (response) {
                    (this.outputStream).writeObject(new Messaggio(3, client.emailAddressProperty().getValue().split("@")[0]));
                }
                response = (boolean) inputStream.readObject();
                if (response) {
                    (this.outputStream).writeObject(new Messaggio(3, (this.sceltaInviateRicevute).getText()));
                    aggiornaPagina();
                }
                this.emailSelezionata = null;
                updateDetailView();
            }
        }
        catch (SocketException se){
            System.out.println("socket chiuso");
            riconnessioneSocket();
        }
        catch (IOException e){
            throw new RuntimeException(e);
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
    public void aggiornaPagina() throws ClassNotFoundException {
        int oldCount = client.inboxPropertyRicevute().size();
        updateEmailList();
        if(this.sceltaInviateRicevute.getText().equals("Inviate"))
            showInviate();
        else
            showRicevute();
        if(client.inboxPropertyRicevute().size() > oldCount)
            showDialog();
    }

    private class Update implements Runnable{
        public void run() {
            Platform.runLater(()-> {
                try {
                    aggiornaPagina();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}