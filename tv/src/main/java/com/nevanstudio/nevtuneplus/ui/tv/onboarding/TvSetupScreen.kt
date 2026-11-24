// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.tv.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevanstudio.nevtuneplus.data.settings.AppSettings
import com.nevanstudio.nevtuneplus.data.settings.TvSettings
import com.nevanstudio.nevtuneplus.ui.MainViewModel
import com.nevanstudio.nevtuneplus.ui.tv.TvAppMark
import com.nevanstudio.nevtuneplus.ui.tv.audio.TvSpatialAudioEngine
import com.nevanstudio.nevtuneplus.ui.tv.components.TvButton
import com.nevanstudio.nevtuneplus.ui.tv.components.tvClick
import com.nevanstudio.nevtuneplus.ui.tv.components.tvInitialFocus
import com.nevanstudio.nevtuneplus.ui.tv.components.tvLift
import com.nevanstudio.nevtuneplus.ui.tv.dialogs.TvAccountDialog
import com.nevanstudio.nevtuneplus.ui.tv.personalization.AppThemeOption
import com.nevanstudio.nevtuneplus.ui.tv.theme.TvDarkPalette
import com.nevanstudio.nevtuneplus.ui.tv.theme.TvMidnightPalette
import com.nevanstudio.nevtuneplus.ui.tv.theme.TvOledPalette
import com.nevanstudio.nevtuneplus.ui.tv.theme.TvType

private val SpotifyGreen = Color(0xFF1DB954)
private val AmoledBlack  = Color(0xFF000000)

private enum class SetupStep { Welcome, SignIn, Appearance, Preferences, Done }

/**
 * AMOLED Black & Spotify Green Android TV Onboarding for NevTune+.
 *
 * - 10-foot optimized layouts, ample padding and typography.
 * - Static, motion-free — no infinite animations.
 * - Remote D-pad focus handling with tvLift, tvClick, and tvInitialFocus.
 * - Persistent configuration saving on completion.
 */
