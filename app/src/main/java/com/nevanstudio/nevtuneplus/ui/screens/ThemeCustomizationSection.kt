// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nevanstudio.nevtuneplus.data.settings.AppSettings
import com.nevanstudio.nevtuneplus.data.settings.ThemeColorSource
import com.nevanstudio.nevtuneplus.data.settings.ThemeMode
import com.nevanstudio.nevtuneplus.ui.theme.ColorSchemePreset
import com.nevanstudio.nevtuneplus.ui.theme.ThemeColorPresets
import kotlin.math.abs
import kotlin.math.roundToInt

/** Returns a concise human-readable label describing the active color scheme or source. */
internal fun activeThemeBadgeLabel(
    themeMode: ThemeMode,
    colorSource: ThemeColorSource,
    customColorScheme: String,
    amoledTheme: Boolean,
): String {
    val modeLabel = when (themeMode) {
        ThemeMode.SYSTEM -> "Auto"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> if (amoledTheme) "AMOLED" else "Dark"
    }
    val sourceLabel = when (colorSource) {
        ThemeColorSource.ALBUM_ART -> "Album Art"
        ThemeColorSource.MONET -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Monet" else "Preset"
        ThemeColorSource.CUSTOM -> {
            if (customColorScheme.startsWith("custom_")) {
                "Custom"
            } else {
                ThemeColorPresets.firstOrNull { it.id == customColorScheme }?.displayName ?: "Mono"
            }
        }
    }
    return "$modeLabel • $sourceLabel"
}

