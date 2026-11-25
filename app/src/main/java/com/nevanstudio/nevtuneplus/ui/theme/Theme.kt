// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nevanstudio.nevtuneplus.R
import com.nevanstudio.nevtuneplus.data.settings.AppFontChoice
import com.nevanstudio.nevtuneplus.data.settings.AppSettings
import com.nevanstudio.nevtuneplus.data.settings.ThemeColorSource
import com.nevanstudio.nevtuneplus.data.settings.ThemeMode

/**
 * Geom font family — the default primary typeface across the whole app for NevTune+.
 * Includes all weights from light to black.
 */
val GeomFont = FontFamily(
    Font(R.font.geom_light, FontWeight.W300),
    Font(R.font.geom_regular, FontWeight.W400),
    Font(R.font.geom_medium, FontWeight.W500),
    Font(R.font.geom_semibold, FontWeight.W600),
    Font(R.font.geom_bold, FontWeight.W700),
    Font(R.font.geom_extrabold, FontWeight.W800),
    Font(R.font.geom_black, FontWeight.W900),
)

val SFProDisplay = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.W400),
    Font(R.font.sf_pro_display_medium, FontWeight.W500),
    Font(R.font.sf_pro_display_semibold, FontWeight.W600),
    Font(R.font.sf_pro_display_bold, FontWeight.W700),
    Font(R.font.sf_pro_display_heavy, FontWeight.W800),
)

// Heavy, tight typography — the backbone of the NevTune+ look.
private val AudifyTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 34.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 30.sp, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.W700, fontSize = 22.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.W700, fontSize = 20.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 16.sp, letterSpacing = (-0.2).sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.W400, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.W400, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.W600, fontSize = 11.sp),
)

/** Applies [family] to every style in the scale, so nothing is left on Roboto. */
private fun Typography.withFamily(family: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

/** Resolves whether dark mode is active for a given [ThemeMode]. */
@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * Centralized Material 3 theme for NevTune+.
 *
 * Dynamically resolves color schemes from:
 * 1. Playing track's album artwork ([ThemeColorSource.ALBUM_ART])
 * 2. Android 12+ Material You wallpaper colors ([ThemeColorSource.MONET])
 * 3. Curated presets or user-crafted RGB/HSL schemes ([ThemeColorSource.CUSTOM])
 *
 * Updates reactively without restarting the Activity.
 */
@Composable
fun NevTunePlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledTheme: Boolean? = null,
    colorSource: ThemeColorSource? = null,
    customColorScheme: String? = null,
    useExactArtworkColors: Boolean? = null,
    extractedAlbumSeedColor: Int? = null,
    windowWidth: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val baseDensity = LocalDensity.current

    val settingsAmoled by AppSettings.amoledTheme.collectAsStateWithLifecycle()
    val settingsSource by AppSettings.colorSource.collectAsStateWithLifecycle()
    val settingsScheme by AppSettings.customColorScheme.collectAsStateWithLifecycle()
    val settingsExactArt by AppSettings.useExactArtworkColors.collectAsStateWithLifecycle()
    val settingsAlbumSeed by AppSettings.extractedAlbumSeedColor.collectAsStateWithLifecycle()
    val fontChoice by AppSettings.fontChoice.collectAsStateWithLifecycle()

    val effectiveAmoled = amoledTheme ?: settingsAmoled
    val effectiveSource = colorSource ?: settingsSource
    val effectiveScheme = customColorScheme ?: settingsScheme
    val effectiveExactArt = useExactArtworkColors ?: settingsExactArt
    val effectiveAlbumSeed = extractedAlbumSeedColor ?: settingsAlbumSeed

    val colorScheme: ColorScheme = remember(
        context,
        darkTheme,
        effectiveAmoled,
        effectiveSource,
        effectiveScheme,
        effectiveExactArt,
        effectiveAlbumSeed,
    ) {
        val baseScheme = when {
            effectiveSource == ThemeColorSource.ALBUM_ART && effectiveAlbumSeed != null -> {
                ColorExtractor.albumArtColorScheme(
                    seedArgb = effectiveAlbumSeed,
                    darkTheme = darkTheme,
                    useExactArtworkColors = effectiveExactArt,
                )
            }
            effectiveSource == ThemeColorSource.MONET && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            else -> {
                getCustomColorScheme(effectiveScheme, darkTheme)
            }
        }
        if (effectiveAmoled && darkTheme) baseScheme.withAmoledBlack() else baseScheme
    }

    val extendedColors = remember(darkTheme, colorScheme.primary, colorScheme.onPrimary) {
        extendedColorsFor(darkTheme, colorScheme.primary, colorScheme.onPrimary)
    }

    val selectedFont = when (fontChoice) {
        AppFontChoice.AUDIFY -> GeomFont
        AppFontChoice.SF_PRO -> SFProDisplay
        AppFontChoice.SYSTEM -> FontFamily.Default
        AppFontChoice.SERIF -> FontFamily.Serif
    }

    val adaptiveFontScale = when {
        windowWidth >= 840.dp -> 1.24f
        windowWidth >= 600.dp -> 1.18f
        windowWidth >= 480.dp -> 1.12f
        windowWidth >= 360.dp -> 1.06f
        else -> 1f
    }

    val typography = remember(selectedFont) {
        AudifyTypography.withFamily(selectedFont)
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalDensity provides Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * adaptiveFontScale,
        ),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = NevTuneShapes,
            content = content,
        )
    }
}

/**
 * Draws the status and navigation bar glyphs dark or light.
 *
 * `enableEdgeToEdge()` decides this from the *system* dark-mode setting, which
 * is the wrong input the moment the in-app theme disagrees with it: Light theme
 * on a phone in dark mode left white icons on a white bar, invisible. The bars
 * have to follow the content the app is actually painting. The player supplies
 * its own stable light-icon value and paints contrast behind it; every other
 * surface follows the theme. Hence a parameter rather than reading it here.
 */
@Composable
fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = findWindow(view) ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = dark
            isAppearanceLightNavigationBars = dark
        }
    }
}

@Composable
fun NevTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    windowWidth: Dp = 0.dp,
    content: @Composable () -> Unit,
) = NevTunePlusTheme(darkTheme = darkTheme, windowWidth = windowWidth, content = content)
