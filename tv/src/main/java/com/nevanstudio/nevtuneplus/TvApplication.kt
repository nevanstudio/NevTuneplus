// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.nevanstudio.nevtuneplus.data.CrashReporter
import com.nevanstudio.nevtuneplus.ui.tv.DeviceType
import com.nevanstudio.nevtuneplus.ui.tv.components.TvComponents

/**
 * Dedicated Application class for Android TV / Google TV.
 *
 * Implements a lightweight, crash-resilient runtime profile:
 * - Ultra-fast non-blocking cold startup.
 * - Bounded memory caches sized specifically for 2GB RAM / 1.5GB RAM TV hardware.
 * - Proactive foreground and background memory trimming to prevent Low Memory Killer terminations.
 * - Compact image disk caching to protect TV flash storage.
 */
@OptIn(UnstableApi::class)
class TvApplication : NevTunePlusApplication() {

    override fun onCreate() {
        super.onCreate()
        val isLite = DeviceType.isTvLite(this)
        val ramMb = DeviceType.getTotalRamBytes(this) / (1024 * 1024)
        CrashReporter.recordBreadcrumb(
            "TvApp",
            "TvApplication started (RAM=${ramMb}MB, Cores=${DeviceType.getCpuCoreCount()}, LiteMode=$isLite)",
        )
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val isLite = DeviceType.isTvLite(context)
        val isLowRam = DeviceType.isLowRam(context)

        return ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
                add(OkHttpNetworkFetcherFactory(callFactory = { coilHttpClient }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    // Low RAM (2GB or less): 10% heap cap. Standard TV: 14% heap cap.
                    .maxSizePercent(context, if (isLite || isLowRam) 0.10 else 0.14)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("tv_image_cache"))
                    .maxSizeBytes(if (isLite || isLowRam) 30L * 1024 * 1024 else 50L * 1024 * 1024)
                    .build()
            }
            .crossfade(if (isLite || isLowRam) 0 else 160)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Aggressively free image and color caches across all low memory triggers
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            runCatching {
                coil3.SingletonImageLoader.get(this).memoryCache?.clear()
                TvComponents.clearCache()
            }
            if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
                System.gc()
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        runCatching {
            coil3.SingletonImageLoader.get(this).memoryCache?.clear()
            TvComponents.clearCache()
            System.gc()
        }
    }
}
