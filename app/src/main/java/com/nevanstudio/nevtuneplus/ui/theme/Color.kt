// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.Hct

// Apple Music's signature red — kept for specific legacy references where needed.
val AccentRed = Color(0xFFFA2D48)

// Semantic status colors
val SuccessLight = Color(0xFF2E7D32)
val SuccessDark = Color(0xFF34C759)
val OnSuccessLight = Color(0xFFFFFFFF)
val OnSuccessDark = Color(0xFF04210C)
val SuccessContainerLight = Color(0xFFD7F5DD)
val SuccessContainerDark = Color(0xFF123B1E)

val WarningLight = Color(0xFFEF6C00)
val WarningDark = Color(0xFFFFB74D)
val OnWarningLight = Color(0xFFFFFFFF)
val OnWarningDark = Color(0xFF331B00)

/**
 * NevTune+ Default Material 3 Dark ColorScheme.
 * Every M3 role is explicitly assigned so no baseline Material purple leaks through.
 */
val DefaultDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF26262A),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFD1D1D6),
    onSecondary = Color(0xFF141416),
    secondaryContainer = Color(0xFF232327),
    onSecondaryContainer = Color(0xFFE5E5EA),
    tertiary = Color(0xFFAEAEB2),
    onTertiary = Color(0xFF141416),
    tertiaryContainer = Color(0xFF2C2C30),
    onTertiaryContainer = Color(0xFFF2F2F7),
    error = Color(0xFFFF6961),
    onError = Color(0xFF3B0807),
    errorContainer = Color(0xFF601410),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0D0D0F),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF8E8E93),
    surfaceTint = Color.White,
    outline = Color(0xFF2C2C2E),
    outlineVariant = Color(0xFF232326),
    scrim = Color.Black,
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF141416),
    inversePrimary = Color(0xFF1C1C1E),
    surfaceDim = Color(0xFF070709),
    surfaceBright = Color(0xFF232327),
    surfaceContainerLowest = Color(0xFF050507),
    surfaceContainerLow = Color(0xFF111114),
    surfaceContainer = Color(0xFF161619),
    surfaceContainerHigh = Color(0xFF1E1E22),
    surfaceContainerHighest = Color(0xFF28282C),
)

/**
 * NevTune+ Default Material 3 Light ColorScheme.
 * Every M3 role is explicitly assigned so no baseline Material purple leaks through.
 */
val DefaultLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5EA),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF3A3A3C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAEAF0),
    onSecondaryContainer = Color(0xFF1C1C1E),
    tertiary = Color(0xFF636366),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECECF2),
    onTertiaryContainer = Color(0xFF2C2C2E),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFDE7E9),
    onErrorContainer = Color(0xFF410E0B),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFF7F7F9),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF6E6E73),
    surfaceTint = Color.Black,
    outline = Color(0xFFE5E5EA),
    outlineVariant = Color(0xFFD1D1D6),
    scrim = Color.Black,
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color.White,
    inversePrimary = Color.White,
    surfaceDim = Color(0xFFECECF0),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F5F7),
    surfaceContainer = Color(0xFFF0F0F4),
    surfaceContainerHigh = Color(0xFFEAEAEF),
    surfaceContainerHighest = Color(0xFFE2E2E8),
)

@Immutable
data class ColorSchemePreset(
    val id: String,
    val displayName: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val style: DynamicPaletteStyle = DynamicPaletteStyle.TONAL_SPOT,
)

/**
 * Curated Material 3 presets adapted from NevPlay and tailored for NevTune+.
 */
