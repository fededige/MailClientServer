package com.example.mailclientserver;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ListProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;

import java.io.IOException;

public class ServerController {
    private ListProperty<StringProperty> consoleLog;
    private Server s = null;

    @FXML
    private TextArea consoleTextArea;
    @FXML
    private RadioButton statoServer;
//    @FXML
//    public void initialize() throws IOException {
//
//    }
    @FXML
    public void avviaServer() throws IOException, InterruptedException {
        if(statoServer.isSelected()){
             this.s = new Server();
            System.out.println("ciao");
            consoleLog = s.ConsoleLogProperty();
            consoleLog.addListener((InvalidationListener) change -> {
                for(StringProperty c : consoleLog){
                    c.addListener((observableValue) -> consoleTextArea.appendText(c.getValue() + "\n"));
                }
            });
        }else{
            if(this.s != null){
                consoleLog = null;
                this.s.close();
            }
        }
    }
}
