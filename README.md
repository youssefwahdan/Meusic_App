# 🎵 Meusic Player

A modern, feature-rich Android music player built with Java and Android Jetpack components. Features offline playback, playlist management, favorites, and a beautiful Material Design interface.

![Android](https://img.shields.io/badge/Android-10+-green.svg)
![Java](https://img.shields.io/badge/Java-8+-blue.svg)
![Room](https://img.shields.io/badge/Room-Database-purple.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)
[![Release](https://img.shields.io/github/v/release/youssefwahdan/Meusic_App)](https://github.com/youssefwahdan/Meusic_App/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/youssefwahdan/Meusic_App/total.svg)](https://github.com/youssefwahdan/Meusic_App/releases)

## ✨ Features

### Core Features
- 🎶 **Full Music Library** - Browse all songs on your device
- 🎨 **Material Design** - Modern, clean UI with smooth animations
- 🎵 **Background Playback** - Play music even when app is closed
- 🔔 **Notification Controls** - Control playback from notification panel
- 📱 **Responsive Layout** - Works on phones and tablets

### Advanced Features
- ❤️ **Favorites** - Mark and quickly access your favorite songs
- 📋 **Playlists** - Create, edit, and manage custom playlists
- 🔀 **Playback Modes** - Shuffle, Repeat All, Repeat One, Normal
- 🔍 **Smart Search** - Search by song, artist, or album
- 🎨 **Album Art** - Display embedded album artwork
- 📊 **Sorting Options** - Sort by title, artist, album, duration, date
-  **Edge-to-Edge Display** - Modern fullscreen experience

### Technical Features
- ️ **Room Database** - Efficient local storage for playlists and favorites
- 🔄 **LiveData** - Reactive UI updates
- 🎯 **MVVM Architecture** - Clean, maintainable code structure
- 🔐 **Runtime Permissions** - Proper handling of storage permissions
- 💾 **State Persistence** - Remembers playback state after app restart

## 📸 Screenshots

<div align="center">
  <img src="screenshots/home.png" width="200" alt="Home Screen"/>
  <img src="screenshots/player.png" width="200" alt="Player Screen"/>
  <img src="screenshots/playlists.png" width="200" alt="Playlists"/>
  <img src="screenshots/favorites.png" width="200" alt="Favorites"/>
</div>

##  Installation

### From Source
1. Clone the repository:
   ```bash
   git clone https://github.com/youssefwahdan/Meusic_App
   cd Meusic_App

2. Open in Android Studio:
  - Launch Android Studio
  - Select "Open an existing project"
  - Navigate to the cloned directory

3. Build and Run:
  - Connect an Android device or start an emulator
  - Click "Run" (▶️) in Android Studio
  - Grant storage permissions when prompted

## Requirements
  - Android Studio Arctic Fox or later
  - Android SDK 21 (Android 5.0) or higher
  - Java 8 or higher

## 🛠️ Tech Stack
  - Language: Java
  - Architecture: MVVM (Model-View-ViewModel)
  - Database: Room Persistence Library
  - UI Components: Android Jetpack (ViewModel, LiveData), RecyclerView, ViewPager2, MotionLayout, Material Design Components
  - Audio: MediaPlayer API
  - Async: ExecutorService
  
## 📁 Project Structure
```
app/
├── java/com/example/first_app/
│   ├── Activities/
│   │   ├── AllSongsActivity.java
│   │   ├── MainActivity.java
│   │   ├── PlaylistDetailActivity.java
│   │   └── ...
│   ├── Fragments/
│   │   ├── SongsFragment.java
│   │   ├── ArtistsFragment.java
│   │   ├── AlbumsFragment.java
│   │   └── ...
│   ├── Adapters/
│   │   ├── SongAdapter.java
│   │   ├── PlaylistAdapter.java
│   │   └── ...
│   ├── Database/
│   │   ├── AppDatabase.java
│   │   ├── FavoriteDao.java
│   │   ├── PlaylistDao.java
│   │   └── ...
│   ├── Managers/
│   │   ├── PlayerManager.java
│   │   ├── MusicLibrary.java
│   │   └── FavoriteManager.java
│   └── Models/
│       ├── Song.java
│       ├── Playlist.java
│       └── ...
└── res/
    ├── layout/
    ├── drawable/
    └── values/
```

## ⚠️ Disclaimer
This app is for educational/portfolio purposes only. It plays music files stored locally on the user's device. The developer is not responsible for any copyrighted material played through this app.

## 🤝 Contributing
Contributions are welcome! Please feel free to submit a Pull Request.
1.Fork the project
2. Create your feature branch (git checkout -b feature/AmazingFeature)
3. Commit your changes (git commit -m 'Add some AmazingFeature')
4. Push to the branch (git push origin feature/AmazingFeature)
5. Open a Pull Request

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.

## 👤 Author
Youssef Khaled - [youssefwahdan](https://github.com/youssefwahdan)

---

##  Download

### Latest Release (v1.0.0)
[![Download APK](https://img.shields.io/badge/Download-APK-brightgreen.svg?style=for-the-badge&logo=android)](https://github.com/youssefwahdan/Meusic_App/releases/latest/download/Meusic.v1.0.0.apk)

**Direct Download:** [app-release.apk](https://github.com/youssefwahdan/Meusic_App/releases/download/v1.0.0/Meusic.v1.0.0.apk)

### Installation Instructions
1. Download the APK file
2. Enable "Install from Unknown Sources" in your Android settings
3. Open the downloaded file and tap "Install"
4. Launch the app and grant storage permissions

⚠️ **Note:** This app is for personal use only. Make sure to download from official releases only.

---
<div align="center">
<strong>Made with ❤️ for music lovers</strong>
</div>
