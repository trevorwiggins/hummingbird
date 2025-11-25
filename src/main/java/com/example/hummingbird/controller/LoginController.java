package com.example.hummingbird.controller;

import com.example.hummingbird.model.UserSession;
import com.example.hummingbird.application.AudioPlayerApplication;
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

import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

/**
 * Controller for the login screen.
 *
 * Responsibilities:
 * <ul>
 *     <li>Authenticate users against local on-disk user folders.</li>
 *     <li>Display errors when credentials are invalid or missing.</li>
 *     <li>Allow creation of new user accounts and folder structure.</li>
 *     <li>On success, store the logged-in user in UserSession and load the main player view.</li>
 * </ul>
 */
public class LoginController {

    // ===================== FXML-INJECTED UI CONTROLS =====================

    /** Username input field. */
    @FXML
    private TextField usernameField;

    /** Password input field (masked). */
    @FXML
    private PasswordField passwordField;

    /** Label used to display login and account-creation error messages. */
    @FXML
    private Label errorLabel;

    // ===================== LOGIN HANDLING =====================

    /**
     * Invoked when the user presses the "Login" button.
     * <ol>
     *     <li>Reads username and password from the text fields.</li>
     *     <li>Validates that both fields are non-empty.</li>
     *     <li>Searches the local "users" directory for matching credentials.</li>
     *     <li>If valid, logs the user in and loads the main music player.</li>
     *     <li>If invalid, shows an error message.</li>
     * </ol>
     */
    @FXML
    void handleLogin(ActionEvent event) {
        System.out.println("Login button clicked");

        String username = usernameField.getText();
        username = username.trim();

        String password = passwordField.getText();

        // Both fields must be provided
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        System.out.println("Attempting to login with username: " + username);
        File userDirectory = checkIfPasswordIsCorrect(username, password);

        if (userDirectory != null) {
            // Successful authentication
            System.out.println("Login successful!");

            UserSession.getInstance().login(username, userDirectory);
            loadMainMusicPlayer();
        } else {
            // Authentication failed
            System.out.println("Login failed (wrong username or password)");
            showError("Invalid username or password");
        }
    }

    /**
     * Checks the given username and password against all user folders on disk.
     * <p>
     * Directory layout:
     * <pre>
     * users/
     *   someUser/
     *     user_login.txt  (line 1: username, line 2: password)
     *     ...
     * </pre>
     *
     * @param username username entered on the login screen
     * @param password password entered on the login screen
     * @return the matching user's folder if credentials are correct; {@code null} otherwise
     */
    private File checkIfPasswordIsCorrect(String username, String password) {
        System.out.println("Checking credentials...");

        File usersFolder = new File("users");

        if (!usersFolder.exists()) {
            System.err.println("ERROR: 'users' folder not found!");
            return null;
        }

        File[] userFolders = usersFolder.listFiles(File::isDirectory);
        if (userFolders == null) {
            System.err.println("ERROR: Could not read user folders");
            return null;
        }

        // Walk each user directory and look for a matching user_login.txt file
        for (File userFolder : userFolders) {
            System.out.println("Checking folder: " + userFolder.getName());

            File loginFile = new File(userFolder, "user_login.txt");

            if (loginFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(loginFile))) {

                    String fileUsername = reader.readLine();
                    String filePassword = reader.readLine();

                    if (fileUsername != null && filePassword != null) {
                        fileUsername = fileUsername.trim();
                        filePassword = filePassword.trim();

                        boolean usernameMatches = username.equals(fileUsername);
                        boolean passwordMatches = password.equals(filePassword);

                        if (usernameMatches && passwordMatches) {
                            System.out.println("Found matching credentials in: " + userFolder.getName());
                            return userFolder;
                        }
                    }

                } catch (IOException e) {
                    System.err.println("ERROR reading " + loginFile.getName() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("No matching credentials found");
        return null;
    }

    /**
     * Displays an error message in the error label and ensures it is visible.
     *
     * @param message text to show to the user
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    // ===================== CREATE ACCOUNT DIALOG =====================

    /**
     * Invoked when the user presses the "Create Account" button/link.
     * <p>
     * Shows a dialog that asks for a new username and password, validates
     * the input, creates the user folder structure on disk, copies sample
     * songs, and optionally logs the user in immediately.
     */
    @FXML
    void handleShowCreateAccountDialog(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New User");
        dialog.setHeaderText("Create a new Hummingbird account");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Build simple username/password form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField newUsernameField = new TextField();
        newUsernameField.setPromptText("Username");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(newUsernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(newPasswordField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Disable "Create" until both fields contain non-blank text
        var createButton = dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);

        newUsernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            createButton.setDisable(newVal.trim().isEmpty() || newPasswordField.getText().trim().isEmpty());
        });

        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            createButton.setDisable(newVal.trim().isEmpty() || newUsernameField.getText().trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> dialogButton);

        var result = dialog.showAndWait();
        if (result.isPresent() && result.get() == createButtonType) {
            String newUsername = newUsernameField.getText().trim();
            String newPassword = newPasswordField.getText();

            String error = validateNewUserInput(newUsername, newPassword);
            if (error != null) {
                showError(error);
                return;
            }

            try {
                File newUserDir = createNewUserOnDisk(newUsername, newPassword);
                if (newUserDir == null) {
                    showError("Could not create user. Please try again.");
                    return;
                }

                // Auto-login new user and jump straight into the main app
                UserSession.getInstance().login(newUsername, newUserDir);
                loadMainMusicPlayer();

            } catch (IOException e) {
                e.printStackTrace();
                showError("Error creating user: " + e.getMessage());
            }
        }
    }

    /**
     * Validates new user input for the account-creation dialog.
     *
     * @param username desired username
     * @param password desired password
     * @return an error message string if invalid, or {@code null} if input is valid
     */
    private String validateNewUserInput(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            return "Username and password cannot be empty.";
        }

        // Basic username format rule
        if (!username.matches("[A-Za-z0-9_]+")) {
            return "Username can only contain letters, numbers, and underscores.";
        }

        // Ensure username is not already used
        File usersFolder = new File("users");
        File userFolder = new File(usersFolder, username);
        if (userFolder.exists()) {
            return "That username is already taken. Please choose another.";
        }

        // Very simple password strength rule
        if (password.length() < 4) {
            return "Password must be at least 4 characters long.";
        }

        return null;
    }

