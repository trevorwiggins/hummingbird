package com.example.hummingbird.model;

import java.io.File;

/**
 * Singleton class that stores information about the currently logged-in user.
 *
 * This object acts as a global session reference shared across the entire
 * application. Controllers can retrieve it using {@link #getInstance()}
 * to read or update the active user's information.
 *
 * Stored data:
 *  • username — the name the user logged in with
 *  • userDirectory — the root folder for that user's data
 *
 * When no user is logged in, both fields are null.
 */
public class UserSession {

    /** Single shared instance (classic Singleton pattern). */
    private static UserSession instance;

    /** Username of the currently logged-in user, or null if none. */
    private String username;

    /** Root directory for the logged-in user (e.g. users/<username>/). */
    private File userDirectory;

    /** Private constructor to prevent external instantiation. */
    private UserSession() { }

    /**
     * Returns the single global UserSession instance.
     * If it doesn’t exist yet, it is created.
     */
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Marks a user as "logged in" and stores their information.
     *
     * @param username      the username entered at login
     * @param userDirectory the folder containing this user's data
     */
    public void login(String username, File userDirectory) {
        this.username = username;
        this.userDirectory = userDirectory;

        System.out.println("Logged in as " + username);
    }

    /**
     * Clears the current session, effectively logging the user out.
     */
    public void logout() {
        this.username = null;
        this.userDirectory = null;

        System.out.println("Logged out");
    }

    /**
     * @return true if a user is currently logged in (both username and directory set)
     */
    public boolean isLoggedIn() {
        return username != null && userDirectory != null;
    }

    /**
     * @return the username of the current user, or null if none
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the root directory for this user's data (e.g. playlists + library),
     *         or null if no user is logged in
     */
    public File getUserDirectory() {
        return userDirectory;
    }

    /**
     * Convenience helper — returns the "playlists" folder for this user.
     *
     * @return File pointing to:  userDirectory/playlists
     *         or null if no user is logged in
     */
    public File getPlaylistDirectory() {
        if (userDirectory == null) return null;
        return new File(userDirectory, "playlists");
    }
}