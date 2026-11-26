// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MathUtils
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.SchemeContent
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFruitSalad
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant
import com.nevanstudio.nevtuneplus.data.model.CARD_ART_PX
import com.nevanstudio.nevtuneplus.data.model.artworkAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Color scoring configuration for album art seed extraction.
 * Adapted from NevPlay's ColorExtractor for NevTune+.
 */
data class ColorScoringConfig(
    val targetChroma: Double = 30.0,
    val weightProportion: Double = 0.7,
    val weightChromaAbove: Double = 0.3,
    val weightChromaBelow: Double = 0.1,
    val cutoffChroma: Double = 5.0,
    val cutoffExcitedProportion: Double = 0.01,
    val maxColorCount: Int = 4,
    val maxHueDifference: Int = 90,
    val minHueDifference: Int = 15,
)

data class ColorExtractionConfig(
    val downscaleMaxDimension: Int = 128,
    val quantizerMaxColors: Int = 128,
    val scoring: ColorScoringConfig = ColorScoringConfig(),
)

private data class ScoredHct(
    val hct: Hct,
    val score: Double,
)

enum class DynamicPaletteStyle {
    TONAL_SPOT,
    VIBRANT,
    EXPRESSIVE,
    FRUIT_SALAD,
    CONTENT,
    MONOCHROME,
}

private const val GRAYSCALE_CHROMA_THRESHOLD = 12.0
private const val MAX_GRAYSCALE_CHANNEL_DELTA = 10
private const val DEFAULT_SEED_ARGB = 0xFF1DB954.toInt()

/**
 * Utility object for extracting ranked Material 3 seed colors from album artwork
 * and generating complete tonal Material 3 [ColorScheme] instances via HCT.
 */
object ColorExtractor {

    private val urlSeedCache = LruCache<String, Int>(64)

    /**
     * Fetches the artwork at [imageUrl] using Coil 3 and extracts its ranked ARGB seed color.
     */
    suspend fun extractSeedFromUrl(
        context: Context,
        imageUrl: String?,
        artPx: Int = CARD_ART_PX,
    ): Int? {
        if (imageUrl.isNullOrBlank()) return null
        urlSeedCache.get(imageUrl)?.let { return it }

        val request = ImageRequest.Builder(context)
            .data(imageUrl.artworkAt(artPx))
            .size(128)
            .allowHardware(false)
            .build()
        val result = runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull()
        val bitmap = (result as? SuccessResult)?.image?.toBitmap() ?: return null
        val seed = extractSeedFromBitmap(bitmap)
        urlSeedCache.put(imageUrl, seed)
        return seed
    }

    /**
     * Extracts a Material 3 seed ARGB color from [bitmap] using QuantizerCelebi + HCT scoring,
     * falling back to AndroidX Palette if needed.
     */
    suspend fun extractSeedFromBitmap(
        bitmap: Bitmap?,
        config: ColorExtractionConfig = ColorExtractionConfig(),
    ): Int = withContext(Dispatchers.Default) {
        if (bitmap == null) return@withContext DEFAULT_SEED_ARGB
        val working = resizeForExtraction(bitmap, config.downscaleMaxDimension)
        try {
            runCatching {
                val pixels = IntArray(working.width * working.height)
                working.getPixels(pixels, 0, working.width, 0, 0, working.width, working.height)
                val fallbackArgb = averageColorArgb(pixels)
                val quantized = QuantizerCelebi.quantize(pixels, config.quantizerMaxColors)
                val ranked = scoreQuantizedColors(quantized, config.scoring, fallbackArgb)
                ranked.firstOrNull() ?: fallbackArgb
            }.getOrElse {
                runCatching {
                    val palette = Palette.from(working).generate()
                    palette.getVibrantColor(palette.getDominantColor(DEFAULT_SEED_ARGB))
                }.getOrDefault(DEFAULT_SEED_ARGB)
            }
        } finally {
            if (working !== bitmap && !working.isRecycled) {
                working.recycle()
            }
        }
    }

