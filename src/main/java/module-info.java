module com.example.hummingbird {
    requires javafx.fxml;
    requires javafx.controls;
    requires atlantafx.base;
    requires javafx.media;
    requires javafx.base;
    exports com.example.hummingbird.ui;
    opens com.example.hummingbird.controller to javafx.fxml;
    opens com.example.hummingbird.ui to javafx.fxml;
}