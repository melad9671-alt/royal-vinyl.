# Offline Music Player

A clean native Android music player built from scratch with Kotlin, Jetpack Compose, and AndroidX Media3.

## Current baseline
- Scans audio files stored on the device through MediaStore.
- Plays full local audio files with ExoPlayer.
- Search by song title, artist, or album.
- Favorites persisted locally.
- Playback queue, previous/next, seek, shuffle, and repeat controls.
- Black interface with soft-purple accents.
- No account, paid API, or subscription required for local playback.

## Build
GitHub Actions builds a debug APK on pushes to `offline-music-player` and on manual dispatch. Download the `Offline-Music-Player-debug` artifact from the workflow run.

## Project status
This branch is a fresh baseline, not a migration of the old application. The four user-requested project-specific changes still need their exact acceptance criteria applied and verified.