    /**
     * Creates a complete Material 3 [ColorScheme] from an HCT seed color and [paletteStyle].
     */
    fun createDynamicScheme(
        sourceHct: Hct,
        paletteStyle: DynamicPaletteStyle,
        isDark: Boolean,
        contrastLevel: Double = 0.0,
    ): ColorScheme {
        val scheme = when (paletteStyle) {
            DynamicPaletteStyle.TONAL_SPOT -> SchemeTonalSpot(sourceHct, isDark, contrastLevel)
            DynamicPaletteStyle.VIBRANT -> SchemeVibrant(sourceHct, isDark, contrastLevel)
            DynamicPaletteStyle.EXPRESSIVE -> SchemeExpressive(sourceHct, isDark, contrastLevel)
            DynamicPaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(sourceHct, isDark, contrastLevel)
            DynamicPaletteStyle.CONTENT -> SchemeContent(sourceHct, isDark, contrastLevel)
            DynamicPaletteStyle.MONOCHROME -> SchemeMonochrome(sourceHct, isDark, contrastLevel)
        }

        return ColorScheme(
            primary = Color(scheme.primary),
            onPrimary = Color(scheme.onPrimary),
            primaryContainer = Color(scheme.primaryContainer),
            onPrimaryContainer = Color(scheme.onPrimaryContainer),
            inversePrimary = Color(scheme.inversePrimary),
            secondary = Color(scheme.secondary),
            onSecondary = Color(scheme.onSecondary),
            secondaryContainer = Color(scheme.secondaryContainer),
            onSecondaryContainer = Color(scheme.onSecondaryContainer),
            tertiary = Color(scheme.tertiary),
            onTertiary = Color(scheme.onTertiary),
            tertiaryContainer = Color(scheme.tertiaryContainer),
            onTertiaryContainer = Color(scheme.onTertiaryContainer),
            background = Color(scheme.background),
            onBackground = Color(scheme.onBackground),
            surface = Color(scheme.surface),
            onSurface = Color(scheme.onSurface),
            surfaceVariant = Color(scheme.surfaceVariant),
            onSurfaceVariant = Color(scheme.onSurfaceVariant),
            surfaceTint = Color(scheme.primary),
            inverseSurface = Color(scheme.inverseSurface),
            inverseOnSurface = Color(scheme.inverseOnSurface),
            error = Color(scheme.error),
            onError = Color(scheme.onError),
            errorContainer = Color(scheme.errorContainer),
            onErrorContainer = Color(scheme.onErrorContainer),
            outline = Color(scheme.outline),
            outlineVariant = Color(scheme.outlineVariant),
            scrim = Color(scheme.scrim),
            surfaceBright = Color(scheme.surfaceBright),
            surfaceDim = Color(scheme.surfaceDim),
            surfaceContainer = Color(scheme.surfaceContainer),
            surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
            surfaceContainerHighest = Color(scheme.surfaceContainerHighest),
            surfaceContainerLow = Color(scheme.surfaceContainerLow),
            surfaceContainerLowest = Color(scheme.surfaceContainerLowest),
            primaryFixed = Color(scheme.primaryFixed),
            primaryFixedDim = Color(scheme.primaryFixedDim),
            onPrimaryFixed = Color(scheme.onPrimaryFixed),
            onPrimaryFixedVariant = Color(scheme.onPrimaryFixedVariant),
            secondaryFixed = Color(scheme.secondaryFixed),
            secondaryFixedDim = Color(scheme.secondaryFixedDim),
            onSecondaryFixed = Color(scheme.onSecondaryFixed),
            onSecondaryFixedVariant = Color(scheme.onSecondaryFixedVariant),
            tertiaryFixed = Color(scheme.tertiaryFixed),
            tertiaryFixedDim = Color(scheme.tertiaryFixedDim),
            onTertiaryFixed = Color(scheme.onTertiaryFixed),
            onTertiaryFixedVariant = Color(scheme.onTertiaryFixedVariant),
        )
    }

