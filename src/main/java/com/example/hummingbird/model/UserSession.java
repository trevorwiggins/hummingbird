package com.example.hummingbird.model;
import java.io.File;

public class UserSession {
    private static UserSession instance;

    private String username;
    private File userDirectory;

    private UserSession() {}

    public static UserSession getInstance() { // user session
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void login(String username, File userDirectory){ // log in
        this.username = username;
        this.userDirectory = userDirectory;

        System.out.println("Logged in as " + username);
    }

    public void logout(){ // log out
        this.username = null;
        this.userDirectory = null;

        System.out.println("Logged out");
    }

    public boolean isLoggedIn(){ // log in if there is username and directory
        boolean hasUsername = (username != null);
        boolean hasUserDirectory = (userDirectory != null);
        return hasUsername && hasUserDirectory;
    }

    public String getUsername(){
        return username;
    }

    public File getUserDirectory(){ // get file with user data
        return userDirectory;
    }

    public File getPlaylistDirectory() {
        // make sure there is a user directory
        if (userDirectory == null) return null;

        return new File(userDirectory, "playlists"); //creates a file that points to the playlists folder inside the user's folder
    }



}
