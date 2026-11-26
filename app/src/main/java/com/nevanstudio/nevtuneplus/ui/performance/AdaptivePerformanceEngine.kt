// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.performance

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Snapshot of device hardware and current system resource indicators used to classify
 * the device into [PerformanceTier.LOW_END], [PerformanceTier.MID_RANGE], or [PerformanceTier.HIGH_END].
 */
data class HardwareSnapshot(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val isLowRamDevice: Boolean,
    val memoryClassMb: Int,
    val cpuCores: Int,
    val is64BitAbi: Boolean,
    val apiLevel: Int,
    val glEsMajorVersion: Int,
    val isPowerSaveMode: Boolean,
    val thermalStatus: Int,
) {
    val availableRamRatio: Double
        get() = if (totalRamMb <= 0L) 0.0 else availableRamMb.toDouble() / totalRamMb.toDouble()

    fun summaryLabel(): String {
        val ramGb = ((totalRamMb + 511L) / 1024L).coerceAtLeast(1L)
        val arch = if (is64BitAbi) "64-bit" else "32-bit"
        return "${ramGb} GB RAM · ${cpuCores} cores · $arch · Android API $apiLevel"
    }
}

data class RuntimePressureSignals(
    val memoryPressure: Boolean = false,
    val thermalOrPowerPressure: Boolean = false,
    val frameDropPressure: Boolean = false,
) {
    val anyPressure: Boolean
        get() = memoryPressure || thermalOrPowerPressure || frameDropPressure

    fun reasonLabel(): String? = when {
        memoryPressure -> "memory pressure"
        frameDropPressure -> "dropped UI frames"
        thermalOrPowerPressure -> "power saver / thermal throttling"
        else -> null
    }
}

internal data class RuntimeAdaptationState(
    val downgradedSteps: Int = 0,
    val lastDowngradeAtMs: Long = 0L,
    val lastPressureAtMs: Long = 0L,
    val downgradeReason: String? = null,
)

internal const val RUNTIME_DOWNGRADE_COOLDOWN_MS = 20_000L
internal const val RUNTIME_RECOVERY_HOLD_MS = 45_000L
internal const val FRAME_DROP_WINDOW_MS = 2_000L
internal const val FRAME_DROP_THRESHOLD_COUNT = 8
internal const val SLOW_FRAME_MULTIPLIER = 1.85f
internal const val HARD_JANK_FRAME_MS = 48L

/**
 * Pure multi-signal hardware classifier (RAM, CPU cores, 64-bit ABI, API level,
 * OpenGL ES version, memory availability, power save, and thermal state).
 */
internal fun classifyDeviceHardware(snapshot: HardwareSnapshot): PerformanceTier {
    // Hard floors for low-end / 2GB or constrained older hardware
    if (snapshot.isLowRamDevice) return PerformanceTier.LOW_END
    if (snapshot.totalRamMb in 1L..2350L) return PerformanceTier.LOW_END
    if (snapshot.cpuCores <= 2) return PerformanceTier.LOW_END
    if (!snapshot.is64BitAbi && snapshot.totalRamMb <= 3300L) return PerformanceTier.LOW_END
    if (snapshot.apiLevel < Build.VERSION_CODES.P && snapshot.totalRamMb <= 3300L) return PerformanceTier.LOW_END
    if (snapshot.glEsMajorVersion in 1..2) return PerformanceTier.LOW_END

    var score = 0
    score += when {
        snapshot.totalRamMb >= 5600L -> 4
        snapshot.totalRamMb >= 3600L -> 2
        snapshot.totalRamMb >= 2400L -> 1
        else -> 0
    }
    score += when {
        snapshot.cpuCores >= 8 -> 2
        snapshot.cpuCores >= 6 -> 1
        else -> 0
    }
    if (snapshot.is64BitAbi) score += 1 else score -= 1
    if (snapshot.apiLevel >= Build.VERSION_CODES.S) score += 1
    else if (snapshot.apiLevel < Build.VERSION_CODES.Q) score -= 1
    if (snapshot.memoryClassMb in 1..128) score -= 1

    if (snapshot.availableRamRatio < 0.12) score -= 2
    else if (snapshot.availableRamRatio < 0.18) score -= 1

    if (snapshot.isPowerSaveMode) score -= 1
    if (snapshot.thermalStatus >= 3) score -= 2

    return when {
        score <= 2 -> PerformanceTier.LOW_END
        score >= 6 &&
            snapshot.totalRamMb >= 5200L &&
            snapshot.is64BitAbi &&
            snapshot.cpuCores >= 6 -> PerformanceTier.HIGH_END
        else -> PerformanceTier.MID_RANGE
    }
}

