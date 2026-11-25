// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Extended semantic color tokens beyond Material 3's standard [androidx.compose.material3.ColorScheme].
 * Adapted from NevPlay's ExtendedTheme for NevTune+ components.
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val playingAccent: Color,
    val onPlayingAccent: Color,
    val glassBorder: Color,
    val isDark: Boolean,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = SuccessDark,
        onSuccess = OnSuccessDark,
        successContainer = SuccessContainerDark,
        warning = WarningDark,
        onWarning = OnWarningDark,
        playingAccent = Color.White,
        onPlayingAccent = Color.Black,
        glassBorder = Color.White.copy(alpha = 0.10f),
        isDark = true,
    )
}

fun extendedColorsFor(darkTheme: Boolean, primary: Color, onPrimary: Color): ExtendedColors =
    if (darkTheme) {
        ExtendedColors(
            success = SuccessDark,
            onSuccess = OnSuccessDark,
            successContainer = SuccessContainerDark,
            warning = WarningDark,
            onWarning = OnWarningDark,
            playingAccent = primary,
            onPlayingAccent = onPrimary,
            glassBorder = Color.White.copy(alpha = 0.10f),
            isDark = true,
        )
    } else {
        ExtendedColors(
            success = SuccessLight,
            onSuccess = OnSuccessLight,
            successContainer = SuccessContainerLight,
            warning = WarningLight,
            onWarning = OnWarningLight,
            playingAccent = primary,
            onPlayingAccent = onPrimary,
            glassBorder = Color.Black.copy(alpha = 0.08f),
            isDark = false,
        )
    }

/**
 * Theme-aware semantic color accessors for NevTune+.
 */
object NevTuneColors {
    val current: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current

    val success: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current.success

    val warning: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current.warning

    val playingAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    /**
     * Contrast-safe accent for labels sitting on dark artwork scrims (such as hero cards).
     * Picks an inverse/light primary when the active theme's primary is too dark.
     */
    val scrimSafeAccent: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val scheme = MaterialTheme.colorScheme
            return when {
                scheme.primary.luminance() >= 0.25f -> scheme.primary
                scheme.inversePrimary.luminance() >= 0.25f -> scheme.inversePrimary
                else -> scheme.primaryContainer
            }
        }
}
