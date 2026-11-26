// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptivePerformanceConfigTest {

    @Test
    fun lowEndTierEnablesLiteOptimizationsAndDisablesExpensiveEffects() {
        val lite = AdaptivePerformanceConfig.forTier(PerformanceTier.LOW_END)

        assertTrue(lite.reduceAnimations)
        assertTrue(lite.disableContinuousMotion)
        assertTrue(lite.disableBlurAndGlass)
        assertTrue(lite.disableProgressiveEdgeBlur)
        assertTrue(lite.disableMeshGradientBlur)
        assertTrue(lite.simplifyCardsAndShadows)
        assertTrue(lite.disableAnimatedCanvas)
        assertTrue(lite.disableLyricsBlur)
        assertFalse(lite.allowHighRefreshRate)
        assertEquals(0, lite.imageCrossfadeMs)
        assertTrue(lite.useRgb565Artwork)
        assertFalse(lite.enableBackgroundPreload)
        assertEquals(1, lite.preloadConcurrency)
        assertTrue(lite.imageMemoryCacheFraction <= 0.12)
        assertTrue(lite.imageDiskCacheMb <= 48L)
    }

    @Test
    fun midRangeTierBalancesEffectsAndProtectsContinuousWork() {
        val balanced = AdaptivePerformanceConfig.forTier(PerformanceTier.MID_RANGE)

        assertFalse(balanced.reduceAnimations)
        assertTrue(balanced.disableContinuousMotion)
        assertFalse(balanced.disableBlurAndGlass)
        assertTrue(balanced.disableProgressiveEdgeBlur)
        assertFalse(balanced.disableMeshGradientBlur)
        assertFalse(balanced.simplifyCardsAndShadows)
        assertFalse(balanced.disableAnimatedCanvas)
        assertFalse(balanced.allowHighRefreshRate)
        assertFalse(balanced.useRgb565Artwork)
        assertTrue(balanced.enableBackgroundPreload)
        assertEquals(2, balanced.preloadConcurrency)
        assertTrue(balanced.imageMemoryCacheFraction in 0.15..0.20)
    }

    @Test
    fun highEndTierEnablesFullVisualExperience() {
        val high = AdaptivePerformanceConfig.forTier(PerformanceTier.HIGH_END)

        assertFalse(high.reduceAnimations)
        assertFalse(high.disableContinuousMotion)
        assertFalse(high.disableBlurAndGlass)
        assertFalse(high.disableProgressiveEdgeBlur)
        assertFalse(high.disableMeshGradientBlur)
        assertFalse(high.simplifyCardsAndShadows)
        assertFalse(high.disableAnimatedCanvas)
        assertFalse(high.disableLyricsBlur)
        assertTrue(high.allowHighRefreshRate)
        assertFalse(high.useRgb565Artwork)
        assertTrue(high.imageCrossfadeMs > 0)
        assertTrue(high.enableBackgroundPreload)
        assertEquals(4, high.preloadConcurrency)
        assertEquals(0.25, high.imageMemoryCacheFraction, 0.0)
    }
}
