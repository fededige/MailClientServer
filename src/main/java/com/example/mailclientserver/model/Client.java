package com.example.mailclientserver.model;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.LinkedList;
import java.util.List;

public class Client {
    private final ListProperty<Email> inbox;
    private final ObservableList<Email> inboxContent;
    private final StringProperty emailAddress;
    private final IntegerBinding emailsCount;


    public Client(String emailAddress) {
        this.inboxContent = FXCollections.observableList(new LinkedList<>());
        this.inbox = new SimpleListProperty<>();
        this.inbox.set(inboxContent);
        this.emailsCount = Bindings.size(inboxContent);
        this.emailAddress = new SimpleStringProperty(emailAddress);
    }

    public ListProperty<Email> inboxProperty() {
        return inbox;
    }

    public StringProperty emailAddressProperty() {
        return emailAddress;
    }

    public void deleteEmail(Email email) {
        inboxContent.remove(email);
    }

    public void setInboxContent(List<Email> emails) {
        inboxContent.remove(0, inboxContent.size());
        System.out.println("client:" + inboxContent.size());
        inboxContent.addAll(emails);
    }

    public IntegerBinding getEmailsCount(){
        return emailsCount;
    }
}