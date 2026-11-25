// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import com.nevanstudio.nevtuneplus.wear.R
import com.nevanstudio.nevtuneplus.wear.data.WearSettings

/**
 * SF Pro Display, as on the phone — subset to Latin for the watch (the phone's
 * files are 2.3 MB a weight; these are ~50 KB). Anything outside the subset, a
 * Hindi or Japanese title, falls back to the system face glyph by glyph.
 */
val SFPro = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.W400),
    Font(R.font.sf_pro_display_medium, FontWeight.W500),
    Font(R.font.sf_pro_display_semibold, FontWeight.W600),
    Font(R.font.sf_pro_display_bold, FontWeight.W700),
)

@Immutable
data class WearAccentPreset(
    val id: String,
    val label: String,
    val color: Color,
)

val DefaultWearAccent = Color(0xFFFA2D48)

val WearAccentPresets: List<WearAccentPreset> = listOf(
    WearAccentPreset("default", "Default", DefaultWearAccent),
    WearAccentPreset("album_art", "Album Art", DefaultWearAccent),
    WearAccentPreset("NevTuneGreen", "Pulse Green", Color(0xFF1DB954)),
    WearAccentPreset("Ocean", "Ocean Blue", Color(0xFF0A84FF)),
    WearAccentPreset("SummerFun", "Summer Fun", Color(0xFFFB8500)),
    WearAccentPreset("Emerald", "Emerald", Color(0xFF2E7D32)),
    WearAccentPreset("Warm", "Sunset Warm", Color(0xFFFF6B35)),
    WearAccentPreset("Forest", "Forest", Color(0xFF558B2F)),
    WearAccentPreset("Rose", "Rose", Color(0xFFE91E63)),
    WearAccentPreset("Lavender", "Lavender", Color(0xFF7C4DFF)),
    WearAccentPreset("Aurora", "Aurora", Color(0xFF00C853)),
    WearAccentPreset("Amber", "Amber", Color(0xFFFF8F00)),
    WearAccentPreset("Crimson", "Crimson", Color(0xFFE53935)),
    WearAccentPreset("Arctic", "Arctic", Color(0xFF00B8D4)),
    WearAccentPreset("MidnightPurple", "Midnight Purple", Color(0xFF9C27B0)),
    WearAccentPreset("Coral", "Coral", Color(0xFFFF5252)),
)

val LocalWearAccent = compositionLocalOf { DefaultWearAccent }

/** Dynamic accent colour for the watch UI; tracks [WearSettings.accentPreset] and playing artwork. */
val AccentRed: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalWearAccent.current

/** Secondary text on the mesh and on black: the phone's onSurfaceVariant. */
val Secondary = Color(0xFF8E8E93)

/** Grouped cells on black, iOS's secondarySystemBackground in dark. */
val Cell = Color(0xFF1C1C1E)

object WearType {
    val title = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W600, fontSize = 15.sp, letterSpacing = (-0.2).sp)
    val subtitle = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W400, fontSize = 13.sp, letterSpacing = (-0.1).sp)
    val header = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W700, fontSize = 17.sp, letterSpacing = (-0.3).sp)
    val row = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W500, fontSize = 14.sp, letterSpacing = (-0.15).sp)
    val rowSub = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W400, fontSize = 12.sp)
    val button = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W600, fontSize = 14.sp)
    val caption = TextStyle(fontFamily = SFPro, fontWeight = FontWeight.W500, fontSize = 11.sp, letterSpacing = 0.2.sp)
}

private fun Color.tunedForWatchAccent(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[1] = (hsl[1] * 1.25f).coerceIn(0.45f, 1f)
    hsl[2] = hsl[2].coerceIn(0.50f, 0.72f)
    return Color(ColorUtils.HSLToColor(hsl))
}

@Composable
fun NevTunePlusWearTheme(
    nowPlayingColors: List<Color> = emptyList(),
    content: @Composable () -> Unit,
) {
    val presetId by WearSettings.accentPreset.collectAsStateWithLifecycle()
    val oledBlack by WearSettings.oledBlack.collectAsStateWithLifecycle()

    val accentColor = remember(presetId, nowPlayingColors) {
        when (presetId) {
            "album_art" -> nowPlayingColors.firstOrNull()?.tunedForWatchAccent() ?: DefaultWearAccent
            "default", "" -> DefaultWearAccent
            else -> WearAccentPresets.firstOrNull { it.id == presetId }?.color ?: DefaultWearAccent
        }
    }

    val primaryColor = if (presetId == "default" || presetId.isBlank()) Color.White else accentColor
    val onPrimaryColor = if (primaryColor.luminance() > 0.5f) Color.Black else Color.White
    val backgroundColor = if (oledBlack) Color.Black else Color(0xFF0D0D0F)
    val surfaceContainerColor = if (oledBlack) Cell else Color(0xFF222226)

    CompositionLocalProvider(LocalWearAccent provides accentColor) {
        MaterialTheme(
            colorScheme = ColorScheme(
                primary = primaryColor,
                onPrimary = onPrimaryColor,
                secondary = accentColor,
                onSecondary = if (accentColor.luminance() > 0.5f) Color.Black else Color.White,
                background = backgroundColor,
                onBackground = Color.White,
                onSurface = Color.White,
                onSurfaceVariant = Secondary,
                surfaceContainer = surfaceContainerColor,
            ),
            typography = Typography(defaultFontFamily = SFPro),
            content = content,
        )
    }
}

@Composable
fun AudifyWearTheme(content: @Composable () -> Unit) = NevTunePlusWearTheme(content = content)
