<p align="center">
  <a href="https://nevtuneplus.vercel.app">
    <img src="https://nevtuneplus.vercel.app/assets/icon.png" alt="NevTune+ Logo" width="112" />
  </a>
</p>

<h1 align="center">NevTune+</h1>

<p align="center">
  <strong>Stream without ads. Keep your offline library. Listen across every screen.</strong>
</p>

<p align="center">
  <a href="https://nevtuneplus.vercel.app">
    <img src="https://img.shields.io/badge/Website-nevtuneplus.vercel.app-00C853?style=for-the-badge&logo=vercel&logoColor=white" alt="Official Website" />
  </a>
  <a href="https://github.com/nevanstudio/NevTuneplus/releases/latest">
    <img src="https://img.shields.io/github/v/release/nevanstudio/NevTuneplus?style=for-the-badge&color=10B981&label=Release" alt="Latest Release" />
  </a>
  <a href="https://github.com/nevanstudio/NevTuneplus/releases">
    <img src="https://img.shields.io/github/downloads/nevanstudio/NevTuneplus/total?style=for-the-badge&color=0EA5E9&label=Downloads" alt="Total Downloads" />
  </a>
  <a href="./LICENSE">
    <img src="https://img.shields.io/badge/License-GPL--3.0-6366F1?style=for-the-badge" alt="GPL-3.0 License" />
  </a>
</p>

<p align="center">
  <a href="https://nevtuneplus.vercel.app/download">
    <img src="https://img.shields.io/badge/Download%20Builds-Mobile%20%C2%B7%20TV%20%C2%B7%20Wear%20OS-16A34A?style=flat-square" alt="Download Builds" />
  </a>
  &nbsp;
  <a href="https://github.com/nevanstudio/NevTuneplus/releases">
    <img src="https://img.shields.io/badge/Changelogs-Releases-334155?style=flat-square" alt="Release Notes" />
  </a>
  &nbsp;
  <a href="https://github.com/nevanstudio/NevTuneplus/issues/new?template=bug_report.yml">
    <img src="https://img.shields.io/badge/Bug%20Report-Open%20Issue-DC2626?style=flat-square" alt="Report a Bug" />
  </a>
  &nbsp;
  <a href="https://github.com/nevanstudio/NevTuneplus/commits/main">
    <img src="https://img.shields.io/github/last-commit/nevanstudio/NevTuneplus?style=flat-square&color=475569" alt="Last Commit" />
  </a>
</p>

---

**NevTune+** is an open-source music player and streaming client by **NevanStudio**. It combines YouTube Music, Spotify playlist sync, and JioSaavn discovery with a full local audio player — built natively for Android phones, tablets, Android TV, Wear OS watches, and Windows desktop. No ads, no subscriptions, and zero analytics tracking.