@Composable
internal fun AmoledBlackSettingRow(
    themeMode: ThemeMode,
    amoledTheme: Boolean,
    onToggleAmoled: (Boolean) -> Unit,
) {
    val darkCapable = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
    }
    SettingsRow(
        icon = Icons.Rounded.Contrast,
        title = "AMOLED Pitch Black",
        subtitle = if (darkCapable) {
            "Pure #000000 background & surfaces with subtle card separation"
        } else {
            "Switch Theme Mode to Dark or System (at night) to use AMOLED black"
        },
        enabled = darkCapable,
        trailing = {
            Switch(
                checked = amoledTheme,
                onCheckedChange = onToggleAmoled,
                enabled = darkCapable,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        onClick = { onToggleAmoled(!amoledTheme) },
    )
}

@Composable
internal fun ColorSourceSettingRow(
    colorSource: ThemeColorSource,
    onSelectSource: (ThemeColorSource) -> Unit,
) {
    val supportsMonet = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    SettingsRow(
        icon = Icons.Rounded.Palette,
        title = "Dynamic Color Source",
        subtitle = when (colorSource) {
            ThemeColorSource.CUSTOM -> "Using curated NevTune+ presets or your custom palette"
            ThemeColorSource.MONET -> "Adapts automatically to your device wallpaper (Material You)"
            ThemeColorSource.ALBUM_ART -> "Generates Material 3 colors live from the currently playing track"
        },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ColorSourceChip(
            label = "Presets",
            icon = Icons.Rounded.ColorLens,
            selected = colorSource == ThemeColorSource.CUSTOM,
            enabled = true,
            onClick = { onSelectSource(ThemeColorSource.CUSTOM) },
            modifier = Modifier.weight(1f),
        )
        ColorSourceChip(
            label = "System",
            icon = Icons.Rounded.Wallpaper,
            selected = colorSource == ThemeColorSource.MONET && supportsMonet,
            enabled = supportsMonet,
            onClick = { if (supportsMonet) onSelectSource(ThemeColorSource.MONET) },
            modifier = Modifier.weight(1f),
        )
        ColorSourceChip(
            label = "Album Art",
            icon = Icons.Rounded.Album,
            selected = colorSource == ThemeColorSource.ALBUM_ART,
            enabled = true,
            onClick = { onSelectSource(ThemeColorSource.ALBUM_ART) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ColorSourceChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(180),
        label = "sourceChipBg",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            selected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(180),
        label = "sourceChipContent",
    )
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = if (!enabled) "Android 12+" else label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun AlbumArtOptionsContent(
    useExactArtworkColors: Boolean,
    onToggleExactArtworkColors: (Boolean) -> Unit,
    extractedSeedArgb: Int?,
) {
    SettingsSubRow(
        title = "Exact Artwork Hue",
        subtitle = "Preserve exact dominant artwork hue instead of soft tonal pastel shifts",
        checked = useExactArtworkColors,
        onCheckedChange = onToggleExactArtworkColors,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val previewColor = extractedSeedArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            previewColor,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                        ),
                    ),
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        )
        Text(
            text = if (extractedSeedArgb != null) {
                "Live palette active (${colorToHex(previewColor)})"
            } else {
                "Play a track to generate colors from its cover art"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ThemePresetsRowContent(
    activeSchemeId: String,
    onSelectPreset: (String) -> Unit,
    onOpenCustomPicker: () -> Unit,
) {
    val isCustomTriplet = activeSchemeId.startsWith("custom_")
    val parsedCustomTriplet = remember(activeSchemeId) {
        parseCustomHexTriplet(activeSchemeId)
    }

    SettingsRow(
        icon = Icons.Rounded.AutoAwesome,
        title = "Color Palette",
        subtitle = if (isCustomTriplet) {
            "Active: Custom RGB/HSL theme"
        } else {
            val activePreset = ThemeColorPresets.firstOrNull { it.id == activeSchemeId }
            "Active: ${activePreset?.displayName ?: "NevTune+ Mono"}"
        },
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = ROW_INSET),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 12.dp),
    ) {
        if (parsedCustomTriplet != null) {
            item(key = "custom_active_card") {
                ThemeColorPresetCard(
                    title = "Custom",
                    description = "Your palette",
                    primary = parsedCustomTriplet.first,
                    secondary = parsedCustomTriplet.second,
                    tertiary = parsedCustomTriplet.third,
                    selected = true,
                    onClick = onOpenCustomPicker,
                )
            }
        }
        items(ThemeColorPresets, key = { it.id }) { preset ->
            ThemeColorPresetCard(
                preset = preset,
                selected = activeSchemeId == preset.id,
                onClick = { onSelectPreset(preset.id) },
            )
        }
    }

    SettingsRow(
        icon = Icons.Rounded.Tune,
        title = "Create Custom Theme",
        subtitle = if (parsedCustomTriplet != null) {
            "Primary ${colorToHex(parsedCustomTriplet.first)} • Secondary ${colorToHex(parsedCustomTriplet.second)}"
        } else {
            "Pick Primary, Secondary & Tertiary colors with HSL sliders"
        },
        value = if (isCustomTriplet) "Edit" else "Create",
        onClick = onOpenCustomPicker,
    )
}

@Composable
private fun ThemeColorPresetCard(
    preset: ColorSchemePreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ThemeColorPresetCard(
        title = preset.displayName,
        description = preset.description,
        primary = preset.primaryColor,
        secondary = preset.secondaryColor,
        tertiary = preset.tertiaryColor,
        selected = selected,
        onClick = onClick,
    )
}

@Composable
private fun ThemeColorPresetCard(
    title: String,
    description: String,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(180),
        label = "presetCardBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(180),
        label = "presetCardBorder",
    )

    Surface(
        modifier = Modifier
            .width(126.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(primary, secondary, tertiary)),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                )
                AnimatedVisibility(visible = selected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class HslColor(
    val hue: Float,
    val saturation: Float,
    val lightness: Float,
)

private fun Color.toHsl(): HslColor {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return HslColor(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f)) / 6f
        g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    } * 360f
    return HslColor(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun HslColor.toComposeColor(): Color {
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = lightness - c / 2f
    val (r, g, b) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f),
    )
}

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return "#%06X".format(argb and 0xFFFFFF)
}

private fun parseCustomHexTriplet(schemeId: String): Triple<Color, Color, Color>? {
    if (!schemeId.startsWith("custom_")) return null
    val parts = schemeId.split("_")
    if (parts.size != 4) return null
    return runCatching {
        Triple(
            Color(("FF" + parts[1].padStart(6, '0')).toLong(16)),
            Color(("FF" + parts[2].padStart(6, '0')).toLong(16)),
            Color(("FF" + parts[3].padStart(6, '0')).toLong(16)),
        )
    }.getOrNull()
}

private val QuickInspirationTriplets = listOf(
    "Sunset" to Triple(Color(0xFFFF6B35), Color(0xFFF7931E), Color(0xFFFFC857)),
    "Cyber" to Triple(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFFFF2A85)),
    "Forest" to Triple(Color(0xFF2E7D32), Color(0xFF66BB6A), Color(0xFF00BFA5)),
    "Royal" to Triple(Color(0xFF5E35B1), Color(0xFF7E57C2), Color(0xFFFFB300)),
)

