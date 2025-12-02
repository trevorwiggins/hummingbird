package com.example.hummingbird.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * central manager for all playlists owned by a single user.
 *
 * responsibilities:
 * load playlists from disk into memory.
 * keep an in-memory mapping of playlist name → list of canonical song objects.
 * maintain a shared medialibrary of all songs in the user's library.
 * keep playlist .songlink files in sync with in-memory state.
 * handle deletion of songs everywhere (from playlists, library, and disk).
 *
 * storage model:
 * all actual .mp3 files live in a shared library directory.
 * each playlist has its own directory under the playlists root.
 * playlist directories only contain .songlink files, each storing
 * the filename of a library mp3.
 * every playlist references the same canonical song instances
 * from the shared medialibrary.
 */
public class PlaylistManager {

    //===================== in-memory state =====================

    /** mapping from playlist name → ordered list of songs in that playlist. */
    private final Map<String, List<Song>> playlists;

    /** root directory where playlist subfolders live (e.g., users/username/playlists). */
    private final File rootDirectory;

    /** directory where the actual .mp3 files are stored for this user. */
    private final File libraryDirectory;

    /** shared media library that holds the canonical song instances. */
    private final MediaLibrary mediaLibrary;

    //===================== construction & loading =====================

    /**
     * creates a playlistmanager, loads all playlists from the given directory,
     * and builds an in-memory medialibrary of canonical songs.
     *
     * expected layout:
     * users/
     *   username/
     *     playlists/
     *       some playlist/
     *         a_song.mp3.songlink    (contains: "a_song.mp3")
     *     library/
     *       a_song.mp3
     *
     * @param directory root directory containing playlist subfolders
     *                  (e.g., users/test_user1/playlists)
     */
    public PlaylistManager(File directory) {
        this.playlists = new HashMap<>();
        this.mediaLibrary = new MediaLibrary();
        this.rootDirectory = directory;

        //if playlists root is invalid, still try to infer library directory and bail early
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            System.out.println("Invalid playlist directory: " + directory);
            this.libraryDirectory = (directory != null && directory.getParentFile() != null)
                    ? new File(directory.getParentFile(), "library")
                    : null;
            return;
        }

        //library folder is sibling of "playlists" (e.g., users/test_user1/library)
        File parent = directory.getParentFile();
        if (parent == null) {
            //fallback: library folder inside the same directory
            this.libraryDirectory = new File(directory, "library");
        } else {
            this.libraryDirectory = new File(parent, "library");
        }

        if (!libraryDirectory.exists() && !libraryDirectory.mkdirs()) {
            System.out.println("Warning: Failed to create library directory: " + libraryDirectory.getAbsolutePath());
        }

        //scan each playlist folder and reconstruct playlists in memory
        File[] playlistFolders = directory.listFiles(File::isDirectory);
        if (playlistFolders == null) return;

