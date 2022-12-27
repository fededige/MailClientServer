package com.example.mailclientserver;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.io.IOException;

public class ServeController {
    ListProperty<StringProperty> consoleLog;

    @FXML
    private TextArea consoleTextArea;
    @FXML
    public void initialize() throws IOException {
        System.out.println("ciao");
        Server s = new Server();
        System.out.println("ciao");
        consoleLog = s.ConsoleLogProperty();
        consoleLog.addListener((InvalidationListener) change -> {
            for(StringProperty c : consoleLog){
                c.addListener((observableValue) -> consoleTextArea.appendText(c.getValue() + "\n"));
            }
        });
    }
}