/**
 * Evaluates runtime adaptation transitions with hysteresis and cooldowns to prevent
 * profile flapping when transient spikes occur.
 */
internal fun evaluateRuntimeAdaptation(
    current: RuntimeAdaptationState,
    signals: RuntimePressureSignals,
    baseTier: PerformanceTier,
    nowMs: Long,
): RuntimeAdaptationState {
    if (baseTier == PerformanceTier.LOW_END) {
        return RuntimeAdaptationState(downgradedSteps = 0, downgradeReason = null)
    }
    val maxSteps = if (baseTier == PerformanceTier.HIGH_END) 2 else 1
    if (signals.anyPressure) {
        val canStepDown = current.downgradedSteps < maxSteps &&
            (current.downgradedSteps == 0 || (nowMs - current.lastDowngradeAtMs) >= RUNTIME_DOWNGRADE_COOLDOWN_MS)
        val nextSteps = if (canStepDown) current.downgradedSteps + 1 else current.downgradedSteps
        return current.copy(
            downgradedSteps = nextSteps,
            lastDowngradeAtMs = if (canStepDown) nowMs else current.lastDowngradeAtMs,
            lastPressureAtMs = nowMs,
            downgradeReason = signals.reasonLabel() ?: current.downgradeReason,
        )
    }

    if (current.downgradedSteps > 0 && (nowMs - current.lastPressureAtMs) >= RUNTIME_RECOVERY_HOLD_MS) {
        val nextSteps = (current.downgradedSteps - 1).coerceAtLeast(0)
        return current.copy(
            downgradedSteps = nextSteps,
            lastPressureAtMs = nowMs,
            downgradeReason = if (nextSteps == 0) null else current.downgradeReason,
        )
    }
    return current
}

internal fun resolveEffectiveTier(
    baseTier: PerformanceTier,
    downgradedSteps: Int,
): PerformanceTier {
    val index = (baseTier.ordinal - downgradedSteps).coerceIn(0, PerformanceTier.entries.lastIndex)
    return PerformanceTier.entries[index]
}

internal fun isProfileDemandingForHardware(
    selectedMode: PerformanceProfileMode,
    hardwareTier: PerformanceTier,
): Boolean = when (selectedMode) {
    PerformanceProfileMode.AUTO, PerformanceProfileMode.LITE -> false
    PerformanceProfileMode.BALANCED -> hardwareTier == PerformanceTier.LOW_END
    PerformanceProfileMode.HIGH_QUALITY -> hardwareTier != PerformanceTier.HIGH_END
}

/**
 * Central Adaptive Performance Engine: detects hardware capabilities at startup,
 * monitors runtime memory/thermal/frame pressure, and produces a unified [AdaptivePerformanceConfig].
 */
object AdaptivePerformanceEngine {

    private val _hardwareSnapshot = MutableStateFlow<HardwareSnapshot?>(null)
    val hardwareSnapshot: StateFlow<HardwareSnapshot?> = _hardwareSnapshot.asStateFlow()

    private val _hardwareTier = MutableStateFlow(PerformanceTier.MID_RANGE)
    val hardwareTier: StateFlow<PerformanceTier> = _hardwareTier.asStateFlow()

