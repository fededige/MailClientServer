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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import com.google.gson.Gson;

public class Server {
    private static ServerSocket serverSocket;
    private Socket socket = null;
    static int NUM_THREAD = 5;
    static ExecutorService exec = null;
    boolean flag = false;
    private static List<String> clientsEmail = new ArrayList<>();

    public void listen(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("in attesa di connesioni");
            int i = 0;
            flag = true;
            while (flag && i < 20){//out passata al task collegata all'in unico per tutti
                socket = serverSocket.accept();
                Runnable task = new ThreadedServer(socket, i, clientsEmail);
                i++;
                exec.execute(task);
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


    public static void main(String[] args) throws IOException {
        updateClients();
        checkFolders();
        exec = Executors.newFixedThreadPool(NUM_THREAD);
        System.out.println("Sono il server");
        Server server = new Server();
        server.listen(4445);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                //avvisiamo tutti i thread del threadpool di fare la close
                serverSocket.close();
                System.out.println("The server is shut down!");
            } catch (IOException e) { /* failed */ }
        }));
    }

}

class ThreadedServer implements Runnable {
    private Socket incoming;
    private int name;
    private List<String> clientEmails;

    public ThreadedServer(Socket incoming, int name, List<String> clientEmails) {
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
                while((m = (Messaggio)inStream.readObject()) != null){
                    System.out.println(m.getCod());
                    switch (m.getCod()){
                        case 0:
                            String emailAddr = (String) m.getContent();
                            System.out.println(emailAddr);
                            outStream.writeObject(checkEmail(emailAddr));
                            break;
                        case 1:
                            Email emailcompleta = (Email) m.getContent();
                            smistaEmail(emailcompleta);
                            break;
                        case 2:
                            String client = (String) m.getContent();
                            outStream.writeObject(updateEmailListSent(client));
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
                            break;
                        case 4:
                            outStream.writeObject(this.clientEmails);
                            break;
                        case 5:
                            client = (String) m.getContent();
                            outStream.writeObject(updateEmailListReceived(client));
                            break;
                        case 6: //TODO: gracefull shutdown
                    }
                }
                System.out.println("fuori dal while");
                Thread.sleep(15000);
            } catch (InterruptedException | ClassNotFoundException e) {
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

}
