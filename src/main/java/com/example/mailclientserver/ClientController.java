package com.example.mailclientserver;

import com.example.mailclientserver.messaggio.Messaggio;
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
    private Client client;
    private Socket socket;
    private ObjectOutputStream outStream;
    private ObjectInputStream inStream;
    private final String ip = "127.0.0.1";
    private final int port = 4445;
    private Email emailSelezionata;
    private List<String> clientList;
    private boolean connessi = true;

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

    /*
    * init chiamo openConnection per aprire le connessioni, e openStream per aprire gli streams.
    *
    *
    * */
    public void init(String emailAddress, Stage stage) throws IOException, ClassNotFoundException {
        openConnection();
        openStreams();

        if(client != null)
            throw new IllegalStateException("Model can only be initialized once");

        outStream.writeObject(new Messaggio(0, emailAddress)); //controlla se il client è esistente, se lo è instanzia il Client

        if(!inStream.readObject().equals("Client inesistente")){
            client = new Client(emailAddress);
            updateEmailList();
            updateClientList();
            client.setInboxContent(client.inboxPropertyRicevute()); //imposta in contenuto dell'inbox visibile con le mail ricevute
        }else{
            outStream.writeObject(new Messaggio(6, null)); //chiudiamo la connessione se il client è inesistente
            socket.close();
            Platform.exit();
            System.exit(0);
        }

        stage.setOnCloseRequest(t -> { //modifichiamo il comportamento del click sul bottone per chiudere la finestra
            try {
                (outStream).writeObject(new Messaggio(6, null));
                socket.close();
            } catch (SocketException se){
                System.err.println("socket.close failed when X clicked");
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            Platform.exit();
            System.exit(0);
        });

        //instanziamo un ScheduledExecutorService in modo tale da poter avviare un Thread ogni 30 secondi per permettere l'aggiornamento del client
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

    /*apre la connessione creando un nuovo Socket, se l'operazione fallisce attende 10 secondi e ritenta*/
    public void openConnection(){
        while(true) {
            try {
                socket = new Socket(ip, port);
                break;
            } catch(IOException e) {
                System.err.println("Reconnect failed, wait");
                try {
                    Thread.sleep(10*1000);
                } catch(InterruptedException ie) {
                    System.err.println("Interrupted");
                }
            }
        }
        System.out.println("Client connesso");
    }

    /* apre gli stream necessari alla connessione corrente */
    private void openStreams() {
        try {
            outStream = new ObjectOutputStream(socket.getOutputStream());
            inStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /*riconnessioneSocket serve per cercare di riconnettere il client al server nel caso in cui vada offline*/
    public void riconnessioneSocket(){
        try{
            if(outStream != null)
                outStream.close();
            if(inStream != null)
                inStream.close();
            if(!socket.isClosed()) {
                this.socket.close();
            }
        } catch (IOException e){
            System.err.println("Errore in riconnessioneSocket");
        }
        if(connessi){
            connessi = false;
            labelStato.setText("Offline");
            Image image = new Image("D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/resources/img/pallino_disconnesso.png");
            pallinoStato.setImage(image);
            Thread t1 = new Thread(() -> {
                while(true) {
                    try {
                        this.socket = new Socket(this.ip, this.port);
                        this.outStream = new ObjectOutputStream(socket.getOutputStream());
                        this.inStream = new ObjectInputStream(socket.getInputStream());
                        connessi = true;
                        Platform.runLater(()-> {
                            labelStato.setText("Online");
                            Image im = new Image("D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/resources/img/pallino_connesso.png");
                            pallinoStato.setImage(im);
                        });
                        break;
                    } catch(IOException e) {
                        System.err.println("Reconnect failed, retrying");
                        try {
                            Thread.sleep(5 * 1000);
                        } catch(InterruptedException ie) {
                            System.err.println("Interrupted");
                        }
                    }
                }
            });
            t1.start();
        }
        else
            System.err.println("altre disconnessioni non provo a riconnettermi");
    }


    /*updateClientList aggiorna la lista di tutti i client registrati per permettere la visualizzazione del menu a tendina della GUI per scrivere l'email*/
    private void updateClientList() {
        try {
            (this.outStream).writeObject(new Messaggio(4, null));
            this.clientList = (List<String>) inStream.readObject();
        } catch (SocketException se){
            System.out.println("socket chiuso");
            riconnessioneSocket();
        }catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    /*updateEmailList aggiorna il contenuto delle email inviate e ricevute dal client*/
    private void updateEmailList() throws ClassNotFoundException {
        try {
            (this.outStream).writeObject(new Messaggio(2, client.emailAddressProperty().getValue().split("@")[0]));
            client.setInboxContentInviate((List<Email>) inStream.readObject()); //TODO risolvere errore
            (this.outStream).writeObject(new Messaggio(5, client.emailAddressProperty().getValue().split("@")[0]));
            client.setInboxContentRicevute((List<Email>) inStream.readObject());
        } catch (IOException e){
            System.err.println("errore in updateEmailList");
            riconnessioneSocket();
        }
    }

    /*mostra un popUp di avviso nel caso siano arrivate nuove mail*/
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

    /*scriviMial avvia la GUI per la scrittura di una nuova mail e aggiorna la la lista delle email*/
    public void scriviMail(List<String> receivers, String subject, String text){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ScriviEmail.fxml"));
            Stage newStage = new Stage();
            newStage.setTitle("Scrivi mail");
            newStage.setScene(new Scene(fxmlLoader.load(), 700, 500));
            ScriviEmailController scriviEmailController = fxmlLoader.getController();
            scriviEmailController.initParameter(socket, client.emailAddressProperty().getValue(), this.outStream, this.inStream, receivers, subject, text, this.clientList); //magari fare iterfaccia Controller con tutti i metodi in comune e necessari
            newStage.initModality(Modality.APPLICATION_MODAL);
            newStage.showAndWait(); //aspettiamo la chiusura di newStage
            aggiornaPagina();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /*mostraMail mostra la mail selezionata*/
    @FXML
    public void mostraMail(){
        emailSelezionata = listEmail.getSelectionModel().getSelectedItem();
        updateDetailView();
    }

    /*updateDatailView mostra la mailSelezionata nei campi appositi della GUI*/
    private void updateDetailView() {
        if(this.emailSelezionata != null){
            DaContent.setText(emailSelezionata.getSender());
            AContent.setText(emailSelezionata.getReceivers().toString().replace("[", " ").replace("]", " ").trim());
            OggettoContent.setText(emailSelezionata.getSubject());
            TextContent.setText(emailSelezionata.getText());
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

    /*mostra il tooltip relativo al campo A della GUI*/
    @FXML
    public void mostraTuttiDest(){
        if(AContent.getText().length() > 90){
            Stage stage = (Stage) AContent.getScene().getWindow();
            showTooltip(stage, AContent, AContent.getText());
        }
    }


    /*funzione per mostrare il tooltip subito sotto al widget scelto*/
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

    /*eliminaMail server per eliminare la mailSelezionata*/
    @FXML
    public void eliminaMail(){
        try{
            if (this.emailSelezionata != null) {
                (this.outStream).writeObject(new Messaggio(3, this.emailSelezionata)); //inviamo la mail selezionata
                boolean response = (boolean) inStream.readObject();
                if (response) {
                    (this.outStream).writeObject(new Messaggio(3, client.emailAddressProperty().getValue().split("@")[0])); //inviamo il nomeutente del client che ha richiesto l'eliminazione
                    response = (boolean) inStream.readObject();
                    if (response) {
                        (this.outStream).writeObject(new Messaggio(3, (this.sceltaInviateRicevute).getText()));//indichiamo al servere da quale cartella abbiamo scelto la mail
                        response = (boolean) inStream.readObject();
                        if(response){
                            aggiornaPagina();
                            this.emailSelezionata = null;
                            updateDetailView();
                        }
                    }
                }
            }
        }
        catch (SocketException se){
            System.out.println("socket chiuso");
            riconnessioneSocket();
        }
        catch (IOException | ClassNotFoundException e){
            throw new RuntimeException(e);
        }
    }


    /*apre la GUI per inviare una mail specificando il mittente della mail selezionata come destinatario (ripondi)*/
    @FXML
    public void rispondiMittente(){
        if(this.emailSelezionata != null){
            List<String> receiver = new ArrayList<>();
            receiver.add(emailSelezionata.getSender());
            scriviMail(receiver, null, null);
        }
    }


    /*apre la GUI per inviare una mail specificando il mittente e tutti i destinatari della mail selezionata come destinatari della nuova mail (ripondi)*/
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


    /*apre la GUI per inviare una mail specificando l'oggetto e il testo della mail selezionata*/
    @FXML
    public void inoltraMail(){
        if(this.emailSelezionata != null){
            scriviMail(null, emailSelezionata.getSubject(), emailSelezionata.getText());
        }
    }

    //TODO: controllare client multipli

    /*modifica il contenuto dell'inbox del client mettendoci solo le mail ricevute*/
    @FXML
    public void showRicevute(){
        (this.sceltaInviateRicevute).setText("Ricevute");
        client.setInboxContent(client.inboxPropertyRicevute());
    }


    /*modifica il contenuto dell'inbox del client mettendoci solo le mail inviate*/
    @FXML
    public void showInviate(){
        (this.sceltaInviateRicevute).setText("Inviate");
        client.setInboxContent(client.inboxPropertyInviate());
    }


    /*aggiornaPagina aggiorna il contenuto della GUI, se il numero di mail ricevute è aumentato (il client ha ricevuto una mail) mostra un popUp*/
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


    /*classe interna che impelenta Runnable, lanciata da ScheduledExecutorService per permettere di aggiornare la pagina ogni 30 sec*/
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