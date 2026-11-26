package com.nevanstudio.nevtuneplus

import com.nevanstudio.nevtuneplus.ui.performance.AdaptivePerformanceConfig
import com.nevanstudio.nevtuneplus.ui.performance.PerformanceTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCacheConfigurationTest {
    @Test
    fun normalDevicesUseTwentyFivePercentOfAvailableHeap() {
        assertEquals(0.25, imageMemoryCacheHeapFraction(isLowRamDevice = false), 0.0)
    }

    @Test
    fun lowRamDevicesUseReducedFifteenPercentHeapBudget() {
        assertEquals(0.15, imageMemoryCacheHeapFraction(isLowRamDevice = true), 0.0)
    }

    @Test
    fun adaptiveConfigsScaleImageMemoryAndDiskCacheByTier() {
        val low = AdaptivePerformanceConfig.forTier(PerformanceTier.LOW_END)
        val mid = AdaptivePerformanceConfig.forTier(PerformanceTier.MID_RANGE)
        val high = AdaptivePerformanceConfig.forTier(PerformanceTier.HIGH_END)

        assertTrue(imageMemoryCacheHeapFraction(low) < imageMemoryCacheHeapFraction(mid))
        assertTrue(imageMemoryCacheHeapFraction(mid) < imageMemoryCacheHeapFraction(high))
        assertTrue(imageDiskCacheMaxBytes(low) < imageDiskCacheMaxBytes(mid))
        assertTrue(imageDiskCacheMaxBytes(mid) <= imageDiskCacheMaxBytes(high))
    }
}
