package com.example.hummingbird.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Central manager for all playlists owned by a single user.
 *
 * Responsibilities:
 * <ul>
 *     <li>Load playlists from disk into memory.</li>
 *     <li>Keep an in-memory mapping of playlist name → list of canonical {@link Song} objects.</li>
 *     <li>Maintain a shared {@link MediaLibrary} of all songs in the user's library.</li>
 *     <li>Keep playlist `.songlink` files in sync with in-memory state.</li>
 *     <li>Handle deletion of songs everywhere (from playlists, library, and disk).</li>
 * </ul>
 *
 * Storage model:
 * <ul>
 *     <li>All actual <code>.mp3</code> files live in a shared <b>library</b> directory.</li>
 *     <li>Each playlist has its own directory under the <b>playlists</b> root.</li>
 *     <li>Playlist directories only contain <code>.songlink</code> files, each storing
 *         the filename of a library MP3.</li>
 *     <li>Every playlist references the same canonical {@link Song} instances
 *         from the shared {@link MediaLibrary}.</li>
 * </ul>
 */
public class PlaylistManager {

    // ===================== IN-MEMORY STATE =====================

    /** Mapping from playlist name → ordered list of songs in that playlist. */
    private final Map<String, List<Song>> playlists;

    /** Root directory where playlist subfolders live (e.g., users/username/playlists). */
    private final File rootDirectory;

    /** Directory where the actual .mp3 files are stored for this user. */
    private final File libraryDirectory;

    /** Shared media library that holds the canonical Song instances. */
    private final MediaLibrary mediaLibrary;

    // ===================== CONSTRUCTION & LOADING =====================

    /**
     * Creates a PlaylistManager, loads all playlists from the given directory,
     * and builds an in-memory {@link MediaLibrary} of canonical songs.
     * <p>
     * Expected layout:
     * <pre>
     * users/
     *   username/
     *     playlists/
     *       Some Playlist/
     *         A_Song.mp3.songlink    (contains: "A_Song.mp3")
     *     library/
     *       A_Song.mp3
     * </pre>
     *
     * @param directory root directory containing playlist subfolders
     *                  (e.g., <code>users/test_user1/playlists</code>)
     */
    public PlaylistManager(File directory) {
        this.playlists = new HashMap<>();
        this.mediaLibrary = new MediaLibrary();
        this.rootDirectory = directory;

        // If playlists root is invalid, still try to infer library directory and bail early
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            System.out.println("Invalid playlist directory: " + directory);
            this.libraryDirectory = (directory != null && directory.getParentFile() != null)
                    ? new File(directory.getParentFile(), "library")
                    : null;
            return;
        }

        // Library folder is sibling of "playlists" (e.g., users/test_user1/library)
        File parent = directory.getParentFile();
        if (parent == null) {
            // Fallback: library folder inside the same directory
            this.libraryDirectory = new File(directory, "library");
        } else {
            this.libraryDirectory = new File(parent, "library");
        }

        if (!libraryDirectory.exists() && !libraryDirectory.mkdirs()) {
            System.out.println("Warning: Failed to create library directory: " + libraryDirectory.getAbsolutePath());
        }

        // Scan each playlist folder and reconstruct playlists in memory
        File[] playlistFolders = directory.listFiles(File::isDirectory);
        if (playlistFolders == null) return;

