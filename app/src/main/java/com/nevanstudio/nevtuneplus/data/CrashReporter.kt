// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.nevanstudio.nevtuneplus.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * Captures, stores, and analyzes application crashes and silent background terminations.
 *
 * Provides crash log inspection directly within the UI so users and developers can
 * diagnose silent stops, verify if an unhandled error occurred, and prevent OEM battery
 * killer kills.
 */
object CrashReporter {
    private const val TAG = "CrashReporter"
    private const val CRASH_FILE_NAME = "last_crash.txt"
    private const val HISTORY_FILE_NAME = "crash_history.log"
    private const val MAX_BREADCRUMBS = 50

    private val breadcrumbs = ArrayDeque<String>(MAX_BREADCRUMBS)
    private val breadcrumbLock = Any()

    private val _lastCrashState = MutableStateFlow<String?>(null)
    val lastCrashState: StateFlow<String?> = _lastCrashState.asStateFlow()

    private val _lastLowMemoryLevel = MutableStateFlow<Int?>(null)
    val lastLowMemoryLevel: StateFlow<Int?> = _lastLowMemoryLevel.asStateFlow()

    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        refreshCrashState()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            recordCrash(thread, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        recordBreadcrumb("App", "Application started (version=${BuildConfig.VERSION_NAME}, code=${BuildConfig.VERSION_CODE})")
    }

    fun recordBreadcrumb(tag: String, message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = "[$timestamp] [$tag] $message"
        synchronized(breadcrumbLock) {
            if (breadcrumbs.size >= MAX_BREADCRUMBS) {
                breadcrumbs.removeFirst()
            }
            breadcrumbs.addLast(entry)
        }
    }

    fun recordLowMemory(level: Int) {
        _lastLowMemoryLevel.value = level
        recordBreadcrumb("Memory", "onTrimMemory received: level=$level")
    }

    private fun recordCrash(thread: Thread, throwable: Throwable) {
        val context = appContext ?: return
        val timeString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val recentBreadcrumbs: List<String>
        synchronized(breadcrumbLock) {
            recentBreadcrumbs = breadcrumbs.toList()
        }

        val report = buildString {
            appendLine("═══════════════════════════════════════════════════════════")
            appendLine("           NEVTUNE+ CRASH REPORT")
            appendLine("═══════════════════════════════════════════════════════════")
            appendLine("Timestamp: $timeString")
            appendLine("Thread: ${thread.name} (id=${thread.id}, priority=${thread.priority})")
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message ?: "No message"}")
            appendLine()
            appendLine("─── Device & Environment ───")
            appendLine("Model: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
            appendLine("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App Version: ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
            appendLine("Build Type: ${BuildConfig.BUILD_TYPE}")
            appendLine("Available RAM: ${memoryInfo.availMem / (1024 * 1024)} MB")
            appendLine("Total RAM: ${memoryInfo.totalMem / (1024 * 1024)} MB")
            appendLine("Low Memory Threshold: ${memoryInfo.lowMemory}")
            appendLine()
            appendLine("─── Recent Breadcrumbs (Last ${recentBreadcrumbs.size}) ───")
            if (recentBreadcrumbs.isEmpty()) {
                appendLine("  (None recorded)")
            } else {
                recentBreadcrumbs.forEach { appendLine("  $it") }
            }
            appendLine()
            appendLine("─── Stack Trace ───")
            appendLine(throwable.stackTraceToString())
            appendLine("═══════════════════════════════════════════════════════════")
        }

        runCatching {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            file.writeText(report)
            _lastCrashState.value = report

            val history = File(context.filesDir, HISTORY_FILE_NAME)
            history.appendText(report + "\n\n")
        }
    }

    fun refreshCrashState() {
        val context = appContext ?: return
        val file = File(context.filesDir, CRASH_FILE_NAME)
        _lastCrashState.value = if (file.exists() && file.length() > 0) file.readText() else null
    }

    fun getLastCrash(): String? {
        val context = appContext ?: return null
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return if (file.exists() && file.length() > 0) file.readText() else null
    }

    fun hasCrash(): Boolean {
        val context = appContext ?: return false
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return file.exists() && file.length() > 0
    }

    fun clearCrashLog() {
        val context = appContext ?: return
        runCatching {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) file.delete()
            _lastCrashState.value = null
        }
    }

    /**
     * Checks if the app is currently excluded from Android's aggressive Doze & battery optimizations.
     * When false, OEM task killers or Doze mode can silently stop background playback with no crash log.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
    }

    /**
     * Launches the system dialog requesting exclusion from battery optimization to prevent silent kills.
     */
    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }.onFailure {
                // Fallback to standard battery settings
                runCatching {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    /**
     * Formats comprehensive diagnostic data for bug reports and troubleshooting.
     */
    fun getFullDiagnosticReport(context: Context): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val batteryIgnored = isBatteryOptimizationIgnored(context)

        return buildString {
            appendLine("NevTune+ Diagnostics Report")
            appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("Version: ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
            appendLine("Battery Optimization Ignored: $batteryIgnored (Silent kill protection)")
            appendLine("Total RAM: ${memoryInfo.totalMem / (1024 * 1024)} MB")
            appendLine("Available RAM: ${memoryInfo.availMem / (1024 * 1024)} MB")
            appendLine("Low Memory State: ${memoryInfo.lowMemory}")
            appendLine("Last Low Memory Trim: ${_lastLowMemoryLevel.value ?: "None"}")
            appendLine("Last Crash Log Present: ${hasCrash()}")
            getLastCrash()?.let { crash ->
                appendLine()
                appendLine("--- LAST CRASH DETAILS ---")
                appendLine(crash)
            }
        }
    }
}