@Composable
fun TvSetupScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(SetupStep.Welcome) }
    var theme by remember { mutableStateOf(AppThemeOption.fromId(TvSettings.tvTheme.value)) }

    val finish = {
        TvSettings.setTvPersonalization(
            nickname = TvSettings.tvNickname.value,
            themeId = theme.id,
            version = 1,
        )
        onComplete()
    }

    BackHandler(enabled = step != SetupStep.Welcome) {
        step = SetupStep.entries[step.ordinal - 1]
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TV Top Segmented Spotify-green Progress Bar
            TvSetupProgressIndicator(
                currentStep = step.ordinal,
                totalSteps = SetupStep.entries.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, start = 48.dp, end = 48.dp),
            )

            // Simple crossfade between steps — no slide animation
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(tween(160)).togetherWith(fadeOut(tween(120)))
                },
                label = "tvSetupStep",
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) { current ->
                when (current) {
                    SetupStep.Welcome -> TvSetupPage(
                        category = "ACOUSTIC PRECISION",
                        title = "Sound elevated.\nLiving room scale.",
                        message = "Welcome to NevTune+. Stream master quality audio, enjoy local lossless files, and experience room-filling sound.",
                        hero = { TvWelcomeHero() },
                    ) {
                        TvSetupButton("Begin Setup", initialFocus = true) { step = SetupStep.SignIn }
                        TvSetupButton("Set Up Later") { finish() }
                    }

                    SetupStep.SignIn -> TvSignInStep(
                        viewModel = viewModel,
                        onContinue = { step = SetupStep.Appearance },
                    )

                    SetupStep.Appearance -> TvSetupPage(
                        category = "VISUAL ATMOSPHERE",
                        title = "Choose Your Style",
                        message = "Select a visual atmosphere tuned for your television panel. Customizable anytime in Settings.",
                        hero = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppThemeOption.entries.forEach { option ->
                                    TvThemeSwatch(
                                        option = option,
                                        selected = option == theme,
                                        initialFocus = option == theme,
                                        onClick = { theme = option },
                                    )
                                }
                            }
                        },
                    ) {
                        TvSetupButton("Continue", initialFocus = true) {
                            TvSettings.setTvTheme(theme.id)
                            step = SetupStep.Preferences
                        }
                    }

                    SetupStep.Preferences -> TvPreferencesStep(
                        onContinue = { step = SetupStep.Done },
                    )

                    SetupStep.Done -> TvSetupPage(
                        category = "SYSTEM CONFIGURED",
                        title = "You're All Set",
                        message = "Your listening environment is ready. Discover boundless music, pristine audio fidelity, and effortless playback.",
                        hero = { TvDoneHero() },
                    ) {
                        TvSetupButton("Start Listening", initialFocus = true) { finish() }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TV Top Progress Indicator — static weights & alpha, no animation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TvSetupProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.semantics { contentDescription = "Setup Step ${currentStep + 1} of $totalSteps" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val isActive  = index <= currentStep
            val isCurrent = index == currentStep
            val weight    = if (isCurrent) 2f else 1f
            val alpha     = when {
                isCurrent -> 1f
                isActive  -> 0.70f
                else      -> 0.18f
            }
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) SpotifyGreen.copy(alpha = alpha) else Color.White.copy(alpha = 0.14f),
                    ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TV Setup Page Frame
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TvSetupPage(
    category: String,
    title: String,
    message: String,
    hero: @Composable () -> Unit,
    buttons: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        hero()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = category,
            style = TvType.Caption.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
            ),
            color = SpotifyGreen,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = TvType.LargeTitle.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = TvType.Body.copy(
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 540.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.width(320.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = buttons,
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun TvSetupButton(text: String, initialFocus: Boolean = false, onClick: () -> Unit) {
    TvButton(
        text = text,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .tvInitialFocus(initialFocus),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// TV Welcome Hero — static NevTune+ logo with green background, no animation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TvWelcomeHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Static ambient aura
        Box(
            Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(SpotifyGreen.copy(alpha = 0.10f)),
        )
        // Static TvAppMark
        TvAppMark(size = 92.dp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TV Sign In Step
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TvSignInStep(viewModel: MainViewModel, onContinue: () -> Unit) {
    val signedIn by viewModel.signedIn.collectAsState()
    val account by viewModel.account.collectAsState()
    var signingIn by remember { mutableStateOf(false) }

    LaunchedEffect(signedIn) {
        if (signedIn && signingIn) {
            signingIn = false
            onContinue()
        }
    }

    TvSetupPage(
        category = "CLOUD & LOCAL INTEGRATION",
        title = if (signedIn) "Signed In" else "Connect YouTube Music",
        message = if (signedIn) {
            "Connected to ${account?.name ?: "your account"}. Your playlists and recommendations are synchronized."
        } else {
            "Stream millions of releases alongside your offline device files. Playback is completely private."
        },
        hero = {
            Box(
                modifier = Modifier
                    .width(460.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, SpotifyGreen.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 22.dp, vertical = 18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TvSignInBenefitItem(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        label = "Cloud Sync",
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color.White.copy(alpha = 0.12f)),
                    )
                    TvSignInBenefitItem(
                        icon = Icons.Rounded.MusicNote,
                        label = "Master Quality",
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color.White.copy(alpha = 0.12f)),
                    )
                    TvSignInBenefitItem(
                        icon = Icons.Rounded.Security,
                        label = "Private & Safe",
                    )
                }
            }
        },
    ) {
        if (signedIn) {
            TvSetupButton("Continue", initialFocus = true, onClick = onContinue)
        } else {
            TvSetupButton("Sign In", initialFocus = true) { signingIn = true }
            TvSetupButton("Continue Offline", onClick = onContinue)
        }
    }

    if (signingIn && !signedIn) {
        TvAccountDialog(viewModel = viewModel, onDismiss = { signingIn = false }, allowPhoneSignIn = false)
    }
}

@Composable
private fun TvSignInBenefitItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SpotifyGreen.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = TvType.Caption,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TV Appearance Step & Theme Swatches
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TvThemeSwatch(
    option: AppThemeOption,
    selected: Boolean,
    initialFocus: Boolean,
    onClick: () -> Unit,
) {
    val palette = when (option) {
        AppThemeOption.MIDNIGHT       -> TvMidnightPalette
        AppThemeOption.PURE_BLACK     -> TvOledPalette
        AppThemeOption.DYNAMIC_ARTWORK -> TvDarkPalette
    }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(172.dp)
                .aspectRatio(16f / 10f)
                .tvInitialFocus(initialFocus)
                .tvLift(interaction, RoundedCornerShape(13.dp), focusedScale = 1.08f, elevation = 18.dp)
                .tvClick(interaction, onClick = onClick)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            if (option == AppThemeOption.DYNAMIC_ARTWORK) Color(0xFF0D2418) else palette.canvasTop,
                            palette.canvasBottom,
                        ),
                    ),
                )
                .border(
                    width = if (focused) 2.5.dp else if (selected) 1.5.dp else 0.5.dp,
                    color = if (focused) Color.White else if (selected) SpotifyGreen else Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(13.dp),
                )
                .padding(13.dp),
        ) {
            Row(
                modifier = Modifier.align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color(0xFF041A12),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = option.title,
            style = TvType.CardTitle.copy(fontWeight = if (focused || selected) FontWeight.Bold else FontWeight.Medium),
            color = if (focused) Color.White else if (selected) SpotifyGreen else Color.White.copy(alpha = 0.70f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TV Preferences Step
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TvPreferencesStep(onContinue: () -> Unit) {
    val lyrics by AppSettings.syncedLyrics.collectAsState()
    val canvas by AppSettings.animatedCanvas.collectAsState()
    val automix by AppSettings.smartFadeEnabled.collectAsState()
    val spatial by TvSettings.spatialAudioEnabled.collectAsState()

    TvSetupPage(
        category = "LISTENING ARCHITECTURE",
        title = "Acoustic Configuration",
        message = "Fine-tune how NevTune+ plays across your TV and sound system. Click any option to toggle.",
        hero = {
            Column(
                modifier = Modifier.width(460.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                TvPreferenceRow(
                    title = "Synced Lyrics",
                    subtitle = "Scrolls active timecoded lyrics in real time with playback",
                    enabled = lyrics,
                    initialFocus = true,
                    onToggle = { AppSettings.setSyncedLyrics(!lyrics) },
                )
                TvPreferenceRow(
                    title = "Motion Canvas",
                    subtitle = "Renders dynamic artwork and ambient backgrounds",
                    enabled = canvas,
                    onToggle = { AppSettings.setAnimatedCanvas(!canvas) },
                )
                TvPreferenceRow(
                    title = "Smart Automix",
                    subtitle = "DJ-style crossfade transitions between tracks",
                    enabled = automix,
                    onToggle = { AppSettings.setSmartFadeEnabled(!automix) },
                )
                TvPreferenceRow(
                    title = "Spatial Soundstage",
                    subtitle = "Expands stereo field for TV surround and soundbars",
                    enabled = spatial,
                    onToggle = {
                        TvSettings.setSpatialAudioEnabled(!spatial)
                        TvSpatialAudioEngine.setEnabled(!spatial)
                    },
                )
            }
        },
    ) {
        TvSetupButton("Continue", onClick = onContinue)
    }
}

@Composable
private fun TvPreferenceRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    initialFocus: Boolean = false,
    onToggle: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvInitialFocus(initialFocus)
            .tvLift(interaction, RoundedCornerShape(12.dp), focusedScale = 1.03f, elevation = 10.dp)
            .background(
                if (focused) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp),
            )
            .border(
                width = if (focused) 1.5.dp else 0.5.dp,
                color = if (focused) Color.White else SpotifyGreen.copy(alpha = 0.20f),
                shape = RoundedCornerShape(12.dp),
            )
            .tvClick(interaction, onClick = onToggle)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TvType.Body.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = TvType.Caption,
                color = Color.White.copy(alpha = 0.60f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (enabled) SpotifyGreen.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f),
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (enabled) "Active" else "Off",
                style = TvType.Caption.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) SpotifyGreen else Color.White.copy(alpha = 0.55f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TV Done Hero — static check icon, no pulsing rings
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TvDoneHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Static ambient aura
        Box(
            Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SpotifyGreen.copy(alpha = 0.12f)),
        )
        // Central celebration checkmark circle
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(SpotifyGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF041A12),
                modifier = Modifier.size(42.dp),
            )
        }
    }
}
