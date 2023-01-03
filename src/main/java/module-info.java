module com.example.mailclientserver {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires com.google.gson;
    requires org.json;

    exports com.example.mailclientserver;
    exports com.example.mailclientserver.messaggio;
    opens com.example.mailclientserver.messaggio to javafx.fxml;
    opens com.example.mailclientserver.model to com.google.gson;
    opens com.example.mailclientserver to com.google.gson, javafx.fxml;
}