        for (File folder : playlistFolders) {
            String playlistName = folder.getName();
            List<Song> songs = new ArrayList<>();

            // Each .songlink file inside this folder maps to a library MP3 filename
            File[] linkFiles = folder.listFiles(
                    f -> f.isFile() && f.getName().toLowerCase().endsWith(".songlink")
            );

            if (linkFiles != null) {
                for (File linkFile : linkFiles) {
                    try {
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

                        // Get or create canonical Song
                        Song canonical = mediaLibrary.getSongByFile(songFile);
                        if (canonical == null) {
                            canonical = new Song(songFile);
                            mediaLibrary.addSong(canonical);
                        }

                        // Avoid duplicates in a single playlist
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

    // ===================== PLAYLIST CREATION / DELETION =====================

    /**
     * Creates or overwrites a playlist with the given name and songs.
     * <ul>
     *     <li>Each incoming Song is moved/ensured in the shared library folder.</li>
     *     <li>Canonical Song instances are stored in memory.</li>
     *     <li>A folder for the playlist is created (if missing).</li>
     *     <li><code>.songlink</code> files are created for each song.</li>
     * </ul>
     *
     * @param name  playlist name (case-sensitive)
     * @param songs initial songs to include (may be null or empty)
     */
    public void addPlaylist(String name, List<Song> songs) {
        if (name == null || name.isBlank()) return;
        if (rootDirectory == null || libraryDirectory == null) return;

        // Build canonical, deduplicated list for this playlist
        List<Song> canonicalSongs = new ArrayList<>();
        if (songs != null) {
            for (Song s : songs) {
                if (s == null) continue;

                Song canonical = ensureSongInLibraryAndMediaLibrary(s);

                if (!canonicalSongs.contains(canonical)) {
                    canonicalSongs.add(canonical);
                }
            }
        }

        // Store in-memory representation
        playlists.put(name, canonicalSongs);

        // Ensure on-disk playlist folder exists
        File folder = new File(rootDirectory, name);
        if (!folder.exists() && !folder.mkdirs()) {
            System.out.println("Warning: Failed to create playlist folder: " + folder.getAbsolutePath());
        }

        // Create one .songlink per song
        for (Song s : canonicalSongs) {
            createLinkFileForSong(folder, s);
        }
    }

    /**
     * Deletes a playlist entirely:
     * <ul>
     *     <li>Removes it from the in-memory map.</li>
     *     <li>Deletes its folder and all contents (all <code>.songlink</code> files).</li>
     * </ul>
     * This does not delete songs from the MediaLibrary or from the library directory.
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

    /**
     * Recursively deletes a directory and all of its children.
     *
     * @param dir directory to delete
     * @return true if everything was removed successfully
     */
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

    // ===================== SONG MANAGEMENT =====================

    /**
     * Adds a song to a specific playlist (creating the playlist on demand)
     * and ensures its file is stored in the shared library.
     * <ul>
     *     <li>The song's file is copied into the library directory if needed.</li>
     *     <li>A canonical Song is placed in the {@link MediaLibrary}.</li>
     *     <li>The canonical Song is added to the playlist's list (if not already present).</li>
     *     <li>A <code>.songlink</code> file is created in the playlist folder.</li>
     * </ul>
     *
     * @param song         song to add
     * @param playlistName target playlist name
     */
    public void addSongToPlaylist(Song song, String playlistName) {
        if (song == null || playlistName == null) return;
        if (rootDirectory == null || libraryDirectory == null) return;

        Song canonical = ensureSongInLibraryAndMediaLibrary(song);

        // Get or create in-memory playlist
        List<Song> list = playlists.computeIfAbsent(playlistName, k -> new ArrayList<>());
        if (!list.contains(canonical)) {
            list.add(canonical);
        }

        // Ensure on-disk playlist folder exists
        File folder = new File(rootDirectory, playlistName);
        if (!folder.exists() && !folder.mkdirs()) {
            System.out.println("Warning: Failed to create playlist folder: " + folder.getAbsolutePath());
        }

        // Create .songlink entry pointing to the library file
        createLinkFileForSong(folder, canonical);
    }

    /**
     * Removes a song from a specific playlist, both in memory and on disk:
     * <ul>
     *     <li>Removes the {@link Song} from the playlist's list.</li>
     *     <li>Deletes all <code>.songlink</code> files in that playlist folder
     *         that reference this song's filename.</li>
     * </ul>
     * The song stays in the MediaLibrary and in other playlists.
     *
     * @param song         song to remove
     * @param playlistName playlist name
     */
    public void removeSongFromPlaylist(Song song, String playlistName) {
        if (song == null || playlistName == null) return;
        if (rootDirectory == null) return;

        List<Song> list = playlists.get(playlistName);
        if (list != null) {
            list.remove(song);
        }

        // Remove matching .songlink files from disk
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
     * Completely removes a song from the system:
     * <ol>
     *     <li>Remove it from every in-memory playlist.</li>
     *     <li>Delete all <code>.songlink</code> files in all playlists that reference it.</li>
     *     <li>Remove it from the {@link MediaLibrary}.</li>
     *     <li>Delete the underlying .mp3 file from the library directory.</li>
     * </ol>
     *
     * @param song song to remove everywhere
     */
    public void removeSongEverywhere(Song song) {
        if (song == null) return;
        if (rootDirectory == null || libraryDirectory == null) return;

        File songFile = song.getSongFile();
        if (songFile == null) return;

        String targetName = songFile.getName();

        // 1) Remove from every in-memory playlist
        for (Map.Entry<String, List<Song>> entry : playlists.entrySet()) {
            List<Song> songs = entry.getValue();
            songs.remove(song);
        }

        // 2) Remove all .songlink entries pointing to this song
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

        // 4) Delete the actual audio file
        if (existed && songFile.exists()) {
            if (!songFile.delete()) {
                System.out.println("Warning: Failed to delete library file: " + songFile.getAbsolutePath());
            }
        }
    }

    // ===================== INTERNAL HELPERS =====================

    /**
     * Ensures that:
     * <ul>
     *     <li>The given Song's audio file is stored inside the shared library directory.</li>
     *     <li>A canonical {@link Song} pointing to the library file exists in the {@link MediaLibrary}.</li>
     * </ul>
     * If the Song is already in the library and registered, this simply returns the existing instance.
     *
     * @param song incoming Song that may point to an arbitrary location
     * @return canonical Song that points to a file inside the library directory
     */
    private Song ensureSongInLibraryAndMediaLibrary(Song song) {
        if (song == null || libraryDirectory == null) return song;

        File sourceFile = song.getSongFile();
        if (sourceFile == null || !sourceFile.exists()) {
            return song;
        }

        // Destination file inside the library directory
        File libraryFile = new File(libraryDirectory, sourceFile.getName());

        // Copy into library if it isn't already there
        if (!libraryFile.equals(sourceFile)) {
            if (!libraryFile.exists()) {
                try {
                    Files.copy(sourceFile.toPath(), libraryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.out.println("Warning: Failed to copy song into library: " + sourceFile.getAbsolutePath());
                }
            }
        }

        // Look up or register canonical Song tied to the library file
        Song canonical = mediaLibrary.getSongByFile(libraryFile);
        if (canonical == null) {
            canonical = new Song(libraryFile);
            mediaLibrary.addSong(canonical);
        }

        return canonical;
    }

    /**
     * Creates or overwrites a <code>.songlink</code> file for the given song
     * inside the specified playlist folder.
     * <p>
     * The file content is just the MP3 filename, which is interpreted relative
     * to the library directory.
     *
     * @param playlistFolder playlist directory
     * @param song           song to create a link for
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

    // ===================== PUBLIC GETTERS & UTILITIES =====================

    /**
     * Returns the Song at a given index within a named playlist.
     *
     * @param playlistName name of playlist to look in
     * @param index        zero-based index
     * @return the Song at that position, or {@code null} if out of range
     */
    public Song getSongFromPlaylist(String playlistName, int index) {
        List<Song> list = playlists.get(playlistName);
        if (list == null || index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    /**
     * Returns a copy of the song list for a given playlist.
     * Modifications to the returned list do not affect internal state.
     *
     * @param playlistName name of playlist
     * @return new list of songs, or an empty list if playlist does not exist
     */
    public List<Song> getPlaylist(String playlistName) {
        List<Song> list = playlists.get(playlistName);
        return (list == null) ? new ArrayList<>() : new ArrayList<>(list);
    }

    /**
     * @return an array of all playlist names currently known to the manager.
     */
    public String[] getAllPlaylistNames() {
        if (playlists.isEmpty()) return new String[0];
        return playlists.keySet().toArray(new String[0]);
    }

    /** @return the shared {@link MediaLibrary} used by all playlists. */
    public MediaLibrary getMediaLibrary() {
        return mediaLibrary;
    }

    /** @return the root playlists directory on disk. */
    public File getRootDirectory() {
        return rootDirectory;
    }

    /** @return the shared library directory where MP3 files are stored. */
    public File getLibraryDirectory() {
        return libraryDirectory;
    }

    /**
     * Checks whether a playlist with the given name exists in memory.
     *
     * @param name playlist name
     * @return true if the playlist is known to this manager
     */
    public boolean playlistExists(String name) {
        if (name == null) return false;
        return playlists.containsKey(name);
    }

    /**
     * Rewrites all playlist folders on disk so that their <code>.songlink</code> files
     * exactly match the current in-memory playlist structure.
     * <p>
     * This is useful after bulk edits (imports, deletions) to ensure disk state
     * and in-memory state are synced.
     */
    public void savePlaylistsToDisk() {
        if (rootDirectory == null) return;

        for (Map.Entry<String, List<Song>> entry : playlists.entrySet()) {
            String playlistName = entry.getKey();
            List<Song> songs = entry.getValue();

            File folder = new File(rootDirectory, playlistName);
            if (!folder.exists() && !folder.mkdirs()) {
                System.out.println("Warning: Failed to create playlist folder during save: " + folder.getAbsolutePath());
                continue;
            }

            // Remove any existing .songlink files
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

            // Recreate .songlink files from current in-memory list
            if (songs != null) {
                for (Song s : songs) {
                    createLinkFileForSong(folder, s);
                }
            }
        }
    }

    /**
     * Returns a human-readable summary of all playlists and their songs.
     */
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
            if (songs.isEmpty()) {
                sb.append("  (No songs in this playlist)\n");
            } else {
                for (int i = 0; i < songs.size(); i++) {
                    sb.append("  ").append(i + 1).append(". ").append(songs.get(i)).append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
