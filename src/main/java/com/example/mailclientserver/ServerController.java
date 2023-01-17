package com.example.mailclientserver;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ListProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;

import java.io.IOException;

public class ServerController {
    private ListProperty<StringProperty> consoleLog;
    private Server server;

    @FXML
    private TextArea consoleTextArea;
    @FXML
    private RadioButton statoServer;


    @FXML
    public void avviaServer() throws IOException{
        if(statoServer.isSelected()){
            Thread thread;
            this.server = new Server();
            thread = new Thread(this.server);
            thread.setDaemon(true);
            thread.setName("Thread_del_Server");
            thread.start();
            consoleListener();
        }else{
            if(this.server != null){
                this.server.stop();
            }
        }
    }

    private void consoleListener() {
        consoleLog = server.ConsoleLogProperty();
        consoleLog.addListener((InvalidationListener) change -> {
            StringProperty c = consoleLog.get(consoleLog.size() - 1);
            c.addListener((observableValue) -> Platform.runLater(()-> consoleTextArea.appendText(c.getValue() + "\n")));
        });
    }
}