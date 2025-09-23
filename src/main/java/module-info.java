module com.example.untitledmediaplayer {
    requires javafx.fxml;
    requires atlantafx.base;


    opens com.example.untitledmediaplayer to javafx.fxml;
    exports com.example.untitledmediaplayer;
}