val ThemeColorPresets: List<ColorSchemePreset> = listOf(
    ColorSchemePreset(
        id = "Default",
        displayName = "NevTune+ Mono",
        description = "Signature high-contrast monochrome",
        primaryColor = Color(0xFF1C1C1E),
        secondaryColor = Color(0xFF636366),
        tertiaryColor = Color(0xFFAEAEB2),
        style = DynamicPaletteStyle.MONOCHROME,
    ),
    ColorSchemePreset(
        id = "NevTuneGreen",
        displayName = "Pulse Green",
        description = "Vibrant studio emerald",
        primaryColor = Color(0xFF1DB954),
        secondaryColor = Color(0xFF1ED760),
        tertiaryColor = Color(0xFF14833B),
        style = DynamicPaletteStyle.VIBRANT,
    ),
    ColorSchemePreset(
        id = "Ocean",
        displayName = "Ocean Blue",
        description = "Deep sapphire & cool cyan",
        primaryColor = Color(0xFF0A84FF),
        secondaryColor = Color(0xFF5AC8FA),
        tertiaryColor = Color(0xFF5E5CE6),
        style = DynamicPaletteStyle.TONAL_SPOT,
    ),
    ColorSchemePreset(
        id = "SummerFun",
        displayName = "Summer Fun",
        description = "Warm sunset & ocean teal",
        primaryColor = Color(0xFFFB8500),
        secondaryColor = Color(0xFFFFB703),
        tertiaryColor = Color(0xFF219EBC),
        style = DynamicPaletteStyle.EXPRESSIVE,
    ),
    ColorSchemePreset(
        id = "Emerald",
        displayName = "Emerald",
        description = "Rich botanical green",
        primaryColor = Color(0xFF2E7D32),
        secondaryColor = Color(0xFF388E3C),
        tertiaryColor = Color(0xFF66BB6A),
        style = DynamicPaletteStyle.TONAL_SPOT,
    ),
    ColorSchemePreset(
        id = "Warm",
        displayName = "Sunset Warm",
        description = "Golden amber & coral",
        primaryColor = Color(0xFFFF6B35),
        secondaryColor = Color(0xFFF7931E),
        tertiaryColor = Color(0xFFFFC857),
        style = DynamicPaletteStyle.VIBRANT,
    ),
    ColorSchemePreset(
        id = "Forest",
        displayName = "Forest",
        description = "Earthy moss & sage",
        primaryColor = Color(0xFF33691E),
        secondaryColor = Color(0xFF558B2F),
        tertiaryColor = Color(0xFF9CCC65),
        style = DynamicPaletteStyle.TONAL_SPOT,
    ),
    ColorSchemePreset(
        id = "Rose",
        displayName = "Rose",
        description = "Vibrant magenta & blush",
        primaryColor = Color(0xFFE91E63),
        secondaryColor = Color(0xFFC2185B),
        tertiaryColor = Color(0xFFFF80AB),
        style = DynamicPaletteStyle.VIBRANT,
    ),
    ColorSchemePreset(
        id = "Lavender",
        displayName = "Lavender",
        description = "Soft electric violet",
        primaryColor = Color(0xFF7C4DFF),
        secondaryColor = Color(0xFF9575CD),
        tertiaryColor = Color(0xFFBA68C8),
        style = DynamicPaletteStyle.EXPRESSIVE,
    ),
    ColorSchemePreset(
        id = "Aurora",
        displayName = "Aurora",
        description = "Luminous mint & lime",
        primaryColor = Color(0xFF00C853),
        secondaryColor = Color(0xFF00E676),
        tertiaryColor = Color(0xFFDCE775),
        style = DynamicPaletteStyle.VIBRANT,
    ),
    ColorSchemePreset(
        id = "Amber",
        displayName = "Amber",
        description = "Honeyed gold & bronze",
        primaryColor = Color(0xFFFF8F00),
        secondaryColor = Color(0xFFFF6F00),
        tertiaryColor = Color(0xFFFFD54F),
        style = DynamicPaletteStyle.TONAL_SPOT,
    ),
    ColorSchemePreset(
        id = "Crimson",
        displayName = "Crimson",
        description = "Bold ruby & scarlet",
        primaryColor = Color(0xFFC62828),
        secondaryColor = Color(0xFFD32F2F),
        tertiaryColor = Color(0xFFFF5252),
        style = DynamicPaletteStyle.VIBRANT,
    ),
    ColorSchemePreset(
        id = "Mint",
        displayName = "Mint",
        description = "Crisp glacier teal",
        primaryColor = Color(0xFF0097A7),
        secondaryColor = Color(0xFF00ACC1),
        tertiaryColor = Color(0xFF4DD0E1),
        style = DynamicPaletteStyle.TONAL_SPOT,
    ),
    ColorSchemePreset(
        id = "Monochrome",
        displayName = "Slate Mono",
        description = "Minimalist neutral grayscale",
        primaryColor = Color(0xFF5C5C60),
        secondaryColor = Color(0xFF787880),
        tertiaryColor = Color(0xFF9E9EA4),
        style = DynamicPaletteStyle.MONOCHROME,
    ),
)

