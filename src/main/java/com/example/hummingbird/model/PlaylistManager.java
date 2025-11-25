package com.example.hummingbird.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Manages all playlists for the application.
 * Each playlist is a mapping from a playlist name to a list of songs.
 * Also maintains a MediaLibrary of all songs.
 *
 * NEW MODEL:
 * - All real MP3 files live in a shared "library" directory.
 * - Each playlist folder contains only `.songlink` files that point to
 *   a filename in the library directory.
 * - All playlists share the same canonical Song instances from MediaLibrary.
 */
public class PlaylistManager {

    // === STORE PLAYLISTS IN MEMORY ===
    private final Map<String, List<Song>> playlists;

    // Root directory where playlist subfolders live
    private final File rootDirectory;

    // Directory where actual MP3 files live (shared library)
    private final File libraryDirectory;

    // Global media library of canonical Song instances
    private final MediaLibrary mediaLibrary;

    /**
     * Initializes the PlaylistManager by scanning a directory for playlists.
     * Each subdirectory in 'directory' is treated as a playlist,
     * and each `.songlink` file inside a subdirectory refers to an MP3 file in the library.
     *
     * @param directory root directory containing playlist subdirectories
     *                  (e.g., users/test_user1/playlists)
     */
    public PlaylistManager(File directory) {
        this.playlists = new HashMap<>();
        this.mediaLibrary = new MediaLibrary();
        this.rootDirectory = directory;

        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            System.out.println("Invalid playlist directory: " + directory);
            // Library directory still gets initialized based on parent
            this.libraryDirectory = (directory != null && directory.getParentFile() != null)
                    ? new File(directory.getParentFile(), "library")
                    : null;
            return;
        }

        // Library folder is sibling of "playlists" (e.g., users/test_user1/library)
        File parent = directory.getParentFile();
        if (parent == null) {
            // Fallback: library in same dir as playlists if no parent
            this.libraryDirectory = new File(directory, "library");
        } else {
            this.libraryDirectory = new File(parent, "library");
        }

        if (!libraryDirectory.exists() && !libraryDirectory.mkdirs()) {
            System.out.println("Warning: Failed to create library directory: " + libraryDirectory.getAbsolutePath());
        }

        // Load existing playlists from disk
        File[] playlistFolders = directory.listFiles(File::isDirectory);
        if (playlistFolders == null) return;

