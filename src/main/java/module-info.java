module com.example.untitledmediaplayer {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.untitledmediaplayer to javafx.fxml;
    exports com.example.untitledmediaplayer;
}