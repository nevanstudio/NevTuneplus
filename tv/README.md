<!--
Copyright (c) 2026 NevanStudio. All rights reserved.
Project: NevTune+
-->
# NevTune+ TV

Android TV / Google TV build of NevTune+: a remote-first Compose-for-TV interface
(home, library, search, settings, a full-screen now-playing view with synced
lyrics, display refresh-rate switching, spatial audio, and QR-code sign-in).

This module holds only what differs from the phone app. Everything else — the
data and playback layers, resources, native code — is built straight from
[`app/`](../app), together with the shared `:shared` and `:sharedUi` modules.

| Path | What it is |
| --- | --- |
| `src/main/java/.../ui/tv/**`, `TvActivity.kt` | The TV interface |
| `src/main/java/...` (same path as a file under `app/`) | A phone-app class TV patches; it replaces the `app/` one at build time |
| `src/main/res` | TV-only resources: banner, fonts, TV theme |
| `src/main/AndroidManifest.xml` | TV's own manifest (`LEANBACK_LAUNCHER`, `TvActivity`) |

## Download and install

Download the **NevTune+ TV 1.0.0 APK** from the [GitHub release](https://github.com/nevanstudio/NevTuneplus/releases/download/v1.0.0/NevTunePlusTV-1.0.0.apk). This is the Android TV / Google TV package; do not install the phone APK on a TV device. The [release page](https://github.com/nevanstudio/NevTuneplus/releases/tag/v1.0.0) contains the complete release inventory.

Install the APK using your preferred Android TV sideloading method. The device must allow installation from the source you use.
## Build

```bash
./gradlew :tv:assembleProdDebug
```

The module is only included when an Android SDK is available, like `:app`.
---

> **NevTune+** — made by [NevanStudio](https://github.com/nevanstudio).