/**
 * Interactive RGB/HSL bottom sheet for crafting custom Primary, Secondary, and Tertiary colors.
 * Adapted from NevPlay's CustomColorsDialog & ExpressiveColorPickerControls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomColorsSheet(
    currentSchemeId: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    val initialTriplet = remember(currentSchemeId) {
        parseCustomHexTriplet(currentSchemeId)
            ?: ThemeColorPresets.firstOrNull { it.id == currentSchemeId }?.let {
                Triple(it.primaryColor, it.secondaryColor, it.tertiaryColor)
            }
            ?: Triple(Color(0xFF1DB954), Color(0xFF5AC8FA), Color(0xFFFF6B35))
    }

    var primaryColor by remember { mutableStateOf(initialTriplet.first) }
    var secondaryColor by remember { mutableStateOf(initialTriplet.second) }
    var tertiaryColor by remember { mutableStateOf(initialTriplet.third) }
    var selectedSlot by remember { mutableIntStateOf(0) } // 0 = Primary, 1 = Secondary, 2 = Tertiary

    val activeColor = when (selectedSlot) {
        0 -> primaryColor
        1 -> secondaryColor
        else -> tertiaryColor
    }
    var hue by remember(selectedSlot) { mutableFloatStateOf(activeColor.toHsl().hue) }
    var saturation by remember(selectedSlot) { mutableFloatStateOf(activeColor.toHsl().saturation) }
    var lightness by remember(selectedSlot) { mutableFloatStateOf(activeColor.toHsl().lightness) }

    fun updateSlotFromHsl(newH: Float = hue, newS: Float = saturation, newL: Float = lightness) {
        hue = newH
        saturation = newS
        lightness = newL
        val updated = HslColor(newH, newS, newL).toComposeColor()
        when (selectedSlot) {
            0 -> primaryColor = updated
            1 -> secondaryColor = updated
            else -> tertiaryColor = updated
        }
    }

    fun applyQuickPreset(triplet: Triple<Color, Color, Color>) {
        primaryColor = triplet.first
        secondaryColor = triplet.second
        tertiaryColor = triplet.third
        val current = when (selectedSlot) {
            0 -> primaryColor
            1 -> secondaryColor
            else -> tertiaryColor
        }.toHsl()
        hue = current.hue
        saturation = current.saturation
        lightness = current.lightness
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(primaryColor, secondaryColor, tertiaryColor)),
                        ),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Custom Color Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Fine-tune your app accents live across Material 3",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Color role tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "Primary" to primaryColor,
                    "Secondary" to secondaryColor,
                    "Tertiary" to tertiaryColor,
                ).forEachIndexed { index, (label, color) ->
                    val selected = selectedSlot == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedSlot = index },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            },
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Live swatch & hex readout
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = activeColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            ) {
                val contentOnActive = if (activeColor.luminance() > 0.45f) Color.Black else Color.White
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = when (selectedSlot) {
                                0 -> "Primary Accent"
                                1 -> "Secondary Accent"
                                else -> "Tertiary Accent"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = contentOnActive.copy(alpha = 0.8f),
                        )
                        Text(
                            text = colorToHex(activeColor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = contentOnActive,
                        )
                    }
                    Text(
                        text = "H ${hue.roundToInt()}°  S ${(saturation * 100).roundToInt()}%  L ${(lightness * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = contentOnActive.copy(alpha = 0.9f),
                    )
                }
            }

            // H/S/L gradient sliders
            GradientHslSlider(
                label = "Hue",
                valueText = "${hue.roundToInt()}°",
                fraction = (hue / 360f).coerceIn(0f, 1f),
                gradientColors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
                ),
                thumbColor = HslColor(hue, 1f, 0.5f).toComposeColor(),
                onFractionChange = { updateSlotFromHsl(newH = it * 360f) },
            )
            GradientHslSlider(
                label = "Saturation",
                valueText = "${(saturation * 100).roundToInt()}%",
                fraction = saturation,
                gradientColors = listOf(
                    HslColor(hue, 0f, lightness).toComposeColor(),
                    HslColor(hue, 1f, lightness).toComposeColor(),
                ),
                thumbColor = activeColor,
                onFractionChange = { updateSlotFromHsl(newS = it) },
            )
            GradientHslSlider(
                label = "Lightness",
                valueText = "${(lightness * 100).roundToInt()}%",
                fraction = lightness,
                gradientColors = listOf(
                    Color.Black,
                    HslColor(hue, saturation, 0.5f).toComposeColor(),
                    Color.White,
                ),
                thumbColor = activeColor,
                onFractionChange = { updateSlotFromHsl(newL = it) },
            )

            // Quick inspiration presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Inspirations",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickInspirationTriplets.forEach { (name, triplet) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { applyQuickPreset(triplet) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(triplet.first, triplet.second, triplet.third)),
                                        ),
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val p = "%06X".format(primaryColor.toArgb() and 0xFFFFFF)
                        val s = "%06X".format(secondaryColor.toArgb() and 0xFFFFFF)
                        val t = "%06X".format(tertiaryColor.toArgb() and 0xFFFFFF)
                        onApply("custom_${p}_${s}_${t}")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = if (primaryColor.luminance() > 0.45f) Color.Black else Color.White,
                    ),
                ) {
                    Text("Apply Theme", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GradientHslSlider(
    label: String,
    valueText: String,
    fraction: Float,
    gradientColors: List<Color>,
    thumbColor: Color,
    onFractionChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        var trackWidthPx by remember { mutableFloatStateOf(1f) }
        val latestOnChange by androidx.compose.runtime.rememberUpdatedState(onFractionChange)
        val clampedFraction = fraction.coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        latestOnChange((offset.x / trackWidthPx).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        latestOnChange((change.position.x / trackWidthPx).coerceIn(0f, 1f))
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.horizontalGradient(gradientColors))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(7.dp)),
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = 12.dp.toPx()
                val usableWidth = (size.width - radius * 2f).coerceAtLeast(1f)
                val cx = radius + clampedFraction * usableWidth
                val cy = size.height / 2f
                drawCircle(
                    color = Color.White,
                    radius = radius,
                    center = Offset(cx, cy),
                )
                drawCircle(
                    color = thumbColor,
                    radius = radius - 3.dp.toPx(),
                    center = Offset(cx, cy),
                )
            }
        }
    }
}
