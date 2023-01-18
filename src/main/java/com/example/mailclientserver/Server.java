package com.example.mailclientserver;

import com.example.mailclientserver.messaggio.Messaggio;
import com.example.mailclientserver.model.Email;
import com.google.gson.Gson;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server implements Runnable{
    private static ServerSocket serverSocket;
    static int NUM_THREAD = 5;
    private final ExecutorService pool;
    boolean isRunning = true;
    private static final List<String> clientsEmail = new ArrayList<>();
    private final ListProperty<StringProperty> consoleLog;
    private static ObservableList<StringProperty> consoleLogContent;
    private final Lock lock;
    String host =  "127.0.0.1";
    int port = 4445;

    public Server() throws IOException {
        this.lock = new ReentrantLock();
        consoleLogContent = FXCollections.observableList(new LinkedList<>());
        this.consoleLog = new SimpleListProperty<>();
        this.consoleLog.set(consoleLogContent);
        updateClients();
        checkFolders();

        /*instanzia il threadpool*/
        this.pool = Executors.newFixedThreadPool(NUM_THREAD, (Runnable r) -> {
            Thread t = new Thread(r); t.setDaemon(true); return t;
        });
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            int i = 0;
            while (isRunning) {
                try {
                    ThreadedServer t = new ThreadedServer(serverSocket.accept(), i, clientsEmail, this.lock);
                    consoleLogContent.add(t.getAction());
                    pool.execute(t);
                    i++;
                }
                catch (SocketException e){
                    System.err.println("Closing ServerSocket...");
                }
            }
            System.out.println("Server stopped ");
        }  catch (IOException e) {
            pool.shutdown();
            e.printStackTrace();
        }
    }


    /*stop termina il threadpool e termina il thread del server*/
    public void stop() {
        pool.shutdown();
        isRunning = false;

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            System.out.println("chiudo pool...");
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                System.out.println("shutdownNow");
                pool.shutdownNow();
                if (!pool.awaitTermination(0, TimeUnit.SECONDS))
                    System.err.println("pool non terminato");
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    /*aggiunge tutti i client registrati in clientsEmail*/
    private static void updateClients() throws IOException{
        Path inputFilePath = Paths.get("D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/java/com/example/mailclientserver/clients_emails.txt");
        try(BufferedReader fileInputReader = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8)){
            String line;
            while((line = fileInputReader.readLine()) != null){
                (clientsEmail).add(line.trim());
            }
        }
    }

    /*controlla che ci siano le cartelle di tutti gli utenti registrati, se non ci sono le crea*/
    private static void checkFolders() throws IOException {
        String casellePath = "D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/java/com/example/mailclientserver/caselle";
        File file = new File(casellePath);
        String[] directories = file.list();
        List<String> directoriesList = new ArrayList<>();
        if(directories != null){
            directoriesList = Arrays.asList(directories);
        }
        for(String c : clientsEmail){
            String nomeClient = c.split("@")[0];
            if(!(directoriesList.contains(nomeClient)) || !(folderIsValid(nomeClient, casellePath))){
                Path path = Paths.get(casellePath + "/" +  nomeClient + "/inviate");
                Files.createDirectories(path);
                path = Paths.get(casellePath + "/" +  nomeClient + "/ricevute");
                Files.createDirectories(path);
            }
        }
    }

    /*controllo della correttezza delle sotto cartelle dei client registrati*/
    private static boolean folderIsValid(String nomeClient, String casellePath) {
        File file = new File(casellePath+"/"+nomeClient);
        String[] subDirectories = file.list();
        List<String> subDirectoriesList = new ArrayList<>();
        if(subDirectories != null){
            subDirectoriesList = Arrays.asList(subDirectories);
        }
        return subDirectoriesList.contains("inviate") && subDirectoriesList.contains("ricevute");
    }

    public ListProperty<StringProperty> ConsoleLogProperty(){
        return this.consoleLog;
    }


    /*classe innestata per permettere di gestire ogni connessione su un thread separato*/
    class ThreadedServer implements Runnable{
        Socket socket;
        private final int name;
        private final List<String> clientEmails;
        private final StringProperty action;
        private Lock lock;

        public ThreadedServer(Socket incoming, int name, List<String> clientEmails, Lock lock) {
            this.action = new SimpleStringProperty("connessione avvenuta");
            this.socket = incoming;
            this.name = name;
            this.clientEmails = clientEmails;
            this.lock = lock;
        }

        public void run() {
            ObjectOutputStream outStream = null;
            ObjectInputStream inStream = null;
            try {
                inStream = new ObjectInputStream(socket.getInputStream());
                outStream = new ObjectOutputStream(socket.getOutputStream());
                Messaggio m = null;
                boolean flag = true;
                while(flag && (m = (Messaggio) inStream.readObject()) != null && !Thread.currentThread().isInterrupted()){
                    switch (m.getCod()) {
                        case 0:
                            String emailAddr = (String) m.getContent();
                            String messaggio = "controllo di : " + emailAddr;
                            emailAddr = checkEmail(emailAddr);
                            outStream.writeObject(emailAddr);
                            action.setValue(this.name + "> " + messaggio + (emailAddr.equals("Client inesistente") ? ": Client inesistente" : ": Client esistente"));
                            break;
                        case 1:
                            Email emailcompleta = (Email) m.getContent();
                            messaggio = "";
                            for (String e : emailcompleta.getReceivers()) {
                                messaggio += e + ", ";
                            }
                            messaggio = messaggio.substring(0, messaggio.length() - 2);
                            smistaEmail(emailcompleta);
                            action.setValue(this.name + "> " + "invio mail a: " + messaggio);
                            break;
                        case 2:
                            String client = (String) m.getContent();
                            outStream.writeObject(updateEmailListSent(client));
                            action.setValue(this.name + "> " + "aggiornamento email inviate");
                            break;
                        case 3:
                            Email messaggioDaEliminare = (Email) m.getContent();
                            if (messaggioDaEliminare != null){
                                outStream.writeObject(true);
                                m = (Messaggio) inStream.readObject();
                                String clientReq = (String) m.getContent();
                                if (clientReq != null) {
                                    outStream.writeObject(true);
                                    m = (Messaggio) inStream.readObject();
                                    String casella = (String) m.getContent();
                                    try{
                                        eliminaMail(clientReq, messaggioDaEliminare, casella);
                                        action.setValue(this.name + "> " + "messaggio eliminato: " + messaggioDaEliminare.getId());
                                        System.out.println("cancellazione andata a buon fine");
                                        outStream.writeObject(true);
                                    }catch (IOException e){
                                        outStream.writeObject(false);
                                        e.printStackTrace();
                                    }
                                } else {
                                    outStream.writeObject(false);
                                }
                            } else {
                                outStream.writeObject(false);
                            }
                            break;
                        case 4:
                            outStream.writeObject(this.clientEmails); //serve per il menu a tendina
                            action.setValue(this.name + "> " + "caricamento utenti totali");
                            break;
                        case 5:
                            client = (String) m.getContent();
                            outStream.writeObject(updateEmailListReceived(client));
                            action.setValue(this.name + "> " + "aggiornamento email ricevute");
                            break;
                        case 6:
                            System.out.println("case 6");
                            action.setValue(this.name + "> " + "chiusura socket");
                            socket.close();
                            flag = false;
                            break;
                        default:
                            action.setValue(this.name + "> " + "azione non riconosciuta");
                            System.err.println("codice non riconosciuto");
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                closeSocket(outStream, inStream);
                System.out.println("End communication");
            }
        }

        private void closeSocket(ObjectOutputStream outStream, ObjectInputStream inStream) {
            try {
                if(outStream != null) {
                    outStream.close();
                }
                if(inStream != null) {
                    inStream.close();
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private synchronized void eliminaMail(String client, Email email, String casella) throws IOException {
            String casellePath = "D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/java/com/example/mailclientserver/caselle/";
            casellePath += client + "/" + casella + "/";
            casellePath += email.getId() + ".txt";
            Path path = Paths.get(casellePath);
            Files.delete(path);
        }

        private synchronized void smistaEmail(Email emailcompleta) {
            this.lock.lock();
            String casellePath = "D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/java/com/example/mailclientserver/caselle/";
            try {
                String[] tempo = (java.time.LocalDateTime.now().toString().substring(0, 24)).split(":");
                String tempTagliato = tempo[0] + "_" + tempo[1] + "_" + tempo[2];
                emailcompleta.setId(tempTagliato);
                emailcompleta.setDate(tempTagliato.split("T")[0]);
                Path newFilePath = Paths.get(casellePath + (emailcompleta.getSender()).split("@")[0] + "/inviate/" + tempTagliato + ".txt");
                Gson gson = new Gson();
                Files.write(newFilePath, gson.toJson(emailcompleta).getBytes());
                for(String receiver : emailcompleta.getReceivers()){
                    receiver = receiver.split("@")[0];
                    newFilePath = Paths.get(casellePath + receiver + "/ricevute/" + tempo[0] + "_" + tempo[1] + "_" + tempo[2] + ".txt");
                    gson = new Gson();
                    Files.write(newFilePath, gson.toJson(emailcompleta).getBytes());
                }
            }
            catch (IOException e) {
                System.out.println("Errore in smistaEmail: createFile");
            } finally {
                this.lock.unlock();
                System.out.println("dopo unlock smistaEmail");
            }
        }


        /*controllo se il client è registrato*/
        private String checkEmail(String emailAddress) {
            return clientEmails.contains(emailAddress) ? emailAddress : "Client inesistente";
        }


        private synchronized List<Email> updateEmailListSent(String client) {
            List<Email> emails = new ArrayList<>();
            String casellaPath = "D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/java/com/example/mailclientserver/caselle/" + client;
            File file = new File(casellaPath + "/inviate");
            String[] emailPaths = file.list();
            List<String> emailPathsList = new ArrayList<>();
            if(emailPaths != null){
                emailPathsList = Arrays.asList(emailPaths);
            }
            for(String name : emailPathsList){
                Path path = Paths.get(casellaPath + "/inviate" + "/" + name);
                try{
                    List<String> contents = Files.readAllLines(path);
                    Email email = new Gson().fromJson(contents.get(0), Email.class);
                    emails.add(email);
                }catch(IOException e){
                    System.out.println("Errore in updateEmailList: readAllLines");
                    e.printStackTrace();
                }
            }
            return emails;
        }

        private synchronized List<Email> updateEmailListReceived(String client) {
            List<Email> emails = new ArrayList<>();
            String casellaPath = "D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/java/com/example/mailclientserver/caselle/" + client;
            File file = new File(casellaPath + "/ricevute");
            String[] emailPaths = file.list();
            List<String> emailPathsList = new ArrayList<>();
            if(emailPaths != null){
                emailPathsList = Arrays.asList(emailPaths);
            }
            for(String name : emailPathsList){
                Path path = Paths.get(casellaPath + "/ricevute" + "/" + name);
                try{
                    List<String> contents = Files.readAllLines(path);
                    Email email = new Gson().fromJson(contents.get(0), Email.class);
                    emails.add(email);
                }catch(IOException e){
                    System.out.println("Errore in updateEmailList: readAllLines");
                    e.printStackTrace();
                }
            }
            return emails;
        }

        public StringProperty getAction() {
            return action;
        }
    }
}
