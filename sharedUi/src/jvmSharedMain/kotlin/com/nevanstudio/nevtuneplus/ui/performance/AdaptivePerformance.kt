// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.performance

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * User-selectable performance profile in Settings.
 * [AUTO] selects the hardware-appropriate tier at startup and adapts at runtime.
 */
enum class PerformanceProfileMode(val label: String) {
    AUTO("Auto"),
    LITE("Lite"),
    BALANCED("Balanced"),
    HIGH_QUALITY("High Quality"),
}

/**
 * Effective device / runtime capability tier resolved by the Adaptive Performance Engine.
 */
enum class PerformanceTier(val label: String) {
    LOW_END("Lite"),
    MID_RANGE("Balanced"),
    HIGH_END("High Quality"),
}

/**
 * Centralized performance configuration consumed across UI, animations, image loading,
 * and background tasks so screens never scatter raw hardware checks.
 */
@Immutable
data class AdaptivePerformanceConfig(
    val tier: PerformanceTier,
    val hardwareTier: PerformanceTier,
    val mode: PerformanceProfileMode,
    val runtimeDowngraded: Boolean = false,
    val downgradeReason: String? = null,
    val reduceAnimations: Boolean,
    val disableContinuousMotion: Boolean,
    val disableBlurAndGlass: Boolean,
    val disableProgressiveEdgeBlur: Boolean,
    val disableMeshGradientBlur: Boolean,
    val simplifyCardsAndShadows: Boolean,
    val disableAnimatedCanvas: Boolean,
    val disableLyricsBlur: Boolean,
    val allowHighRefreshRate: Boolean,
    val imageCrossfadeMs: Int,
    val artworkDecodeMaxPx: Int,
    val imageMemoryCacheFraction: Double,
    val imageDiskCacheMb: Long,
    val useRgb565Artwork: Boolean,
    val preloadConcurrency: Int,
    val enableBackgroundPreload: Boolean,
) {
    companion object {
        fun forTier(
            tier: PerformanceTier,
            hardwareTier: PerformanceTier = tier,
            mode: PerformanceProfileMode = PerformanceProfileMode.AUTO,
            runtimeDowngraded: Boolean = false,
            downgradeReason: String? = null,
        ): AdaptivePerformanceConfig = when (tier) {
            PerformanceTier.LOW_END -> AdaptivePerformanceConfig(
                tier = tier,
                hardwareTier = hardwareTier,
                mode = mode,
                runtimeDowngraded = runtimeDowngraded,
                downgradeReason = downgradeReason,
                reduceAnimations = true,
                disableContinuousMotion = true,
                disableBlurAndGlass = true,
                disableProgressiveEdgeBlur = true,
                disableMeshGradientBlur = true,
                simplifyCardsAndShadows = true,
                disableAnimatedCanvas = true,
                disableLyricsBlur = true,
                allowHighRefreshRate = false,
                imageCrossfadeMs = 0,
                artworkDecodeMaxPx = 480,
                imageMemoryCacheFraction = 0.10,
                imageDiskCacheMb = 40L,
                useRgb565Artwork = true,
                preloadConcurrency = 1,
                enableBackgroundPreload = false,
            )
            PerformanceTier.MID_RANGE -> AdaptivePerformanceConfig(
                tier = tier,
                hardwareTier = hardwareTier,
                mode = mode,
                runtimeDowngraded = runtimeDowngraded,
                downgradeReason = downgradeReason,
                reduceAnimations = false,
                disableContinuousMotion = true,
                disableBlurAndGlass = false,
                disableProgressiveEdgeBlur = true,
                disableMeshGradientBlur = false,
                simplifyCardsAndShadows = false,
                disableAnimatedCanvas = false,
                disableLyricsBlur = false,
                allowHighRefreshRate = false,
                imageCrossfadeMs = 140,
                artworkDecodeMaxPx = 720,
                imageMemoryCacheFraction = 0.18,
                imageDiskCacheMb = 75L,
                useRgb565Artwork = false,
                preloadConcurrency = 2,
                enableBackgroundPreload = true,
            )
            PerformanceTier.HIGH_END -> AdaptivePerformanceConfig(
                tier = tier,
                hardwareTier = hardwareTier,
                mode = mode,
                runtimeDowngraded = runtimeDowngraded,
                downgradeReason = downgradeReason,
                reduceAnimations = false,
                disableContinuousMotion = false,
                disableBlurAndGlass = false,
                disableProgressiveEdgeBlur = false,
                disableMeshGradientBlur = false,
                simplifyCardsAndShadows = false,
                disableAnimatedCanvas = false,
                disableLyricsBlur = false,
                allowHighRefreshRate = true,
                imageCrossfadeMs = 210,
                artworkDecodeMaxPx = 1200,
                imageMemoryCacheFraction = 0.25,
                imageDiskCacheMb = 100L,
                useRgb565Artwork = false,
                preloadConcurrency = 4,
                enableBackgroundPreload = true,
            )
        }

        val BalancedDefault: AdaptivePerformanceConfig = forTier(PerformanceTier.MID_RANGE)
        val HighDefault: AdaptivePerformanceConfig = forTier(PerformanceTier.HIGH_END)
    }
}

val LocalAdaptivePerformance = staticCompositionLocalOf { AdaptivePerformanceConfig.BalancedDefault }
