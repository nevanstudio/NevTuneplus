// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.playback

/** Maps the raw Media3 position onto the user-adjusted lyric timeline. */
internal fun adjustedLyricSubtitlePosition(positionMs: Long, offsetMs: Int): Long =
    (positionMs - offsetMs.toLong()).coerceAtLeast(0L)
