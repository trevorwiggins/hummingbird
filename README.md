# 🎵 Hummingbird Audio Player

_A lightweight JavaFX music player with playlists, queues, metadata, and user accounts._

## 📚 Table of Contents

- Overview 
- Features
- Installation
- Project Structure
- How Playlists & Library Work
- Metadata Handling
- Using the App
- Best Practices
- Troubleshooting

---
## 📝 Overview

Hummingbird is a custom-built JavaFX audio player designed for simplicity, clarity, and reliability.
It uses a unique file-based playlist system and supports full metadata extraction through jaudiotagger.

---

## ✨ Features

### 🔊 Audio Playback

- Play / Pause 
- Next / Previous 
- Volume slider 
- Seek slider with real-time updates 
- Auto-advance to next queued track

### 📂 Playlist System

- Each playlist is a real folder 
- Uses .songlink files instead of duplicating MP3s 
- Unlimited playlists per user

### 🎶 Media Library

- All MP3 files stored once in /library/ 
- Playlists reference shared songs 
- Import MP3 files directly from your file system 
- Automatic metadata loading

### 🧠 Metadata Support (via jaudiotagger)

- Song title
- Artist
- Duration (in seconds)
- Filename fallback if metadata missing

### 🧩 Queue System

- Queue entire playlists or individual songs 
- Prevents duplicates 
- Drag-and-drop reordering 
- Persistent while app is open

### 👤 User Accounts

- Simple local login system 
- Each user gets their own isolated library + playlists 
- user_login.txt stored in each user folder 
- Auto-creates a demo “Sample Playlist” with sample MP3s

### 🎨 UI Features

- Modern PrimerDark theme 
- Clean, responsive JavaFX layout 
- Playlist Manager, Create Playlist screen, Audio Player screen 
- Blur-backdrop deletion popup

---
## ⚙️ Installation
1. Install Java 17 or newer

    JavaFX 21+ requires JDK 17+.
    You can download it from:
    https://adoptium.net/

2. Install jaudiotagger

    Hummingbird uses jaudiotagger to read MP3 metadata.

    Download: https://bitbucket.org/ijabz/jaudiotagger/downloads/jaudiotagger-2.0.1.jar

    Place the file in: `/libs/jaudiotagger-2.0.1.jar`

    Then add this to your pom.xml:
```xml
<dependency>
    <groupId>org.jaudiotagger</groupId>
    <artifactId>jaudiotagger</artifactId>
    <version>2.0.1</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/jaudiotagger-2.0.1.jar</systemPath>
</dependency>
```
3. Run the App

    From IntelliJ or command line:

    mvn clean javafx:run

---
## 📁 Project Structure 

```
Hummingbird/
│
├── src/main/java/com/example/hummingbird/
│   ├── application/
│   │   └── AudioPlayerApplication.java
│   │
│   ├── controller/
│   │   ├── LoginController.java
│   │   ├── PlaylistManagerController.java
│   │   ├── CreatePlaylistController.java
│   │   └── AudioPlayerController.java
│   │
│   └── model/
│       ├── UserSession.java
│       ├── Song.java
│       ├── PlaylistManager.java
│       ├── QueueManager.java
│       └── MP3FileImporter.java
│
├── src/main/resources/com/example/hummingbird/
│   ├── login_view.fxml
│   ├── playlist_manager_view.fxml
│   ├── create_playlist_view.fxml
│   ├── player_view.fxml
│   └── sample_songs/ (sample MP3 files for new users)
│
├── libs/
│   └── jaudiotagger-2.0.1.jar
│
└── pom.xml
```

---
## 🗂 How Playlists & Library Work

Hummingbird uses a two-folder architecture inside each user account:
```
users/
└─ <username>/
├─ playlists/
│   ├─ Rock/
│   │   ├─ song1.mp3.songlink
│   │   ├─ song2.mp3.songlink
│   │   └─ ...
│   └─ Chill/
│       ├─ trackA.mp3.songlink
│       └─ ...
│
├─ library/
│   ├─ song1.mp3
│   ├─ song2.mp3
│   ├─ trackA.mp3
│   └─ ...
│
└─ user_login.txt
```
### 🎯 How it works:

- MP3 files are stored only in the library/ folder. 
- Each playlist folder contains .songlink files, not real MP3s. 
- Each .songlink contains only the filename of its target MP3. 
- At runtime, PlaylistManager resolves these links to canonical Song objects, ensuring no duplicates.

This design keeps storage clean and ensures metadata is consistent across all playlists.

---
## 🏷 Metadata Handling

Hummingbird uses jaudiotagger to extract:

- Title 
- Artist 
- Duration (in seconds)

Fallback behavior:

- If no metadata is found:
  - Title → filename 
  - Artist → "unknown"
  - Duration → 0

This ensures every file is still usable even without proper tagging.

---
## 🎮 Using the Application
### 1️⃣ Login

- Enter username + password 
- If the user doesn’t exist, choose Create Account 
- New accounts are initialized with a Sample Playlist

### 2️⃣ Audio Player View

Contains full playback controls:

| Control             | Function                           |
| ------------------- | ---------------------------------- |
| **Play / Pause**    | Start or stop playback             |
| **Next / Previous** | Navigate queue                     |
| **Queue**           | View & reorder songs (drag + drop) |
| **Playlists**       | Return to playlist manager         |
| **Add to Queue**    | Add selected playlist or song      |
| **Clear Queue**     | Empties queue & resets player      |

The top label always shows the current song title.

### 3️⃣ Playlist Manager View

Here you can:

| Action               | Description                                   |
| -------------------- | --------------------------------------------- |
| **Playlists**        | Shows all playlist folders                    |
| **Open Playlist**    | View songs inside a playlist                  |
| **Import MP3**       | Copy songs into the library + add to playlist |
| **Add Song**         | Add an existing library track to a playlist   |
| **Remove Song**      | Remove from playlist (but not from library)   |
| **Delete Playlist**  | Deletes entire playlist folder                |
| **View All Songs**   | Shows every song in the library               |
| **Switch to Player** | Opens the audio player screen                 |

### 4️⃣ Create Playlist View

Used to build a new playlist:

- Enter playlist name
- Search music library (live filtering)
- Add songs to a temporary preview list
- Create playlist once satisfied
- The app writes .songlink files automatically

---
## 🌟 Best Practices

```
✔ Use properly tagged .mp3 files
✔ Name files clearly: Artist - Title.mp3
✔ Keep playlist folders flat (no subfolders)
✔ Don’t manually edit .songlink files
✔ Don’t manually delete files inside /library/
✔ Let the app manage playlist + library structure
```

---
## 🐞 Troubleshooting
### _Song displays “unknown” or wrong metadata_

The MP3 file has missing or corrupt ID3 tags.
Fix using tools like:

- Mp3Tag (Windows)

- Kid3 (Win/Mac/Linux)

### _Queue isn't playing automatically_

This happens if the MediaPlayer wasn’t initialized. 

→ Press Play after queueing a playlist.

### _Playlist or songs vanish_

Likely caused by manually editing playlist folders.
Let Hummingbird manage them.

### _MP3 won’t import_

Check:

- File must be .mp3 
- File must not be in use by another program 
- File path must be readable