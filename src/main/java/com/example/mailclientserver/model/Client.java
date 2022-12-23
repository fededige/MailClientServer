package com.example.mailclientserver.model;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.LinkedList;
import java.util.List;

public class Client {
    private final ListProperty<Email> inboxInviate;
    private final ListProperty<Email> inboxRicevute;
    private final ObservableList<Email> inboxContentInviate;
    private final ObservableList<Email> inboxContentRicevute;
    private final ListProperty<Email> inbox;
    private final ObservableList<Email> inboxContent;
    private final StringProperty emailAddress;
    private final IntegerBinding emailsCount;


    public Client(String emailAddress) {
        this.inboxContentInviate = FXCollections.observableList(new LinkedList<>());
        this.inboxInviate = new SimpleListProperty<>();
        this.inboxInviate.set(inboxContentInviate);
        this.inboxContentRicevute = FXCollections.observableList(new LinkedList<>());
        this.inboxRicevute = new SimpleListProperty<>();
        this.inboxRicevute.set(inboxContentRicevute);


        /*****************************************************/
        this.inboxContent = FXCollections.observableList(new LinkedList<>());
        this.inbox = new SimpleListProperty<>();
        this.inbox.set(inboxContent);
        this.emailsCount = Bindings.size(inboxContent);
        this.emailAddress = new SimpleStringProperty(emailAddress);
    }

    public ListProperty<Email> inboxProperty() {
        return inbox;
    }

    public ListProperty<Email> inboxPropertyInviate() {
        return inboxInviate;
    }

    public ListProperty<Email> inboxPropertyRicevute() {
        return inboxRicevute;
    }

    public StringProperty emailAddressProperty() {
        return emailAddress;
    }

    public void deleteEmail(Email email) { //come mai non è mai usato?
        inboxContent.remove(email);
    }

    public void setInboxContent(List<Email> emails) {
        inboxContent.remove(0, inboxContent.size());
        inboxContent.addAll(emails);
    }

    public void setInboxContentInviate(List<Email> emailsInviate) {
        inboxContentInviate.remove(0, inboxContentInviate.size());
        inboxContentInviate.addAll(emailsInviate);
    }

    public void setInboxContentRicevute(List<Email> emailsRicevute) {
        inboxContentRicevute.remove(0, inboxContentRicevute.size());
        inboxContentRicevute.addAll(emailsRicevute);
    }

    public IntegerBinding getEmailsCount(){
        return emailsCount;
    }
}