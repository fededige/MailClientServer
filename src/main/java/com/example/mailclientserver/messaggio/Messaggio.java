package com.example.mailclientserver.messaggio;

import java.io.Serializable;

public class Messaggio implements Serializable {
    private int cod;
    private Object content;

    public Messaggio(int cod, Object content) {
        this.cod = cod;
        this.content = content;
    }

    public int getCod() {
        return cod;
    }

    public Object getContent() {
        return content;
    }
}