        for (File folder : playlistFolders) {
            String playlistName = folder.getName();
            List<Song> songs = new ArrayList<>();

            // We expect .songlink files that contain the base filename of the MP3 in library
            File[] linkFiles = folder.listFiles(
                    f -> f.isFile() && f.getName().toLowerCase().endsWith(".songlink")
            );

            if (linkFiles != null) {
                for (File linkFile : linkFiles) {
                    try {
                        // Link file content is just the MP3 filename in the library directory
                        String targetFileName = Files.readString(linkFile.toPath()).trim();
                        if (targetFileName.isEmpty()) {
                            System.out.println("Warning: Empty link in " + linkFile.getAbsolutePath());
                            continue;
                        }

                        File songFile = new File(libraryDirectory, targetFileName);
                        if (!songFile.exists()) {
                            System.out.println("Warning: Library file not found for link: " + linkFile.getName());
                            continue;
                        }

                        // Find or create canonical Song in MediaLibrary
                        Song canonical = mediaLibrary.getSongByFile(songFile);
                        if (canonical == null) {
                            canonical = new Song(songFile);
                            mediaLibrary.addSong(canonical);
                        }

                        // Avoid duplicates in the same playlist
                        if (!songs.contains(canonical)) {
                            songs.add(canonical);
                        }

                    } catch (IOException e) {
                        System.out.println("Warning: Failed to read link file: " + linkFile.getAbsolutePath());
                    }
                }
            }

            playlists.put(playlistName, songs);
        }
    }

    // ============================================
    // PLAYLIST CREATION / DELETION
    // ============================================

    /**
     * Adds a new playlist with the given name and songs.
     * Songs are canonicalized into the MediaLibrary and stored in memory,
     * and `.songlink` pointer files are created in the playlist folder.
     *
     * @param name  name of the new playlist
     * @param songs list of songs to include (may be empty)
     */
    public void addPlaylist(String name, List<Song> songs) {
        if (name == null || name.isBlank()) return;
        if (rootDirectory == null || libraryDirectory == null) return;

        // Build canonical, de-duplicated list of songs for this playlist
        List<Song> canonicalSongs = new ArrayList<>();
        if (songs != null) {
            for (Song s : songs) {
                if (s == null) continue;

                // Ensure the underlying file is in the library
                Song canonical = ensureSongInLibraryAndMediaLibrary(s);

                if (!canonicalSongs.contains(canonical)) {
                    canonicalSongs.add(canonical);
                }
            }
        }

        // Store playlist in memory
        playlists.put(name, canonicalSongs);

        // Ensure playlist folder exists
        File folder = new File(rootDirectory, name);
        if (!folder.exists() && !folder.mkdirs()) {
            System.out.println("Warning: Failed to create playlist folder: " + folder.getAbsolutePath());
        }

        // Create `.songlink` files for each song in the playlist
        for (Song s : canonicalSongs) {
            createLinkFileForSong(folder, s);
        }
    }

    /**
     * Deletes a playlist with the given name.
     * Removes it from memory and deletes the corresponding folder on disk.
     * Does NOT remove songs from the MediaLibrary or delete actual MP3 files.
     *
     * @param name name of the playlist to delete
     */
    public void deletePlaylist(String name) {
        if (name == null) return;

        playlists.remove(name);

        if (rootDirectory == null) return;
        File folder = new File(rootDirectory, name);
        if (folder.exists() && folder.isDirectory()) {
            if (!deleteDirectoryRecursive(folder)) {
                System.out.println("Warning: Failed to delete playlist folder: " + folder.getAbsolutePath());
            }
        }
    }

    private boolean deleteDirectoryRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    if (!deleteDirectoryRecursive(f)) return false;
                } else {
                    if (!f.delete()) return false;
                }
            }
        }
        return dir.delete();
    }

    // ============================================
    // SONG MANAGEMENT
    // ============================================

    /**
     * Adds a song to the specified playlist and also to the MediaLibrary if not already present.
     * Ensures the underlying file is in the shared library directory.
     * Creates/updates a `.songlink` file in the playlist folder.
     *
     * @param song         Song to add
     * @param playlistName Name of the playlist
     */
    public void addSongToPlaylist(Song song, String playlistName) {
        if (song == null || playlistName == null) return;
        if (rootDirectory == null || libraryDirectory == null) return;

        // Ensure the song's file resides in the library and is canonical in MediaLibrary
        Song canonical = ensureSongInLibraryAndMediaLibrary(song);

        // Add to in-memory playlist (creating if needed)
        List<Song> list = playlists.computeIfAbsent(playlistName, k -> new ArrayList<>());
        if (!list.contains(canonical)) {
            list.add(canonical);
        }

        // Ensure playlist folder exists
        File folder = new File(rootDirectory, playlistName);
        if (!folder.exists() && !folder.mkdirs()) {
            System.out.println("Warning: Failed to create playlist folder: " + folder.getAbsolutePath());
        }

        // Create `.songlink` file pointing to the library file
        createLinkFileForSong(folder, canonical);
    }

    /**
     * Removes a song from the specified playlist in memory and removes the corresponding
     * `.songlink` file from the playlist folder. Does NOT remove it from the library.
     *
     * @param song         Song to remove
     * @param playlistName Name of the playlist
     */
    public void removeSongFromPlaylist(Song song, String playlistName) {
        if (song == null || playlistName == null) return;
        if (rootDirectory == null) return;

        List<Song> list = playlists.get(playlistName);
        if (list != null) {
            list.remove(song);
        }

        // Delete corresponding `.songlink` from disk
        File folder = new File(rootDirectory, playlistName);
        if (!folder.exists() || !folder.isDirectory()) return;

        File[] linkFiles = folder.listFiles(
                f -> f.isFile() && f.getName().toLowerCase().endsWith(".songlink")
        );
        if (linkFiles == null) return;

        File songFile = song.getSongFile();
        if (songFile == null) return;
        String targetName = songFile.getName();

        for (File link : linkFiles) {
            try {
                String content = Files.readString(link.toPath()).trim();
                if (targetName.equals(content)) {
                    if (!link.delete()) {
                        System.out.println("Warning: Failed to delete link file: " + link.getAbsolutePath());
                    }
                }
            } catch (IOException e) {
                System.out.println("Warning: Failed to read link file during removal: " + link.getAbsolutePath());
            }
        }
    }

    /**
     * Removes a song from all playlists AND the MediaLibrary, and deletes the actual file
     * from the shared library directory. Also removes all `.songlink` files that point to it.
     *
     * @param song Song to remove everywhere
     */
    public void removeSongEverywhere(Song song) {
        if (song == null) return;
        if (rootDirectory == null || libraryDirectory == null) return;

        File songFile = song.getSongFile();
        if (songFile == null) return;

        String targetName = songFile.getName();

        // 1) Remove from all in-memory playlists
        for (Map.Entry<String, List<Song>> entry : playlists.entrySet()) {
            List<Song> songs = entry.getValue();
            songs.remove(song);
        }

        // 2) Remove all `.songlink` files in all playlists that point to this song
        File[] playlistFolders = rootDirectory.listFiles(File::isDirectory);
        if (playlistFolders != null) {
            for (File folder : playlistFolders) {
                File[] linkFiles = folder.listFiles(
                        f -> f.isFile() && f.getName().toLowerCase().endsWith(".songlink")
                );
                if (linkFiles == null) continue;

                for (File link : linkFiles) {
                    try {
                        String content = Files.readString(link.toPath()).trim();
                        if (targetName.equals(content)) {
                            if (!link.delete()) {
                                System.out.println("Warning: Failed to delete link: " + link.getAbsolutePath());
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Warning: Failed to read link file: " + link.getAbsolutePath());
                    }
                }
            }
        }

        // 3) Remove from MediaLibrary
        boolean existed = mediaLibrary.removeSongFromLibrary(song);

        // 4) Delete the actual file from the library directory
        if (existed && songFile.exists()) {
            if (!songFile.delete()) {
                System.out.println("Warning: Failed to delete library file: " + songFile.getAbsolutePath());
            }
        }
    }

    // ============================================
    // HELPERS
    // ============================================

    /**
     * Ensures the given Song's underlying file is stored in the shared library directory
     * and that a canonical Song pointing to that file exists in MediaLibrary.
     *
     * @param song incoming Song (may point to a non-library location)
     * @return canonical Song instance from MediaLibrary pointing to a file in the library
     */
    private Song ensureSongInLibraryAndMediaLibrary(Song song) {
        if (song == null || libraryDirectory == null) return song;

        File sourceFile = song.getSongFile();
        if (sourceFile == null || !sourceFile.exists()) {
            return song;
        }

        // Desired location inside the library
        File libraryFile = new File(libraryDirectory, sourceFile.getName());

        // If not already in the library, copy it there
        if (!libraryFile.equals(sourceFile)) {
            if (!libraryFile.exists()) {
                try {
                    Files.copy(sourceFile.toPath(), libraryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.out.println("Warning: Failed to copy song into library: " + sourceFile.getAbsolutePath());
                }
            }
        }

        // Now ensure MediaLibrary has a canonical Song for the library file
        Song canonical = mediaLibrary.getSongByFile(libraryFile);
        if (canonical == null) {
            canonical = new Song(libraryFile);
            mediaLibrary.addSong(canonical);
        }

        return canonical;
    }

    /**
     * Creates or overwrites a `.songlink` pointer file for the given song inside a playlist folder.
     * The link file contains just the MP3 filename as stored in the library directory.
     */
    private void createLinkFileForSong(File playlistFolder, Song song) {
        if (playlistFolder == null || song == null) return;

        File songFile = song.getSongFile();
        if (songFile == null) return;

        String baseName = songFile.getName();
        File linkFile = new File(playlistFolder, baseName + ".songlink");

        try (FileWriter fw = new FileWriter(linkFile)) {
            fw.write(baseName);
        } catch (IOException e) {
            System.out.println("Warning: Failed to create link file: " + linkFile.getAbsolutePath());
        }
    }

    // ============================================
    // GETTERS / UTILITIES
    // ============================================

    public Song getSongFromPlaylist(String playlistName, int index) {
        List<Song> list = playlists.get(playlistName);
        if (list == null || index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    public List<Song> getPlaylist(String playlistName) {
        // Return a *copy* to avoid accidental external mutation of internal list
        List<Song> list = playlists.get(playlistName);
        return (list == null) ? new ArrayList<>() : new ArrayList<>(list);
    }

    public String[] getAllPlaylistNames() {
        if (playlists.isEmpty()) return new String[0];
        return playlists.keySet().toArray(new String[0]);
    }

    public MediaLibrary getMediaLibrary() {
        return mediaLibrary;
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public File getLibraryDirectory() {
        return libraryDirectory;
    }

    /**
     * NEW: Check if a playlist exists in memory.
     */
    public boolean playlistExists(String name) {
        if (name == null) return false;
        return playlists.containsKey(name);
    }

    /**
     * NEW: Rewrite all `.songlink` files on disk to match the current
     * in-memory playlists.
     *
     * Useful after bulk operations (deletions, migrations) to ensure
     * disk and memory stay in sync.
     */
    public void savePlaylistsToDisk() {
        if (rootDirectory == null) return;

        // For each playlist in memory, recreate its folder's .songlink files
        for (Map.Entry<String, List<Song>> entry : playlists.entrySet()) {
            String playlistName = entry.getKey();
            List<Song> songs = entry.getValue();

            File folder = new File(rootDirectory, playlistName);
            if (!folder.exists() && !folder.mkdirs()) {
                System.out.println("Warning: Failed to create playlist folder during save: " + folder.getAbsolutePath());
                continue;
            }

            // Delete all existing .songlink files
            File[] existingLinks = folder.listFiles(
                    f -> f.isFile() && f.getName().toLowerCase().endsWith(".songlink")
            );
            if (existingLinks != null) {
                for (File link : existingLinks) {
                    if (!link.delete()) {
                        System.out.println("Warning: Failed to delete old link file: " + link.getAbsolutePath());
                    }
                }
            }

            // Recreate links based on current in-memory songs
            if (songs != null) {
                for (Song s : songs) {
                    createLinkFileForSong(folder, s);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (playlists.isEmpty()) {
            sb.append("No playlists available.\n");
            return sb.toString();
        }
        for (Map.Entry<String, List<Song>> entry : playlists.entrySet()) {
            sb.append("Playlist: ").append(entry.getKey()).append("\n");
            List<Song> songs = entry.getValue();
            if (songs.isEmpty()) sb.append("  (No songs in this playlist)\n");
            else {
                for (int i = 0; i < songs.size(); i++) {
                    sb.append("  ").append(i + 1).append(". ").append(songs.get(i)).append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
