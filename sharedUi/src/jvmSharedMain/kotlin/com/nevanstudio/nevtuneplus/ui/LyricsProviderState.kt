// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui

/** What the current track's provider picker already knows without another request. */
enum class LyricsProviderState {
    NOT_FETCHED,
    FETCHING,
    FOUND,
    NOT_FOUND,
}