> 🌐 **Official Website & Direct Downloads:** [nevtuneplus.vercel.app](https://nevtuneplus.vercel.app)

---

## Screenshots

<div align="center">

| Now Playing | Synced Lyrics | Music Library |
|:-----------:|:-------------:|:-------------:|
| <img src="https://nevtuneplus.vercel.app/assets/screenshots/now_playing_screen.png" width="240" alt="Now Playing screen" /> | <img src="https://nevtuneplus.vercel.app/assets/screenshots/half_lyrics_screen.png" width="240" alt="Synced lyrics view" /> | <img src="https://nevtuneplus.vercel.app/assets/screenshots/library_screen.png" width="240" alt="Library view" /> |

| Artist Page | Audio Hardware Profile | Sleep Timer |
|:-----------:|:----------------------:|:-----------:|
| <img src="https://nevtuneplus.vercel.app/assets/screenshots/full_artist_screen.png" width="240" alt="Artist screen" /> | <img src="https://nevtuneplus.vercel.app/assets/screenshots/device_capability_screen.png" width="240" alt="Device capabilities screen" /> | <img src="https://nevtuneplus.vercel.app/assets/screenshots/sleep_timer_screen.png" width="240" alt="Sleep timer screen" /> |

</div>

---

## Highlights & Features

### Playback & DSP Audio Engine
| Feature | Details |
|---|---|
| **Playback Modes** | Gapless playback, adjustable crossfade, queue restore, and screen-off background sessions |
| **Audio Processing** | 10-band EQ with presets, AutoEQ profiles, loudness normalization, and spatial mixing |
| **Native Decoders** | Built-in FFmpeg (`librempeg`) + Media3/ExoPlayer — FLAC, ALAC, Opus, AAC, MP3, DSD, DTS, AC-3/E-AC-3, HLS & DASH |
| **Smart Tools** | Playback speed (`0.5x – 2.0x`) with pitch lock, silence skipping, and sleep timer with fade-out |
| **Hardware Routing** | Speaker, wired/Bluetooth audio, Google Cast, SMB2/3 (`SMBJ`), and WebDAV network shares |

### Discovery, Lyrics & Social
- **Unified Catalog** — Search and stream via YouTube Music (`InnerTube`), import Spotify playlists + Canvas motion artwork, or browse JioSaavn.
- **Offline Library** — Download tracks, albums, and playlists locally with full metadata tagging (FLAC/MP4/WebM) alongside your existing device files.
- **Multi-Provider Lyrics** — Synced LRC & word/line timing with tap-to-seek and translation across LRCLib, YouTube Music, Genius, Kugou, Apple, BetterLyrics, and more.
- **Listen Together** — Host or join live synced listening rooms backed by the Go party server (`backend/`).
- **Scrobbling & Presence** — Optional Last.fm scrobbling, ListenBrainz support, and Discord Rich Presence.

---

## Multi-Platform Support

| Surface | Module | Requirements | Package / Output |
|---|---|---|---|
| **Android Phone & Tablet** | `:app` | Android 8.0+ (`minSdk 26`, `targetSdk 36`) | `NevTune+ Mobile - <version>.apk` |
| **Android TV / Google TV** | `:tv` | Android 8.0+ (`minSdk 26`, `targetSdk 36`) | `NevTune+ TV - <version>.apk` |
| **Wear OS** | `:wearos` | Wear OS 3.0+ (`minSdk 30`, `targetSdk 36`) | `NevTune+ Wear OS - <version>.apk` |
| **Desktop (Work in Progress)** | `desktop/` | Rust + Tauri/egui workspace | Native desktop binaries |

> If a release is published with only Mobile selected, the APK is named `NevTune+ - <version>.apk`. All multi-platform builds include a `SHA256SUMS.txt` checksum manifest.

---

## Project Structure

```text
NevTuneplus/
├── app/                         # Primary Android phone & tablet application
│   └── src/main/java/com/nevanstudio/nevtuneplus/
│       ├── auth/                # Sign-in flows & token handling (YT Music, Spotify, Discord)
│       ├── data/                # API clients, update checker, lyrics, scrobblers, DB
│       ├── download/            # Background downloader & container taggers
│       ├── playback/            # Media3/ExoPlayer pipeline, DSP audio sink, Cast, SMB/WebDAV
│       ├── ui/                  # Compose screens, MiniPlayer, theming, settings
│       ├── wear/                # Phone-to-watch sync bridge
│       └── widget/              # Home screen playback widgets
├── tv/                          # Android TV / Google TV 10-foot D-pad experience
│   └── src/main/java/com/nevanstudio/nevtuneplus/
├── wearos/                      # Wear OS watch companion app
│   └── src/main/java/com/nevanstudio/nevtuneplus/
├── shared/                      # Kotlin Multiplatform (KMP) shared domain & network logic
├── sharedUi/                    # Shared Compose Multiplatform UI components & theme system
├── native/                      # Native C/C++ audio engine
│   ├── analyzer/                # Signal analysis & DSP helpers
│   └── ffmpeg/                  # Custom FFmpeg (librempeg) native bindings
├── desktop/                     # Rust desktop client workspace (crates: music, ui, views, etc.)
├── backend/                     # Go server for Listen Together rooms, presence & clock sync
├── docs/
│   ├── assets/banners/          # Release header banners (1280x640, banner_<version>.png)
│   └── releases/                # Per-version release notes (RELEASE_NOTES_<version>.md)
├── scripts/                     # Build, CalVer bump, and setup helpers
│   ├── bump-version.ps1         # Stamps CalVer across Gradle, Cargo, lyrics UA & release notes
│   ├── build-apks.bat / .sh     # Local release APK staging scripts
│   └── setup.ps1 / .sh          # Dev environment bootstrap scripts
└── .github/
    ├── ISSUE_TEMPLATE/          # Bug report & tester feedback templates
    └── workflows/               # CI checks (check.yml), signed releases (android.yml), backend deploy
```

---

## Versioning & How Updates Work

### Calendar Versioning (`CalVer`)
NevTune+ uses **UTC Calendar Versioning** in the format **`YY.MM.DD`** (for example, `26.11.25`) instead of arbitrary semantic numbers:

| Build Type | `versionName` | `versionCode` | Why |
|---|---|---|---|
| **Debug / Local Dev** | `1.0` | `1` | Pinned so midnight UTC rollovers don't invalidate Gradle's local build cache |
| **Release Build** | `YY.MM.DD` (e.g. `26.11.25`) | `YYMMDD00` (e.g. `26112500`) | All three Android surfaces (`:app`, `:tv`, `:wearos`) lock to the exact same version |
| **Desktop (`Cargo.toml`)** | `YY.M.D` (e.g. `26.1.5`) | — | Leading zeros stripped automatically by `scripts/bump-version.ps1` for Cargo SemVer compatibility |

To prepare a new version locally or inspect what will change:

```powershell
# Preview version bump + changelog generation without writing files
./scripts/bump-version.ps1 -DryRun

# Stamp today's UTC CalVer (or pass an explicit date like 26.12.01)
./scripts/bump-version.ps1

# Stamp, commit, and push a release tag (vYY.MM.DD)
./scripts/bump-version.ps1 -Push
```

You can also pass versions directly to Gradle at build time:

```bash
./gradlew :app:assembleProdRelease -Pnevtune.release=true -PnevtuneVersionName=26.11.25 -PnevtuneVersionCode=26112500
```



---

## Tech Stack

| Layer | Technologies |
|---|---|
| **Language** | Kotlin `2.4.21` · Rust (desktop) · Go (backend) · C/C++ (FFmpeg JNI) |
| **Android UI** | Jetpack Compose · Material 3 · Compose BOM `2026.09.00` · Haze progressive blur |
| **Multiplatform** | Kotlin Multiplatform (`:shared`, `:sharedUi`) · Compose Multiplatform `1.12.2` |
| **Media & Extraction** | Media3 / ExoPlayer `1.11.1` · NewPipeExtractor `v0.26.3` · InnerTubeX · FFmpeg (`librempeg`) |
| **Storage & Network** | Room · EncryptedSharedPreferences · OkHttp · Ktor · SMBJ |
| **Audio / ML** | ONNX Runtime (automix beat analysis) · 32-bit float DSP pipeline |
| **Build Tooling** | Android Gradle Plugin `9.3.1` · JDK `21` · CMake `3.22.1` |

---

## Building from Source

### Prerequisites
- **JDK 21**
- **Android Studio** (latest stable) with Android SDK (`compileSdk 37`, `minSdk 26`, `targetSdk 36`)
- **CMake 3.22.1** + Android NDK (installed via SDK Manager for `native/` builds)

### Commands

```bash
git clone https://github.com/nevanstudio/NevTuneplus.git
cd NevTuneplus

# Run mobile debug build
./gradlew :app:assembleDevDebug

# Install debug build directly to a connected phone
./gradlew :app:installDevDebug

# Build production release APKs locally (unsigned unless keystore.properties is configured)
./gradlew :app:assembleProdRelease :tv:assembleProdRelease :wearos:assembleProdRelease
```

<details>
<summary>Optional: local.properties & signing setup</summary>

Create a `local.properties` file in the root directory if you want to include optional service keys in your local build:

```properties
LASTFM_API_KEY=your_key_here
LASTFM_SECRET=your_secret_here
LISTEN_TOGETHER_SERVER=https://your-server.example.com
```

For local signed release builds, create `keystore.properties` (already gitignored):

```properties
storeFile=path/to/keystore.jks
storePassword=your_store_password
keyAlias=your_alias
keyPassword=your_key_password
```

</details>

---

## Contributing & Community

Bug reports, tester feedback, and pull requests are welcome:
- **Official Website:** [nevtuneplus.vercel.app](https://nevtuneplus.vercel.app)
- **Report a Bug:** [Bug Report Template](https://github.com/nevanstudio/NevTuneplus/issues/new?template=bug_report.yml)
- **Tester Feedback:** [Feedback Form](https://github.com/nevanstudio/NevTuneplus/issues/new?template=tester-feedback.yml)
- **Guidelines:** See [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md)

---

## License

NevTune+ is free, open-source software licensed under the **GNU General Public License v3.0 (GPL-3.0)**.  
See [LICENSE](./LICENSE) for the full terms.

<p align="center">
  Built by <a href="https://github.com/nevanstudio">NevanStudio</a>
</p>
