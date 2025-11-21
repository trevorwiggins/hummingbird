# Hummingbird Audio Player
Hummingbird is a lightweight JavaFX audio player that supports playlists, queues, and metadata-aware song display. This README provides guidance on installing, organizing your music, and using the program effectively.

## Table of Contents

1. _Introduction_
    
2. _Installing Dependencies_

3. _Metadata Support_

4. _Recommended File Naming and Folder Structure_

5. _Using Hummingbird_

6. _Tips and Best Practices_

---
## Introduction

Hummingbird allows you to:

- Load playlists from a directory structure.

- Play, pause, skip, and seek songs.

- Maintain a reorderable song queue.

- Display song metadata such as title, artist, and duration.

- Import `.mp3` files directly from your file system into playlists.

---
## Installing Dependencies

Hummingbird uses **jaudiotagger** for reading metadata from audio files.

### Maven

Add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>org.jaudiotagger</groupId>
    <artifactId>jaudiotagger</artifactId>
    <version>2.0.1</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/jaudiotagger-2.0.1.jar</systemPath>
</dependency>
```


Make sure you download jaudiotagger-2.0.1.jar and place it in your project’s **libs/** folder.

---
## Metadata Support

Hummingbird reads song metadata using jaudiotagger:

- _Title_: The main song title displayed in the player.

- _Artist_: The performing artist.

- _Duration_: Total song length in seconds.

### How Metadata is Extracted

When a song is loaded:

1. Hummingbird opens the `.mp3` file using jaudiotagger.

2. It reads the embedded ID3 tags for _Title_, _Artist_, and _Duration_.

3. If metadata exists, it is displayed in the UI; if not, the filename is used as a fallback.

### User Recommendation

For best results, download `.mp3` files from sources that embed proper ID3 tags (e.g., iTunes, Amazon Music, Bandcamp, or properly ripped CDs).

Avoid files with missing or corrupted metadata, as Hummingbird will fall back to filenames.

---
## Recommended File Naming and Folder Structure

Hummingbird reads playlists and songs from a folder structure. Proper organization ensures smooth operation.

```
Folder Structure
users/
└─ your_username/
    ├─ playlists/
    │   ├─ Playlist1/
    │   │   ├─ song1.mp3
    │   │   ├─ song2.mp3
    │   │   └─ ...
    │   └─ Playlist2/
    │       ├─ songA.mp3
    │       ├─ songB.mp3
    │       └─ ...
    └─ user_login.txt (username + password)    
```


**Notes:**
* Each playlist is a folder under `playlists/`.
* Playlist name = folder name (e.g., Playlist1).
* All `.mp3` files in a folder are considered part of that playlist.

### File Naming

While metadata is preferred, clear filenames help when metadata is missing.

Recommended format:

    Artist - Title.mp3

    Example:
    Adele - Hello.mp3
    The Beatles - Hey Jude.mp3


- **Avoid special characters in filenames.**

- Only `.mp3` files are supported; other formats are ignored.

- Keep playlist folders flat (no nested subfolders).

---
## Using Hummingbird

1. Place your playlists under `users/<your_username>/playlists/`.

2. Launch the Hummingbird application.

3. Use the Library view to see available playlists.

4. Click the Open button to view a playlist in the player.

5. Click the Import button to add your own songs to a playlist.

6. Playback controls:

```
    Button                  |  Function
    --------------------------------------------------------------------------------
    Play/Pause              |  Starts/resumes/pauses playback
                            |
    Next                    |  Skips to next song in queue
                            |
    Previous                |  Goes back to previous song in queue
                            |
    Playlists               |  Displays currently loaded playlists
                            |
    Queue                   |  Displays currently loaded queue
                            |
    Import MP3              |  Allows for import of .mp3 files from file explorer
                            |
    Play Selected Playlist  |  Queues currently selected playlist
                            |
    Open Selected Playlist  |  Previews currently selected playlist
                            |
    Clear Queue             |  Clears the current queue
```
7. Drag-and-drop within the queue view to reorder songs.

8. Right-click a song in playlist view to add it to the queue individually.

**Metadata Display:** Only the song _Title_ is shown in the songLabel. Queue and list views display _Title_, _Artist_, and _Duration_.

---
## Tips and Best Practices

- Always use `.mp3` files with ID3 tags for the best experience.

- Keep playlists in separate folders with meaningful names.

- Avoid duplicate songs in the same playlist to prevent queue confusion.

- If metadata is missing, Hummingbird will display the filename.

---
This README ensures users understand both the technical metadata requirements and the practical folder/file organization needed for Hummingbird to work as intended.