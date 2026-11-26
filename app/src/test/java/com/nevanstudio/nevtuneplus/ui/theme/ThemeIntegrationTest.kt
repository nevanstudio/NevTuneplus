// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeIntegrationTest {

    @Test
    fun albumArtSchemeAdaptsContainersAndSecondaryTextEvenWithoutExactMode() {
        val vividBlue = Color(0xFF1565C0).toArgb()
        val vividCoral = Color(0xFFE53935).toArgb()

        val blueDark = ColorExtractor.albumArtColorScheme(vividBlue, darkTheme = true, useExactArtworkColors = false)
        val coralDark = ColorExtractor.albumArtColorScheme(vividCoral, darkTheme = true, useExactArtworkColors = false)

        assertNotEquals(blueDark.primary, coralDark.primary)
        assertNotEquals(blueDark.surfaceContainer, coralDark.surfaceContainer)
        assertNotEquals(blueDark.surfaceContainerHigh, coralDark.surfaceContainerHigh)
        assertNotEquals(blueDark.onSurfaceVariant, coralDark.onSurfaceVariant)
        assertNotEquals(blueDark.outlineVariant, coralDark.outlineVariant)
    }

    @Test
    fun allPresetSchemesMaintainReadableTextContrastInLightAndDarkModes() {
        for (preset in ThemeColorPresets) {
            for (dark in listOf(false, true)) {
                val scheme = getCustomColorScheme(preset.id, darkTheme = dark)
                val onSurfaceContrast = contrastRatio(scheme.onSurface, scheme.surface)
                val onContainerContrast = contrastRatio(scheme.onSurface, scheme.surfaceContainer)
                val secondaryContrast = contrastRatio(scheme.onSurfaceVariant, scheme.surfaceContainerLow)

                assertTrue("${preset.id} (dark=$dark) onSurface contrast too low: $onSurfaceContrast", onSurfaceContrast >= 7.0f)
                assertTrue("${preset.id} (dark=$dark) onContainer contrast too low: $onContainerContrast", onContainerContrast >= 6.0f)
                assertTrue("${preset.id} (dark=$dark) onSurfaceVariant contrast too low: $secondaryContrast", secondaryContrast >= 4.0f)
            }
        }
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val l1 = foreground.luminance()
        val l2 = background.luminance()
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