    /**
     * Creates a new user directory structure on disk and writes the login file.
     * Also seeds the account with a "Sample Playlist" and a few sample songs.
     *
     * Directory layout created:
     * <pre>
     * users/
     *   username/
     *     user_login.txt
     *     playlists/
     *       Sample Playlist/
     *         someSong.mp3.songlink
     *     library/
     *       someSong.mp3
     * </pre>
     *
     * @param username new username
     * @param password new password
     * @return the new user's folder, or {@code null} if creation failed
     */
    private File createNewUserOnDisk(String username, String password) throws IOException {

        File usersFolder = new File("users");
        if (!usersFolder.exists() && !usersFolder.mkdirs()) {
            System.err.println("ERROR: Could not create users folder at " + usersFolder.getAbsolutePath());
            return null;
        }

        // === USER FOLDER ===
        File userFolder = new File(usersFolder, username);
        if (userFolder.exists()) {
            System.err.println("User folder already exists: " + userFolder.getAbsolutePath());
            return null;
        }
        if (!userFolder.mkdirs()) {
            System.err.println("ERROR: Could not create user folder: " + userFolder.getAbsolutePath());
            return null;
        }

        // === PLAYLISTS + LIBRARY FOLDERS ===
        File playlistsFolder = new File(userFolder, "playlists");
        File libraryFolder   = new File(userFolder, "library");
        if (!playlistsFolder.exists() && !playlistsFolder.mkdirs()) {
            System.err.println("ERROR: Could not create playlists folder for " + username);
            return null;
        }
        if (!libraryFolder.exists() && !libraryFolder.mkdirs()) {
            System.err.println("ERROR: Could not create library folder for " + username);
            return null;
        }

        // === WRITE LOGIN FILE ===
        File loginFile = new File(userFolder, "user_login.txt");
        try (var writer = new java.io.BufferedWriter(new java.io.FileWriter(loginFile))) {
            writer.write(username);
            writer.newLine();
            writer.write(password);
            writer.newLine();
        }

        // === CREATE "Sample Playlist" FOLDER ===
        File samplePlaylistFolder = new File(playlistsFolder, "Sample Playlist");
        if (!samplePlaylistFolder.exists() && !samplePlaylistFolder.mkdirs()) {
            System.err.println("ERROR: Could not create Sample Playlist folder for " + username);
            // Not fatal: login will still work, just no sample playlist
        }

        // === SAMPLE SONGS CONFIG ===
        String basePath = "/com/example/hummingbird/sample_songs/";

        String[] sampleFiles = {
                "Dark Sanctuary.mp3",
                "Robin Thicke - Blurred Lines ft. T.I., Pharrell.mp3",
                "RAYE-WHERE-IS-MY-HUSBAND.mp3"
        };

        // Copy each sample audio file into the user's library and create
        // a corresponding .songlink file in the Sample Playlist folder.
        for (String fileName : sampleFiles) {
            String resourcePath = basePath + fileName;

            File libraryTarget = new File(libraryFolder, fileName);
            try {
                copyResourceToFile(resourcePath, libraryTarget);
            } catch (IOException e) {
                System.err.println("WARNING: Failed to copy sample song " + fileName +
                        " to library for user " + username + ": " + e.getMessage());
                continue;
            }

            if (samplePlaylistFolder.exists()) {
                File linkFile = new File(samplePlaylistFolder, fileName + ".songlink");
                try (var fw = new java.io.FileWriter(linkFile)) {
                    // PlaylistManager will interpret this as a reference to a library file
                    fw.write(fileName);
                } catch (IOException e) {
                    System.err.println("WARNING: Failed to create .songlink for " + fileName +
                            " in " + samplePlaylistFolder.getAbsolutePath());
                }
            }
        }

        System.out.println("Created new user with sample playlist at: " + userFolder.getAbsolutePath());
        return userFolder;
    }

    /**
     * Copies a classpath resource (inside the JAR/resources) to a file on disk.
     *
     * @param resourcePath path inside the resources folder
     * @param targetFile   destination file on the filesystem
     */
    private void copyResourceToFile(String resourcePath, File targetFile) throws IOException {
        try (var in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            java.nio.file.Files.copy(in, targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ===================== MAIN PLAYER LOADING =====================

    /**
     * Loads the main audio player view and replaces the login scene.
     * Uses the currently logged-in username from UserSession to set
     * the window title.
     */
    private void loadMainMusicPlayer() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AudioPlayerApplication.class.getResource("/com/example/hummingbird/player_view.fxml")
            );

            Stage window = (Stage) usernameField.getScene().getWindow();
            Scene mainPlayerScene = new Scene(loader.load(), 900, 800);
            window.setScene(mainPlayerScene);

            String username = UserSession.getInstance().getUsername();
            window.setTitle("Hummingbird - " + username);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading main player");
        }
    }
}