    /**
     * Builds an album-art-derived [ColorScheme] from [seedArgb].
     * When [useExactArtworkColors] is true, backgrounds and surfaces also take on the artwork's tonal hue.
     * When false, the accents are derived dynamically while keeping NevTune+'s clean neutral surfaces.
     */
    fun albumArtColorScheme(
        seedArgb: Int,
        darkTheme: Boolean,
        useExactArtworkColors: Boolean,
    ): ColorScheme {
        val sourceHct = Hct.fromInt(seedArgb)
        val isNeutral = isNeutralSeed(seedArgb, sourceHct)
        val style = when {
            isNeutral -> DynamicPaletteStyle.MONOCHROME
            useExactArtworkColors && sourceHct.chroma > 12.0 -> DynamicPaletteStyle.CONTENT
            sourceHct.chroma > 45.0 -> DynamicPaletteStyle.VIBRANT
            sourceHct.chroma > 18.0 -> DynamicPaletteStyle.EXPRESSIVE
            else -> DynamicPaletteStyle.TONAL_SPOT
        }
        val dynamic = createDynamicScheme(sourceHct, style, darkTheme)
        if (useExactArtworkColors) return dynamic

        val base = if (darkTheme) DefaultDarkColorScheme else DefaultLightColorScheme
        return base.copy(
            primary = dynamic.primary,
            onPrimary = dynamic.onPrimary,
            primaryContainer = dynamic.primaryContainer,
            onPrimaryContainer = dynamic.onPrimaryContainer,
            inversePrimary = dynamic.inversePrimary,
            secondary = dynamic.secondary,
            onSecondary = dynamic.onSecondary,
            secondaryContainer = dynamic.secondaryContainer,
            onSecondaryContainer = dynamic.onSecondaryContainer,
            tertiary = dynamic.tertiary,
            onTertiary = dynamic.onTertiary,
            tertiaryContainer = dynamic.tertiaryContainer,
            onTertiaryContainer = dynamic.onTertiaryContainer,
            surfaceVariant = lerp(base.surfaceVariant, dynamic.surfaceVariant, 0.28f),
            onSurfaceVariant = lerp(base.onSurfaceVariant, dynamic.onSurfaceVariant, 0.35f),
            outline = lerp(base.outline, dynamic.outline, 0.30f),
            outlineVariant = lerp(base.outlineVariant, dynamic.outlineVariant, 0.30f),
            surfaceContainerLowest = lerp(base.surfaceContainerLowest, dynamic.surfaceContainerLowest, 0.16f),
            surfaceContainerLow = lerp(base.surfaceContainerLow, dynamic.surfaceContainerLow, 0.24f),
            surfaceContainer = lerp(base.surfaceContainer, dynamic.surfaceContainer, 0.28f),
            surfaceContainerHigh = lerp(base.surfaceContainerHigh, dynamic.surfaceContainerHigh, 0.32f),
            surfaceContainerHighest = lerp(base.surfaceContainerHighest, dynamic.surfaceContainerHighest, 0.34f),
            surfaceTint = dynamic.primary,
            primaryFixed = dynamic.primaryFixed,
            primaryFixedDim = dynamic.primaryFixedDim,
            onPrimaryFixed = dynamic.onPrimaryFixed,
            onPrimaryFixedVariant = dynamic.onPrimaryFixedVariant,
            secondaryFixed = dynamic.secondaryFixed,
            secondaryFixedDim = dynamic.secondaryFixedDim,
            onSecondaryFixed = dynamic.onSecondaryFixed,
            onSecondaryFixedVariant = dynamic.onSecondaryFixedVariant,
            tertiaryFixed = dynamic.tertiaryFixed,
            tertiaryFixedDim = dynamic.tertiaryFixedDim,
            onTertiaryFixed = dynamic.onTertiaryFixed,
            onTertiaryFixedVariant = dynamic.onTertiaryFixedVariant,
        )
    }

    private fun isNeutralSeed(argb: Int, sourceHct: Hct): Boolean {
        if (sourceHct.chroma > GRAYSCALE_CHROMA_THRESHOLD) return false
        val red = (argb ushr 16) and 0xFF
        val green = (argb ushr 8) and 0xFF
        val blue = argb and 0xFF
        return maxOf(abs(red - green), abs(green - blue), abs(red - blue)) <= MAX_GRAYSCALE_CHANNEL_DELTA
    }

