module org.example.spotify {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires org.json; // Required for playing audio

    opens org.example.spotify to javafx.fxml; // Open your package to JavaFX
    exports org.example.spotify; // Export your package
}