package com.example.hummingbird.controller;

import com.example.hummingbird.model.UserSession;
import com.example.hummingbird.ui.AudioPlayerUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

// Login Screen Controller:

public class LoginController {

    // connect to the UI elements from loginscene.fxml

    @FXML
    private TextField usernameField; // where user types their username

    @FXML
    private PasswordField passwordField; // where user types their password

    @FXML
    private Label errorLabel;// red text that shows error messages


    // what happens when user clicks the login button:

    @FXML
    void handleLogin(ActionEvent event) {
        System.out.println("Login button clicked");

        // get whatever the user typed in the username field
        String username = usernameField.getText();
        username = username.trim();  // remove extra spaces before/after

        // get whatever the user typed in the password field
        String password = passwordField.getText();

        // check if they actually typed something
        if (username.isEmpty() || password.isEmpty()) {
            // if either field is empty, show an error and stop here
            showError("Please enter both username and password");
            return;  //stop running this method
        }

        // try to log them in
        System.out.println("Attempting to login with username: " + username);
        File userDirectory = checkIfPasswordIsCorrect(username, password);

        // check if login worked
        if (userDirectory != null) {
            // the username and password were correct
            System.out.println("Login successful!");

            // save the user info so the whole program knows who's logged in
            UserSession.getInstance().login(username, userDirectory);

            // switch to the main music player screen
            loadMainMusicPlayer();

        } else {
            // the username or password was wrong
            System.out.println("Login failed (wrong username or password)");
            showError("Invalid username or password");
        }
    }


    // check if the username and password are correct. looks through all the user folders to find a matching username/password

    private File checkIfPasswordIsCorrect(String username, String password) {
        System.out.println("Checking credentials...");

        // create a File object pointing to the "users" folder
        File usersFolder = new File("users");

        // make sure the users folder actually exists
        if (!usersFolder.exists()) {
            System.err.println("ERROR: 'users' folder not found!");
            return null;
        }

        // get a list of all folders inside the users folder
        File[] userFolders = usersFolder.listFiles(File::isDirectory);

        // make sure we got something
        if (userFolders == null) {
            System.err.println("ERROR: Could not read user folders");
            return null;
        }

        // loop through each user folder
        for (File userFolder : userFolders) {
            System.out.println("Checking folder: " + userFolder.getName());

            // Look for the user_login.txt file inside this folder
            File loginFile = new File(userFolder, "user_login.txt");

            // check if this user has a login file
            if (loginFile.exists()) {
                // try to read the username and password from the file
                try {
                    // open the file
                    BufferedReader reader = new BufferedReader(new FileReader(loginFile));

                    // read username
                    String fileUsername = reader.readLine();
                    //read password
                    String filePassword = reader.readLine();

                    // close file
                    reader.close();

                    //make sure we actually got both lines from the file
                    if (fileUsername != null && filePassword != null) {
                        // remove any extra spaces
                        fileUsername = fileUsername.trim();
                        filePassword = filePassword.trim();

                        //compare what user typed with what's in the file
                        boolean usernameMatches = username.equals(fileUsername);
                        boolean passwordMatches = password.equals(filePassword);

                        if (usernameMatches && passwordMatches) {
                            // match found
                            System.out.println("Found matching credentials in: " + userFolder.getName());
                            return userFolder;  // Return this user's folder
                        }
                    }

                } catch (IOException e) {
                    // if something went wrong reading the file, print an error
                    System.err.println("ERROR reading " + loginFile.getName() + ": " + e.getMessage());
                }
            }
        }

        // no login found
        System.out.println("No matching credentials found");
        return null;
    }

    // show error messages to the user

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);    //make text visible (it's hidden by default)
    }

    // load the main music player after successful login

    private void loadMainMusicPlayer() {
        try {
            System.out.println("Loading main music player...");

            // get the window (Stage) that's currently showing the login screen
            Stage window = (Stage) usernameField.getScene().getWindow();

            // load the FXML file for the main music player
            FXMLLoader loader = new FXMLLoader(AudioPlayerUI.class.getResource("/com/example/hummingbird/mainscene.fxml"));

            // create a new scene with the loaded FXML (700 pixels wide, 800 pixels tall)
            Scene mainPlayerScene = new Scene(loader.load(), 700, 800);

            // change the window to show the new scene
            window.setScene(mainPlayerScene);

            // update the window title to show who's logged in
            String username = UserSession.getInstance().getUsername();
            window.setTitle("Hummingbird - " + username);

            System.out.println("Main player loaded successfully!");

        } catch (IOException e) {
            //print error
            e.printStackTrace();
            showError("Error loading main player");
        }
    }
}