    private val _config = MutableStateFlow(AdaptivePerformanceConfig.BalancedDefault)
    val config: StateFlow<AdaptivePerformanceConfig> = _config.asStateFlow()

    @Volatile
    private var profileMode: PerformanceProfileMode = PerformanceProfileMode.AUTO

    @Volatile
    private var pressureSignals = RuntimePressureSignals()

    @Volatile
    private var runtimeState = RuntimeAdaptationState()

    @Volatile
    private var lastSystemCheckMs = 0L

    @Volatile
    var onConfigChanged: ((AdaptivePerformanceConfig) -> Unit)? = null

    fun initialize(context: Context, initialMode: PerformanceProfileMode): AdaptivePerformanceConfig {
        profileMode = initialMode
        val snapshot = readHardwareSnapshot(context.applicationContext)
        _hardwareSnapshot.value = snapshot
        val detected = classifyDeviceHardware(snapshot)
        _hardwareTier.value = detected
        pressureSignals = pressureSignals.copy(
            memoryPressure = snapshot.isLowRamDevice || snapshot.availableRamRatio < 0.12,
            thermalOrPowerPressure = snapshot.isPowerSaveMode || snapshot.thermalStatus >= 3,
        )
        runtimeState = RuntimeAdaptationState()
        return recomputeConfig(nowMs = System.currentTimeMillis())
    }

    fun setProfileMode(mode: PerformanceProfileMode): AdaptivePerformanceConfig {
        profileMode = mode
        runtimeState = RuntimeAdaptationState()
        return recomputeConfig(nowMs = System.currentTimeMillis())
    }

    fun refreshSystemPressure(context: Context, nowMs: Long = System.currentTimeMillis()): AdaptivePerformanceConfig {
        if (nowMs - lastSystemCheckMs < 4_000L) return _config.value
        lastSystemCheckMs = nowMs
        val snapshot = readHardwareSnapshot(context.applicationContext)
        _hardwareSnapshot.value = snapshot
        pressureSignals = pressureSignals.copy(
            memoryPressure = snapshot.availableRamRatio < 0.12 || snapshot.isLowRamDevice,
            thermalOrPowerPressure = snapshot.isPowerSaveMode || snapshot.thermalStatus >= 3,
        )
        return recomputeConfig(nowMs)
    }

    @Suppress("DEPRECATION")
    fun onTrimMemory(level: Int, nowMs: Long = System.currentTimeMillis()): AdaptivePerformanceConfig {
        val severe = level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
        val critical = level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        if (severe || critical) {
            pressureSignals = pressureSignals.copy(memoryPressure = true)
            return recomputeConfig(nowMs)
        }
        return _config.value
    }

    fun reportFrameDropPressure(highJank: Boolean, nowMs: Long = System.currentTimeMillis()): AdaptivePerformanceConfig {
        if (pressureSignals.frameDropPressure == highJank && !highJank && runtimeState.downgradedSteps == 0) {
            return _config.value
        }
        pressureSignals = pressureSignals.copy(frameDropPressure = highJank)
        return recomputeConfig(nowMs)
    }

