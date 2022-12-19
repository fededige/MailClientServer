package com.example.mailclientserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Server {
    private static ServerSocket serverSocket;
    private Socket socket = null;
    static int NUM_THREAD = 5;
    static ExecutorService exec = null;
    boolean flag = false;

    public void listen(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("in attesa di connesioni");
            int i = 0;
            flag = true;
            while (flag && i < 6){//out passata al task collegata all'in unico per tutti
                socket = serverSocket.accept();
                Runnable task = new ThreadedServer(socket, i);
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

    public static void main(String[] args){
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

    public ThreadedServer(Socket incoming, int name) {
        System.out.println("connesione avvenuta");
        this.incoming = incoming;
        this.name = name;
    }

    public void run() {
        try {
            try {
                System.out.println("RUN connesione avvenuta:" + " " + Thread.currentThread().getName());
                BufferedReader in =  new BufferedReader(new InputStreamReader(incoming.getInputStream()));
                Writer out = new BufferedWriter(new OutputStreamWriter(incoming.getOutputStream(), StandardCharsets.UTF_8));
                String line = null;
                while((line = in.readLine()) != null){
                    System.out.println("line: " + line);
                    out.append(checkEmail(line)).append("\n");
                    out.flush();
                }
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                incoming.close();
                System.out.println("chiusura avvenuta");
            }
        }
        catch (IOException e) {e.printStackTrace();}
    }


    private String checkEmail(String emailAddress) {
        System.out.println(emailAddress);
        String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        return Pattern.compile(regexPattern).matcher(emailAddress).matches() ? emailAddress : "Email non valida";
    }

}
