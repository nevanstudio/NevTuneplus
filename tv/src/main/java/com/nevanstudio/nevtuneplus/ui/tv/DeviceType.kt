// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.tv

import android.app.ActivityManager
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * Universal device capability and form-factor classifier.
 *
 * Distinguishes Android TV / Google TV / Fire TV devices from handhelds/tablets
 * using standard Android framework APIs. Also exposes low-RAM and hardware capability
 * detection so the UI and playback engines can scale their memory, graphics, and
 * animations gracefully without hardcoded OEM allowlists.
 */
object DeviceType {

    /**
     * Determines whether the current running environment is an Android TV / Google TV /
     * Fire OS TV device.
     */
    fun isTv(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true
        }
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                pm.hasSystemFeature("android.hardware.type.television")
    }

    /**
     * Returns true if the device has a physical touchscreen.
     */
    fun hasTouchScreen(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    }

    /**
     * Returns true if the device is a memory-constrained (low-RAM) device.
     * Evaluates both Android's isLowRamDevice flag AND checks for total system
     * RAM <= 2.5GB to ensure all 2GB RAM and 1.5GB RAM TV devices (like Chromecast,
     * Mi Box, onn. TV boxes, Fire Sticks) are reliably identified.
     */
    fun isLowRam(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        if (am.isLowRamDevice) return true
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        // 2.5GB threshold reliably covers 2GB and 1.5GB TV boards
        return memInfo.totalMem > 0L && memInfo.totalMem <= 2_684_354_560L
    }

    /**
     * Returns total physical system RAM in bytes, or 0 if unqueryable.
     */
    fun getTotalRamBytes(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0L
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.totalMem
    }

    /**
     * Returns the available CPU core count.
     */
    fun getCpuCoreCount(): Int = Runtime.getRuntime().availableProcessors()

    /**
     * Returns true if the hardware is low-end (e.g. quad-core A53 or <= 2.5GB RAM).
     */
    fun isLowEndHardware(context: Context): Boolean {
        return isLowRam(context) || getCpuCoreCount() <= 4
    }

    /**
     * Determines whether the TV app should operate in TV Lite mode.
     * Respects user preference in [com.nevanstudio.nevtuneplus.data.settings.TvSettings]
     * if configured, or automatically defaults to true on low-RAM / low-end hardware.
     */
    fun isTvLite(context: Context): Boolean {
        val pref = com.nevanstudio.nevtuneplus.data.settings.TvSettings.tvLiteMode.value
        return when (pref) {
            "on" -> true
            "off" -> false
            else -> isLowRam(context)
        }
    }

    /**
     * Returns true if the device has a working microphone hardware feature.
     */
    fun hasMicrophone(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    }
}
