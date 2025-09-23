module com.example.hummingbird {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.hummingbird to javafx.fxml;
    exports com.example.hummingbird;
}