/**
 * Generates a full Material 3 [ColorScheme] for a preset name or a custom hex triplet
 * (`custom_RRGGBB_RRGGBB_RRGGBB`).
 */
fun getCustomColorScheme(schemeName: String, darkTheme: Boolean): ColorScheme {
    parseCustomColorScheme(schemeName, darkTheme)?.let { return it }

    if (schemeName == "Default") {
        return if (darkTheme) DefaultDarkColorScheme else DefaultLightColorScheme
    }

    val preset = ThemeColorPresets.firstOrNull { it.id == schemeName }
        ?: return if (darkTheme) DefaultDarkColorScheme else DefaultLightColorScheme

    return buildSchemeFromTriplet(
        primarySeed = preset.primaryColor,
        secondarySeed = preset.secondaryColor,
        tertiarySeed = preset.tertiaryColor,
        style = preset.style,
        darkTheme = darkTheme,
        preserveExactPrimary = false,
    )
}

/**
 * Parses `custom_<primaryHex>_<secondaryHex>_<tertiaryHex>` into a complete Material 3 [ColorScheme].
 */
fun parseCustomColorScheme(schemeName: String, darkTheme: Boolean): ColorScheme? {
    if (!schemeName.startsWith("custom_")) return null
    val parts = schemeName.split("_")
    if (parts.size != 4) return null

    return runCatching {
        val primary = Color(("FF" + parts[1].padStart(6, '0')).toLong(16))
        val secondary = Color(("FF" + parts[2].padStart(6, '0')).toLong(16))
        val tertiary = Color(("FF" + parts[3].padStart(6, '0')).toLong(16))
        buildSchemeFromTriplet(
            primarySeed = primary,
            secondarySeed = secondary,
            tertiarySeed = tertiary,
            style = DynamicPaletteStyle.CONTENT,
            darkTheme = darkTheme,
            preserveExactPrimary = true,
        )
    }.getOrNull()
}

private fun buildSchemeFromTriplet(
    primarySeed: Color,
    secondarySeed: Color,
    tertiarySeed: Color,
    style: DynamicPaletteStyle,
    darkTheme: Boolean,
    preserveExactPrimary: Boolean,
): ColorScheme {
    val primaryScheme = ColorExtractor.createDynamicScheme(
        sourceHct = Hct.fromInt(primarySeed.toArgb()),
        paletteStyle = style,
        isDark = darkTheme,
    )
    val secondaryScheme = ColorExtractor.createDynamicScheme(
        sourceHct = Hct.fromInt(secondarySeed.toArgb()),
        paletteStyle = style,
        isDark = darkTheme,
    )
    val tertiaryScheme = ColorExtractor.createDynamicScheme(
        sourceHct = Hct.fromInt(tertiarySeed.toArgb()),
        paletteStyle = style,
        isDark = darkTheme,
    )

    val base = primaryScheme.copy(
        secondary = secondaryScheme.primary,
        onSecondary = secondaryScheme.onPrimary,
        secondaryContainer = secondaryScheme.primaryContainer,
        onSecondaryContainer = secondaryScheme.onPrimaryContainer,
        tertiary = tertiaryScheme.primary,
        onTertiary = tertiaryScheme.onPrimary,
        tertiaryContainer = tertiaryScheme.primaryContainer,
        onTertiaryContainer = tertiaryScheme.onPrimaryContainer,
    )

    if (!preserveExactPrimary) return base

    // For user-picked custom colors, honor the exact chosen hues while keeping tonal containers legible.
    return base.copy(
        primary = primarySeed,
        onPrimary = contrastContentFor(primarySeed),
        secondary = secondarySeed,
        onSecondary = contrastContentFor(secondarySeed),
        tertiary = tertiarySeed,
        onTertiary = contrastContentFor(tertiarySeed),
        surfaceTint = primarySeed,
    )
}

private fun contrastContentFor(background: Color): Color =
    if (background.luminance() > 0.45f) Color.Black else Color.White

/**
 * Applies pitch-black AMOLED surfaces while keeping subtle elevation separation for cards & dialogs.
 */
fun ColorScheme.withAmoledBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF161618),
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF1C1C1E),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0C),
    surfaceContainer = Color(0xFF121214),
    surfaceContainerHigh = Color(0xFF1A1A1D),
    surfaceContainerHighest = Color(0xFF222226),
)
