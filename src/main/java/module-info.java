module com.example.mailclientserver {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.example.mailclientserver to javafx.fxml;
    exports com.example.mailclientserver;
    exports com.example.mailclientserver.messaggio;
    opens com.example.mailclientserver.messaggio to javafx.fxml;
}