    private fun resizeForExtraction(bitmap: Bitmap, maxDimension: Int): Bitmap {
        if (maxDimension <= 0) return bitmap
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / max(bitmap.width, bitmap.height).toFloat()
        val newWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun scoreQuantizedColors(
        colorsToPopulation: Map<Int, Int>,
        scoring: ColorScoringConfig,
        fallbackColorArgb: Int,
    ): List<Int> {
        if (colorsToPopulation.isEmpty()) return listOf(fallbackColorArgb)

        val colorsHct = ArrayList<Hct>(colorsToPopulation.size)
        val huePopulation = IntArray(360)
        var populationSum = 0.0

        for ((argb, population) in colorsToPopulation) {
            if (population <= 0) continue
            val hct = Hct.fromInt(argb)
            colorsHct.add(hct)
            val hue = MathUtils.sanitizeDegreesInt(hct.hue.roundToInt())
            huePopulation[hue] += population
            populationSum += population.toDouble()
        }

        if (populationSum <= 0.0) return listOf(fallbackColorArgb)

        val hueExcitedProportions = DoubleArray(360)
        for (hue in 0 until 360) {
            val proportion = huePopulation[hue] / populationSum
            for (neighbor in hue - 14..hue + 15) {
                val wrappedHue = MathUtils.sanitizeDegreesInt(neighbor)
                hueExcitedProportions[wrappedHue] += proportion
            }
        }

        val scoredColors = ArrayList<ScoredHct>(colorsHct.size)
        for (hct in colorsHct) {
            val hue = MathUtils.sanitizeDegreesInt(hct.hue.roundToInt())
            val excitedProportion = hueExcitedProportions[hue]
            if (hct.chroma < scoring.cutoffChroma || excitedProportion <= scoring.cutoffExcitedProportion) {
                continue
            }
            val proportionScore = excitedProportion * 100.0 * scoring.weightProportion
            val chromaWeight = if (hct.chroma < scoring.targetChroma) scoring.weightChromaBelow else scoring.weightChromaAbove
            val chromaScore = (hct.chroma - scoring.targetChroma) * chromaWeight
            scoredColors.add(ScoredHct(hct, proportionScore + chromaScore))
        }

        if (scoredColors.isEmpty()) return listOf(fallbackColorArgb)
        scoredColors.sortByDescending { it.score }

        val chosen = mutableListOf<Hct>()
        val maxHueDifference = scoring.maxHueDifference.coerceAtLeast(scoring.minHueDifference)
        val minHueDifference = scoring.minHueDifference.coerceAtLeast(1)
        val desiredColorCount = scoring.maxColorCount.coerceAtLeast(1)

        for (differenceDegrees in maxHueDifference downTo minHueDifference) {
            chosen.clear()
            for (candidate in scoredColors) {
                val isDuplicateHue = chosen.any {
                    MathUtils.differenceDegrees(candidate.hct.hue, it.hue) < differenceDegrees.toDouble()
                }
                if (!isDuplicateHue) {
                    chosen.add(candidate.hct)
                }
                if (chosen.size >= desiredColorCount) break
            }
            if (chosen.size >= desiredColorCount) break
        }

        return if (chosen.isEmpty()) listOf(fallbackColorArgb) else chosen.map { it.toInt() }
    }

    private fun averageColorArgb(pixels: IntArray): Int {
        if (pixels.isEmpty()) return DEFAULT_SEED_ARGB
        var totalRed = 0L
        var totalGreen = 0L
        var totalBlue = 0L
        for (argb in pixels) {
            totalRed += (argb ushr 16) and 0xFF
            totalGreen += (argb ushr 8) and 0xFF
            totalBlue += argb and 0xFF
        }
        val size = pixels.size.toLong()
        val r = (totalRed / size).toInt().coerceIn(0, 255)
        val g = (totalGreen / size).toInt().coerceIn(0, 255)
        val b = (totalBlue / size).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
