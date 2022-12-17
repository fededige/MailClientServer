package com.example.mailclientserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

}
