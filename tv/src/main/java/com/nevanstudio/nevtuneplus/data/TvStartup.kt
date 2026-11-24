// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.data

import android.content.Context
import androidx.startup.Initializer
import com.nevanstudio.nevtuneplus.data.settings.TvSettings
import com.nevanstudio.nevtuneplus.ui.player.CanvasPlaybackLimits
import com.nevanstudio.nevtuneplus.ui.tv.DeviceType

/**
 * Runs once at process start, before any activity or the playback service, so
 * the TV-only hooks into the shared code are in place whichever of them gets
 * there first. Registered in the TV manifest.
 */
class TvStartup : Initializer<Unit> {
    override fun create(context: Context) {
        TvSettings.init(context)
        TvUsbMusic.install()

        if (DeviceType.isTv(context)) {
            // On low-RAM (2GB or less) or TV Lite mode, video canvas playback is completely disabled
            // to eliminate hardware decoder contention, OOM, and frame drops.
            // On higher-end TV hardware, capped at 24fps to spare the GPU.
            if (DeviceType.isTvLite(context)) {
                CanvasPlaybackLimits.maxFrameRate = 0
            } else {
                CanvasPlaybackLimits.maxFrameRate = 24
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