        for (File folder : playlistFolders) {
            String playlistName = folder.getName();
            List<Song> songs = new ArrayList<>();

            //each .songlink file inside this folder maps to a library mp3 filename
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

                        //get or create canonical song
                        Song canonical = mediaLibrary.getSongByFile(songFile);
                        if (canonical == null) {
                            canonical = new Song(songFile);
                            mediaLibrary.addSong(canonical);
                        }

                        //avoid duplicates in a single playlist
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

    //===================== playlist creation / deletion =====================

    /**
     * creates or overwrites a playlist with the given name and songs.
     * each incoming song is moved/ensured in the shared library folder.
     * canonical song instances are stored in memory.
     * a folder for the playlist is created (if missing).
     * .songlink files are created for each song.
     *
     * @param name  playlist name (case-sensitive)
     * @param songs initial songs to include (may be null or empty)
     */
    public void addPlaylist(String name, List<Song> songs) {
        if (name == null || name.isBlank()) return;
        if (rootDirectory == null || libraryDirectory == null) return;

        //build canonical, deduplicated list for this playlist
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

        //store in-memory representation
        playlists.put(name, canonicalSongs);

        //ensure on-disk playlist folder exists
        File folder = new File(rootDirectory, name);
        if (!folder.exists() && !folder.mkdirs()) {
            System.out.println("Warning: Failed to create playlist folder: " + folder.getAbsolutePath());
        }

        //create one .songlink per song
        for (Song s : canonicalSongs) {
            createLinkFileForSong(folder, s);
        }
    }

    /**
     * deletes a playlist entirely:
     * removes it from the in-memory map.
     * deletes its folder and all contents (all .songlink files).
     * this does not delete songs from the medialibrary or from the library directory.
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
     * recursively deletes a directory and all of its children.
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

    //===================== song management =====================

    /**
     * adds a song to a specific playlist (creating the playlist on demand)
     * and ensures its file is stored in the shared library.
     * the song's file is copied into the library directory if needed.
     * a canonical song is placed in the medialibrary.
     * the canonical song is added to the playlist's list (if not already present).
     * a .songlink file is created in the playlist folder.
     *
     * @param song         song to add
     * @param playlistName target playlist name
     */
    public void addSongToPlaylist(Song song, String playlistName) {
        if (song == null || playlistName == null) return;
        if (rootDirectory == null || libraryDirectory == null) return;

        Song canonical = ensureSongInLibraryAndMediaLibrary(song);

        //get or create in-memory playlist
        List<Song> list = playlists.computeIfAbsent(playlistName, k -> new ArrayList<>());
        if (!list.contains(canonical)) {
            list.add(canonical);
        }

        //ensure on-disk playlist folder exists
        File folder = new File(rootDirectory, playlistName);
        if (!folder.exists() && !folder.mkdirs()) {
            System.out.println("Warning: Failed to create playlist folder: " + folder.getAbsolutePath());
        }

        //create .songlink entry pointing to the library file
        createLinkFileForSong(folder, canonical);
    }

    /**
     * removes a song from a specific playlist, both in memory and on disk:
     * removes the song from the playlist's list.
     * deletes all .songlink files in that playlist folder
     * that reference this song's filename.
     * the song stays in the medialibrary and in other playlists.
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

        //remove matching .songlink files from disk
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
     * completely removes a song from the system:
     * remove it from every in-memory playlist.
     * delete all .songlink files in all playlists that reference it.
     * remove it from the medialibrary.
     * delete the underlying .mp3 file from the library directory.
     *
     * @param song song to remove everywhere
     */
    public void removeSongEverywhere(Song song) {
        if (song == null) return;
        if (rootDirectory == null || libraryDirectory == null) return;

        File songFile = song.getSongFile();
        if (songFile == null) return;

        String targetName = songFile.getName();

        //1) remove from every in-memory playlist
        for (Map.Entry<String, List<Song>> entry : playlists.entrySet()) {
            List<Song> songs = entry.getValue();
            songs.remove(song);
        }

        //2) remove all .songlink entries pointing to this song
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

        //3) remove from medialibrary
        boolean existed = mediaLibrary.removeSongFromLibrary(song);

        //4) delete the actual audio file
        if (existed && songFile.exists()) {
            if (!songFile.delete()) {
                System.out.println("Warning: Failed to delete library file: " + songFile.getAbsolutePath());
            }
        }
    }

    //===================== internal helpers =====================

    /**
     * ensures that:
     * the given song's audio file is stored inside the shared library directory.
     * a canonical song pointing to the library file exists in the medialibrary.
     * if the song is already in the library and registered, this simply returns the existing instance.
     *
     * @param song incoming song that may point to an arbitrary location
     * @return canonical song that points to a file inside the library directory
     */
    private Song ensureSongInLibraryAndMediaLibrary(Song song) {
        if (song == null || libraryDirectory == null) return song;

        File sourceFile = song.getSongFile();
        if (sourceFile == null || !sourceFile.exists()) {
            return song;
        }

        //destination file inside the library directory
        File libraryFile = new File(libraryDirectory, sourceFile.getName());

        //copy into library if it isn't already there
        if (!libraryFile.equals(sourceFile)) {
            if (!libraryFile.exists()) {
                try {
                    Files.copy(sourceFile.toPath(), libraryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.out.println("Warning: Failed to copy song into library: " + sourceFile.getAbsolutePath());
                }
            }
        }

        //look up or register canonical song tied to the library file
        Song canonical = mediaLibrary.getSongByFile(libraryFile);
        if (canonical == null) {
            canonical = new Song(libraryFile);
            mediaLibrary.addSong(canonical);
        }

        return canonical;
    }

    /**
     * creates or overwrites a .songlink file for the given song
     * inside the specified playlist folder.
     *
     * the file content is just the mp3 filename, which is interpreted relative
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

    //===================== public getters & utilities =====================

    /**
     * returns the song at a given index within a named playlist.
     *
     * @param playlistName name of playlist to look in
     * @param index        zero-based index
     * @return the song at that position, or null if out of range
     */
    public Song getSongFromPlaylist(String playlistName, int index) {
        List<Song> list = playlists.get(playlistName);
        if (list == null || index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    /**
     * returns a copy of the song list for a given playlist.
     * modifications to the returned list do not affect internal state.
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

    /** @return the shared medialibrary used by all playlists. */
    public MediaLibrary getMediaLibrary() {
        return mediaLibrary;
    }

    /** @return the root playlists directory on disk. */
    public File getRootDirectory() {
        return rootDirectory;
    }

    /** @return the shared library directory where mp3 files are stored. */
    public File getLibraryDirectory() {
        return libraryDirectory;
    }

    /**
     * checks whether a playlist with the given name exists in memory.
     *
     * @param name playlist name
     * @return true if the playlist is known to this manager
     */
    public boolean playlistExists(String name) {
        if (name == null) return false;
        return playlists.containsKey(name);
    }

    /**
     * rewrites all playlist folders on disk so that their .songlink files
     * exactly match the current in-memory playlist structure.
     *
     * this is useful after bulk edits (imports, deletions) to ensure disk state
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

            //remove any existing .songlink files
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

            //recreate .songlink files from current in-memory list
            if (songs != null) {
                for (Song s : songs) {
                    createLinkFileForSong(folder, s);
                }
            }
        }
    }

    /**
     * returns a human-readable summary of all playlists and their songs.
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
