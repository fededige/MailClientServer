package com.example.mailclientserver;

import com.example.mailclientserver.messaggio.Messaggio;
import com.example.mailclientserver.model.Email;

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
    private Lock lock;

    public void listen(int port) throws IOException, InterruptedException {
        try {
            System.out.println("dentro listen");
            serverSocket = new ServerSocket(port);
            System.out.println("in attesa di connesioni");
            int i = 0;
            flag = true;
            while (flag){//out passata al task collegata all'in unico per tutti
                socket = serverSocket.accept();
                ThreadedServer t = new ThreadedServer(socket, i, clientsEmail, this.lock);
                consoleLogContent.add(t.getAction());
                i++;
                exec.execute(t);
            }
            exec.shutdown();
        }catch(SocketException ignored){
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    public void close() throws IOException, InterruptedException {
        System.out.println("close");
        System.out.println(Thread.currentThread().getName());
        if(!serverSocket.isClosed()) {
            System.out.println("serveSocket close");
            serverSocket.close();
        }
        exec.shutdown();
        if (!exec.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            System.out.println("after 100 millisecs");
            exec.shutdownNow();
        }
        if(exec.isShutdown())
            System.out.println("isShutdown");
        if(exec.isTerminated())
            System.out.println("isTerminated");
    }

    private static void updateClients() throws IOException{
        Path inputFilePath = Paths.get("D:/informatica/anno2023/Programmazione III/MailClientServer/src/main/java/com/example/mailclientserver/clients_emails.txt");
        try(BufferedReader fileInputReader = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8)){
            String line;
            while((line = fileInputReader.readLine()) != null){
                (clientsEmail).add(line.trim());
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
        this.lock = new ReentrantLock();

        consoleLogContent = FXCollections.observableList(new LinkedList<>());
        this.consoleLog = new SimpleListProperty<>();
        this.consoleLog.set(consoleLogContent);
        updateClients();
        checkFolders();
        exec = Executors.newFixedThreadPool(NUM_THREAD);
        System.out.println("Sono il server");
        Thread t1 = new Thread(() -> {
            try {
                listen(4445);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        t1.start();
    }
}

class ThreadedServer implements Runnable {
    private Socket incoming;
    private int name;
    private List<String> clientEmails;
    private StringProperty action;
    private Lock lock;


    public ThreadedServer(Socket incoming, int name, List<String> clientEmails, Lock lock) {
        this.action = new SimpleStringProperty("connessione avvenuta");
        System.out.println("connesione avvenuta");
        this.incoming = incoming;
        this.name = name;
        this.clientEmails = clientEmails;
        this.lock = lock;
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
                            if(messaggioDaEliminare != null) {
                                eliminaMail(clientReq, messaggioDaEliminare, casella);
                                action.setValue(this.name + "> " + "messaggio eliminato: " + messaggioDaEliminare.getId());
                            }
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
            } catch (SocketException se){
                System.out.println("socket closed");
            }catch (ClassNotFoundException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if(incoming.isClosed()) {
                    System.out.println("già chiuso");
                }
                incoming.close();
                System.out.println("chiusura avvenuta " + Thread.currentThread().getName());
                Thread.currentThread().interrupt();
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

    private synchronized void smistaEmail(Email emailcompleta) throws InterruptedException {
        System.out.println("dentro smistaEmail");
        this.lock.lock();
        System.out.println("dentro smistaEmail dopo lock");
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
