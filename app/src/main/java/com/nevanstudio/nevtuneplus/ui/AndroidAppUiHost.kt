// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nevanstudio.nevtuneplus.data.settings.AppSettings
import com.nevanstudio.nevtuneplus.data.settings.LibraryViewType
import com.nevanstudio.nevtuneplus.download.Downloads

/** The shared pages' settings and downloads, answered from the phone's own stores. */
object AndroidAppUiHost : AppUiHost {
    override val swipeToPlayNext = AppSettings.swipeToPlayNext
    override val homeRecentsViewType = AppSettings.homeRecentsViewType
    override fun setHomeRecentsViewType(value: LibraryViewType) = AppSettings.setHomeRecentsViewType(value)
    override val showWeeklyRecapCard = AppSettings.showWeeklyRecapCard
    override fun setWeeklyRecapSeenWeek(value: Long) = AppSettings.setWeeklyRecapSeenWeek(value)
    override val pinnedPlaylists = AppSettings.pinnedPlaylists
    override val librarySort = AppSettings.librarySort
    override val adaptivePerformance = AppSettings.adaptivePerformance

    @Composable
    override fun downloadedIds(): Set<String> {
        val saved by Downloads.saved.collectAsStateWithLifecycle()
        return saved.keys
    }
}
