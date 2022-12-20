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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Server {
    private static ServerSocket serverSocket;
    private Socket socket = null;
    static int NUM_THREAD = 5;
    static ExecutorService exec = null;
    boolean flag = false;
    private static List<String> clientEmails = new ArrayList<>();

    public void listen(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("in attesa di connesioni");
            int i = 0;
            flag = true;
            while (flag && i < 6){//out passata al task collegata all'in unico per tutti
                socket = serverSocket.accept();
                Runnable task = new ThreadedServer(socket, i, clientEmails);
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
                (clientEmails).add(line.trim());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        updateClients();
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
                            System.out.println(emailcompleta.getSender());
                            System.out.println(emailcompleta.getReceivers().get(0));
                            System.out.println(emailcompleta.getSubject());
                            System.out.println(emailcompleta.getText());
                            break;
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


    private String checkEmail(String emailAddress) {
        String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        return Pattern.compile(regexPattern).matcher(emailAddress).matches() && clientEmails.contains(emailAddress) ? emailAddress : "Email non valida";
    }

}