    private fun recomputeConfig(nowMs: Long): AdaptivePerformanceConfig {
        val hwTier = _hardwareTier.value
        val baseTier = when (profileMode) {
            PerformanceProfileMode.AUTO -> hwTier
            PerformanceProfileMode.LITE -> PerformanceTier.LOW_END
            PerformanceProfileMode.BALANCED -> PerformanceTier.MID_RANGE
            PerformanceProfileMode.HIGH_QUALITY -> PerformanceTier.HIGH_END
        }

        // Only Auto (and Balanced when struggling severely) auto-downgrades at runtime;
        // explicit Lite is already minimal, and explicit High Quality honors user override
        // unless critical memory pressure hits.
        val effectiveSignals = when (profileMode) {
            PerformanceProfileMode.LITE -> RuntimePressureSignals()
            PerformanceProfileMode.HIGH_QUALITY -> pressureSignals.copy(
                frameDropPressure = false,
                thermalOrPowerPressure = false,
            )
            PerformanceProfileMode.BALANCED, PerformanceProfileMode.AUTO -> pressureSignals
        }

        runtimeState = evaluateRuntimeAdaptation(
            current = runtimeState,
            signals = effectiveSignals,
            baseTier = baseTier,
            nowMs = nowMs,
        )

        val effectiveTier = resolveEffectiveTier(baseTier, runtimeState.downgradedSteps)
        val downgraded = effectiveTier.ordinal < baseTier.ordinal
        val resolved = AdaptivePerformanceConfig.forTier(
            tier = effectiveTier,
            hardwareTier = hwTier,
            mode = profileMode,
            runtimeDowngraded = downgraded,
            downgradeReason = if (downgraded) runtimeState.downgradeReason else null,
        )
        _config.value = resolved
        onConfigChanged?.invoke(resolved)
        return resolved
    }

    fun readHardwareSnapshot(context: Context): HardwareSnapshot {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(512L)
        val availMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
        val glVersion = runCatching {
            val req = activityManager?.deviceConfigurationInfo?.reqGlEsVersion ?: 0x00020000
            req ushr 16
        }.getOrDefault(2)
        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { powerManager?.currentThermalStatus ?: 0 }.getOrDefault(0)
        } else {
            0
        }

        return HardwareSnapshot(
            totalRamMb = totalMb,
            availableRamMb = availMb,
            isLowRamDevice = activityManager?.isLowRamDevice == true || memInfo.lowMemory,
            memoryClassMb = activityManager?.memoryClass ?: 128,
            cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            is64BitAbi = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty(),
            apiLevel = Build.VERSION.SDK_INT,
            glEsMajorVersion = glVersion,
            isPowerSaveMode = powerManager?.isPowerSaveMode == true,
            thermalStatus = thermal,
        )
    }
}

/**
 * Lightweight foreground-only frame monitor that observes slow UI frames via [Choreographer]
 * and feeds [AdaptivePerformanceEngine.reportFrameDropPressure] without polling threads.
 */
class RuntimePerformanceMonitor(
    private val context: Context,
    private val expectedRefreshRateHz: () -> Float,
) : Choreographer.FrameCallback {

    private var active = false
    private var lastFrameNs = 0L
    private var windowStartMs = 0L
    private var slowFramesInWindow = 0

    fun start() {
        if (active) return
        active = true
        lastFrameNs = 0L
        windowStartMs = System.currentTimeMillis()
        slowFramesInWindow = 0
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stop() {
        if (!active) return
        active = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!active) return
        val nowMs = System.currentTimeMillis()
        if (lastFrameNs != 0L) {
            val deltaMs = (frameTimeNanos - lastFrameNs) / 1_000_000f
            val hz = expectedRefreshRateHz().coerceIn(50f, 240f)
            val frameBudgetMs = 1000f / hz
            val thresholdMs = maxOf(frameBudgetMs * SLOW_FRAME_MULTIPLIER, HARD_JANK_FRAME_MS.toFloat())
            // Ignore huge pauses (> 500ms) caused by app backgrounding/dialog transitions.
            if (deltaMs in thresholdMs..500f) {
                slowFramesInWindow++
            }
        }
        lastFrameNs = frameTimeNanos

        if (nowMs - windowStartMs >= FRAME_DROP_WINDOW_MS) {
            val hasJank = slowFramesInWindow >= FRAME_DROP_THRESHOLD_COUNT
            AdaptivePerformanceEngine.refreshSystemPressure(context, nowMs)
            AdaptivePerformanceEngine.reportFrameDropPressure(hasJank, nowMs)
            windowStartMs = nowMs
            slowFramesInWindow = 0
        }

        if (active) {
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
}
