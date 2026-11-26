// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.performance

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptivePerformanceEngineTest {

    private fun snapshot(
        totalRamMb: Long,
        availableRamMb: Long = totalRamMb / 2,
        isLowRamDevice: Boolean = false,
        memoryClassMb: Int = 256,
        cpuCores: Int = 8,
        is64BitAbi: Boolean = true,
        apiLevel: Int = Build.VERSION_CODES.TIRAMISU,
        glEsMajorVersion: Int = 3,
        isPowerSaveMode: Boolean = false,
        thermalStatus: Int = 0,
    ) = HardwareSnapshot(
        totalRamMb = totalRamMb,
        availableRamMb = availableRamMb,
        isLowRamDevice = isLowRamDevice,
        memoryClassMb = memoryClassMb,
        cpuCores = cpuCores,
        is64BitAbi = is64BitAbi,
        apiLevel = apiLevel,
        glEsMajorVersion = glEsMajorVersion,
        isPowerSaveMode = isPowerSaveMode,
        thermalStatus = thermalStatus,
    )

    @Test
    fun classifiesVeryLowEndOneToTwoGbDevicesAsLowEnd() {
        val androidGoOneGb = snapshot(
            totalRamMb = 1024,
            availableRamMb = 220,
            isLowRamDevice = true,
            memoryClassMb = 96,
            cpuCores = 4,
            is64BitAbi = false,
            apiLevel = Build.VERSION_CODES.O_MR1,
        )
        val dualCoreLegacy = snapshot(
            totalRamMb = 2048,
            availableRamMb = 500,
            cpuCores = 2,
            is64BitAbi = false,
            apiLevel = Build.VERSION_CODES.P,
        )
        assertEquals(PerformanceTier.LOW_END, classifyDeviceHardware(androidGoOneGb))
        assertEquals(PerformanceTier.LOW_END, classifyDeviceHardware(dualCoreLegacy))
    }

    @Test
    fun classifiesTwoGbRamDeviceAsLowEndRegardlessOfCpuCores() {
        val twoGbQuad = snapshot(
            totalRamMb = 2000,
            availableRamMb = 900,
            cpuCores = 8,
            is64BitAbi = true,
            apiLevel = Build.VERSION_CODES.R,
        )
        assertEquals(PerformanceTier.LOW_END, classifyDeviceHardware(twoGbQuad))
    }

    @Test
    fun usesMultiSignalClassificationNotRamAlone() {
        val threeGb32BitOld = snapshot(
            totalRamMb = 3072,
            cpuCores = 4,
            is64BitAbi = false,
            apiLevel = Build.VERSION_CODES.O,
        )
        assertEquals(PerformanceTier.LOW_END, classifyDeviceHardware(threeGb32BitOld))

        val sixGbWeakCpu32Bit = snapshot(
            totalRamMb = 6144,
            cpuCores = 4,
            is64BitAbi = false,
            apiLevel = Build.VERSION_CODES.R,
        )
        // Not HIGH_END because 32-bit + 4 cores fail multi-signal flagship gates.
        assertEquals(PerformanceTier.MID_RANGE, classifyDeviceHardware(sixGbWeakCpu32Bit))
    }

    @Test
    fun classifiesMidRangeThreeToFourGbDevices() {
        val fourGbMid = snapshot(
            totalRamMb = 4096,
            availableRamMb = 1600,
            cpuCores = 8,
            is64BitAbi = true,
            apiLevel = Build.VERSION_CODES.S,
        )
        assertEquals(PerformanceTier.MID_RANGE, classifyDeviceHardware(fourGbMid))
    }

    @Test
    fun classifiesHighEndSixToTwelveGbDevices() {
        val sixGbFlagship = snapshot(
            totalRamMb = 6144,
            availableRamMb = 2800,
            cpuCores = 8,
            is64BitAbi = true,
            apiLevel = Build.VERSION_CODES.TIRAMISU,
        )
        val twelveGbUltra = snapshot(
            totalRamMb = 12288,
            availableRamMb = 6400,
            cpuCores = 8,
            is64BitAbi = true,
            apiLevel = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        )
        assertEquals(PerformanceTier.HIGH_END, classifyDeviceHardware(sixGbFlagship))
        assertEquals(PerformanceTier.HIGH_END, classifyDeviceHardware(twelveGbUltra))
    }

    @Test
    fun runtimeAdaptationRespectsCooldownAndRecoveryHysteresis() {
        val pressure = RuntimePressureSignals(frameDropPressure = true)
        val clear = RuntimePressureSignals()

        // First pressure event immediately steps HIGH_END down 1 step -> MID_RANGE.
        val s1 = evaluateRuntimeAdaptation(
            current = RuntimeAdaptationState(),
            signals = pressure,
            baseTier = PerformanceTier.HIGH_END,
            nowMs = 1_000L,
        )
        assertEquals(1, s1.downgradedSteps)
        assertEquals(PerformanceTier.MID_RANGE, resolveEffectiveTier(PerformanceTier.HIGH_END, s1.downgradedSteps))

        // Pressure inside cooldown does NOT double-step to LOW_END immediately.
        val s2 = evaluateRuntimeAdaptation(
            current = s1,
            signals = RuntimePressureSignals(memoryPressure = true),
            baseTier = PerformanceTier.HIGH_END,
            nowMs = 10_000L,
        )
        assertEquals(1, s2.downgradedSteps)

        // After cooldown elapses, sustained pressure steps down to LOW_END.
        val s3 = evaluateRuntimeAdaptation(
            current = s2,
            signals = RuntimePressureSignals(memoryPressure = true),
            baseTier = PerformanceTier.HIGH_END,
            nowMs = 1_000L + RUNTIME_DOWNGRADE_COOLDOWN_MS + 500L,
        )
        assertEquals(2, s3.downgradedSteps)
        assertEquals(PerformanceTier.LOW_END, resolveEffectiveTier(PerformanceTier.HIGH_END, s3.downgradedSteps))

        // Brief clear period before recovery hold must NOT flap upward.
        val tooEarlyRecovery = evaluateRuntimeAdaptation(
            current = s3,
            signals = clear,
            baseTier = PerformanceTier.HIGH_END,
            nowMs = s3.lastPressureAtMs + 10_000L,
        )
        assertEquals(2, tooEarlyRecovery.downgradedSteps)

        // Sustained stability recovers one step at a time.
        val recoveredOne = evaluateRuntimeAdaptation(
            current = tooEarlyRecovery,
            signals = clear,
            baseTier = PerformanceTier.HIGH_END,
            nowMs = tooEarlyRecovery.lastPressureAtMs + RUNTIME_RECOVERY_HOLD_MS + 100L,
        )
        assertEquals(1, recoveredOne.downgradedSteps)

        val recoveredAll = evaluateRuntimeAdaptation(
            current = recoveredOne,
            signals = clear,
            baseTier = PerformanceTier.HIGH_END,
            nowMs = recoveredOne.lastPressureAtMs + RUNTIME_RECOVERY_HOLD_MS + 100L,
        )
        assertEquals(0, recoveredAll.downgradedSteps)
        assertNull(recoveredAll.downgradeReason)
        assertEquals(PerformanceTier.HIGH_END, resolveEffectiveTier(PerformanceTier.HIGH_END, recoveredAll.downgradedSteps))
    }

    @Test
    fun demandingOverrideWarningTriggersOnlyWhenSelectedModeExceedsHardware() {
        assertFalse(isProfileDemandingForHardware(PerformanceProfileMode.AUTO, PerformanceTier.LOW_END))
        assertFalse(isProfileDemandingForHardware(PerformanceProfileMode.LITE, PerformanceTier.LOW_END))
        assertTrue(isProfileDemandingForHardware(PerformanceProfileMode.BALANCED, PerformanceTier.LOW_END))
        assertTrue(isProfileDemandingForHardware(PerformanceProfileMode.HIGH_QUALITY, PerformanceTier.LOW_END))
        assertTrue(isProfileDemandingForHardware(PerformanceProfileMode.HIGH_QUALITY, PerformanceTier.MID_RANGE))
        assertFalse(isProfileDemandingForHardware(PerformanceProfileMode.HIGH_QUALITY, PerformanceTier.HIGH_END))
    }
}
