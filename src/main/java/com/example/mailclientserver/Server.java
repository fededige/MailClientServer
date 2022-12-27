package com.example.mailclientserver;

import com.example.mailclientserver.messaggio.Messaggio;
import com.example.mailclientserver.model.Email;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
import com.google.gson.Gson;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Server {
    private static ServerSocket serverSocket;
    private Socket socket = null;
    static int NUM_THREAD = 5;
    static ExecutorService exec = null;
    boolean flag = false;
    private static List<String> clientsEmail = new ArrayList<>();
    private ListProperty<StringProperty> consoleLog;
    private static ObservableList<StringProperty> consoleLogContent;

    public void listen(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("in attesa di connesioni");
            int i = 0;
            flag = true;
            while (flag && i < 20){//out passata al task collegata all'in unico per tutti
                socket = serverSocket.accept();
                ThreadedServer t = new ThreadedServer(socket, i, clientsEmail);
                consoleLogContent.add(t.getAction());
                i++;
                exec.execute(t);
            }
            exec.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket!=null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void updateClients() throws IOException{
        Path inputFilePath = Paths.get("D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/java/com/example/mailclientserver/clients_emails.txt");
        try(BufferedReader fileInputReader = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8)){
            String line = null;
            while((line = fileInputReader.readLine()) != null){
                (clientsEmail).add(line.trim()); //.split("@")[0] forse
            }
        }
    }
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


    public Server() throws IOException {
        consoleLogContent = FXCollections.observableList(new LinkedList<>());
        consoleLog = new SimpleListProperty<>();
        consoleLog.set(consoleLogContent);
        updateClients();
        checkFolders();
        exec = Executors.newFixedThreadPool(NUM_THREAD);
        System.out.println("Sono il server");
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                listen(4445);
            }
        });
        t1.start();
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            try {
//                //avvisiamo tutti i thread del threadpool di fare la close
//                serverSocket.close();
//                System.out.println("The server is shut down!");
//            } catch (IOException e) { /* failed */ }
//        }));
    }

}

class ThreadedServer implements Runnable {
    private Socket incoming;
    private int name;
    private List<String> clientEmails;
    private StringProperty action;

    public ThreadedServer(Socket incoming, int name, List<String> clientEmails) {
        this.action = new SimpleStringProperty("connessione avvenuta");
        System.out.println("connesione avvenuta");
        this.incoming = incoming;
        this.name = name;
        this.clientEmails = clientEmails;
    }

    public void run() {
        try {
            try {
                System.out.println("RUN connesione avvenuta:" + " " + Thread.currentThread().getName());
                ObjectInputStream inStream = new ObjectInputStream(incoming.getInputStream());
                //System.out.println("dopo in");
                ObjectOutputStream outStream = new ObjectOutputStream(incoming.getOutputStream());
                //System.out.println("dopo out");
                Messaggio m = null;
                boolean flag = true;
                while(flag && (m = (Messaggio)inStream.readObject()) != null){
                    System.out.println(m.getCod());
                    switch (m.getCod()){
                        case 0:
                            String emailAddr = (String) m.getContent();
                            String messaggio = "controllo di : " + emailAddr;
                            emailAddr = checkEmail(emailAddr);
                            outStream.writeObject(emailAddr);
                            action.setValue(this.name + "> " + messaggio + (emailAddr.equals("Client inesistente") ? ": Cliente inesistente" : ": Client esistente"));
                            break;
                        case 1:
                            Email emailcompleta = (Email) m.getContent();
                            messaggio = "";
                            for(String e : emailcompleta.getReceivers()){
                                messaggio += e + ", ";
                            }
                            if(messaggio != null)
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
                            if(messaggioDaEliminare != null)
                                outStream.writeObject(true);
                            m = (Messaggio)inStream.readObject();
                            String clientReq = (String) m.getContent();
                            if(clientReq != null)
                                outStream.writeObject(true);
                            m = (Messaggio)inStream.readObject();
                            String casella = (String) m.getContent();
                            eliminaMail(clientReq, messaggioDaEliminare, casella);
                            action.setValue(this.name + "> " + "messaggio eliminato: " + messaggioDaEliminare.getId());
                            break;
                        case 4:
                            outStream.writeObject(this.clientEmails); //serve per il menu a tendina
                            action.setValue(this.name + "> "+ "caricamento utenti totali");
                            break;
                        case 5:
                            client = (String) m.getContent();
                            outStream.writeObject(updateEmailListReceived(client));
                            action.setValue(this.name + "> " + "aggiornamento email ricevute");
                            break;
                        case 6:
                            System.out.println("case 6");
                            action.setValue(this.name + "> " + "chiusura socket");
                            incoming.close();
                            flag = false;
                            break;
                    }
                }
                System.out.println("fuori dal while");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                incoming.close();
                System.out.println("chiusura avvenuta");
            }
        }
        catch (IOException e) {e.printStackTrace();}
    }

    private synchronized void eliminaMail(String client, Email email, String casella) throws IOException {
        String casellePath = "D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/java/com/example/mailclientserver/caselle/";
        casellePath += client + "/" + casella + "/";
        casellePath += email.getId() + ".txt";
        Path path = Paths.get(casellePath);
        Files.delete(path);
    }

    private synchronized void smistaEmail(Email emailcompleta) {
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
        }
    }


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
        System.out.println("STAMPA EMAIL INVIATE");
        for(Email es : emails){
            System.out.println(es.getSender());
            for(String s : es.getReceivers()){
                System.out.println("\t" + s);
            }
        }
        System.out.println("FINE EMAIL INVIATE");
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
        System.out.println("STAMPA EMAIL RICEVUTE");
        for(Email es : emails){
            System.out.println(es.getSender());
            for(String s : es.getReceivers()){
                System.out.println("\t" + s);
            }
        }
        System.out.println("FINE EMAIL RICEVUTE");
        return emails;
    }

    public StringProperty getAction() {
        return action;
    }

    public StringProperty actionProperty() {
        return action;
    }

    public void setAction(String action) {
        this.action.set(action);
    }
}
