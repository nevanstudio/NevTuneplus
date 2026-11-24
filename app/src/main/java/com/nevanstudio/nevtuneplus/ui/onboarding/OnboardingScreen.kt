// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nevanstudio.nevtuneplus.R
import com.nevanstudio.nevtuneplus.data.LocalMediaRepository
import com.nevanstudio.nevtuneplus.sharedui.resources.Res
import com.nevanstudio.nevtuneplus.sharedui.resources.ic_google_g
import com.nevanstudio.nevtuneplus.sharedui.resources.ic_logo
import com.nevanstudio.nevtuneplus.ui.icons.NevTunePlusIcons
import org.jetbrains.compose.resources.painterResource

// ─────────────────────────────────────────────────────────────────────────────
// NevTune+ — AMOLED Black & Spotify Green Redesign
//
// Visual Identity:
// - Pure AMOLED black background, Spotify-inspired green (#1DB954) accents.
// - Static, motion-free layout for all screens.
// - Clean minimal layouts with solid color fills, no gradients.
// ─────────────────────────────────────────────────────────────────────────────

private const val PAGE_WELCOME     = 0
private const val PAGE_NAME        = 1
private const val PAGE_CONNECT     = 2
private const val PAGE_PERMISSIONS = 3
private const val PAGE_MUSIC       = 4
private const val TOTAL_PAGES      = 5

private const val NAME_LIMIT = 40

private val SpotifyGreen = Color(0xFF1DB954)
private val AmoledBlack  = Color(0xFF000000)

// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    signedIn: Boolean,
    initialName: String,
    onNameChosen: (String) -> Unit,
    onSignIn: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableIntStateOf(PAGE_WELCOME) }
    var name by rememberSaveable { mutableStateOf(initialName) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = page != PAGE_WELCOME) {
        page = when (page) {
            PAGE_NAME        -> PAGE_WELCOME
            PAGE_CONNECT     -> PAGE_NAME
            PAGE_PERMISSIONS -> PAGE_CONNECT
            else             -> PAGE_PERMISSIONS
        }
    }

    LaunchedEffect(signedIn, page) {
        if (signedIn && page == PAGE_CONNECT) page = PAGE_PERMISSIONS
    }

    Box(
        modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        // Static page content — no animated transitions
        Box(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            OnboardingPageFrame(page = page, onSkip = onFinish) {
                when (page) {
                    PAGE_WELCOME ->
                        WelcomePage(onNext = { page = PAGE_NAME })

                    PAGE_NAME ->
                        NamePage(
                            name = name,
                            onNameChange = { name = it.take(NAME_LIMIT) },
                            onBack  = { page = PAGE_WELCOME },
                            onNext  = {
                                val chosen = name.trim()
                                if (chosen.isEmpty()) return@NamePage
                                focusManager.clearFocus()
                                onNameChosen(chosen)
                                page = if (signedIn) PAGE_PERMISSIONS else PAGE_CONNECT
                            },
                        )

                    PAGE_CONNECT ->
                        ConnectPage(
                            onBack   = { page = PAGE_NAME },
                            onSignIn = onSignIn,
                            onSkip   = { page = PAGE_PERMISSIONS },
                        )

                    PAGE_PERMISSIONS ->
                        PermissionsPage(
                            onBack     = { page = if (signedIn) PAGE_NAME else PAGE_CONNECT },
                            onContinue = { page = PAGE_MUSIC },
                        )

                    else ->
                        ReadyPage(
                            onBack   = { page = PAGE_PERMISSIONS },
                            onFinish = onFinish,
                        )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page frame: tablet-responsive container with header & progress
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnboardingPageFrame(
    page: Int,
    onSkip: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
        ) {
            OnboardingHeader(page = page, onSkip = onSkip)
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header: green logo badge + segmented progress + skip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnboardingHeader(page: Int, onSkip: () -> Unit) {
    // Brand row
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Logo + wordmark
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SpotifyGreen),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
            }
            Text(
                "NEVTUNE+",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.4.sp,
                color = Color.White,
            )
        }

        // Skip button (not on first or last page) — always visible, no AnimatedVisibility
        if (page != PAGE_WELCOME && page != PAGE_MUSIC) {
            Box(
                Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(0.5.dp, SpotifyGreen.copy(alpha = 0.40f), CircleShape)
                    .clickable(role = Role.Button, onClick = onSkip)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Skip",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SpotifyGreen,
                )
            }
        }
    }

    // Segmented progress bar — static weights & alpha, no animation
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp)
            .semantics { contentDescription = "Step ${page + 1} of $TOTAL_PAGES" },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(TOTAL_PAGES) { index ->
            val active  = index <= page
            val current = index == page
            val weight  = if (current) 2.2f else 1f
            val alpha   = when {
                current -> 1f
                active  -> 0.70f
                else    -> 0.20f
            }
            Box(
                Modifier
                    .weight(weight)
                    .height(3.5.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) SpotifyGreen.copy(alpha = alpha)
                        else Color.White.copy(alpha = 0.16f),
                    ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 0 — Welcome
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.WelcomePage(onNext: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    WelcomeHeroVisual()
    Spacer(Modifier.height(28.dp))

    CategoryKicker("ACOUSTIC ARCHITECTURE")
    Spacer(Modifier.height(6.dp))

    Text(
        text = "Pure sound.\nZero compromise.",
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 44.sp,
            lineHeight = 48.sp,
            letterSpacing = (-1.8).sp,
        ),
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(14.dp))

    Text(
        text = "Welcome to NevTune+. A sanctuary for high-resolution audio, bit-perfect local playback, and real-time synchronized music sharing.",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.76f),
        lineHeight = 24.sp,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(36.dp))

    PrimaryButton(label = "Begin Experience", onClick = onNext)

    Spacer(Modifier.height(24.dp))
}

// Static NevTune+ logo hero — no animations
@Composable
private fun WelcomeHeroVisual() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(210.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Static green circle aura
        Box(
            Modifier
                .size(170.dp)
                .clip(CircleShape)
                .background(SpotifyGreen.copy(alpha = 0.08f)),
        )
        // Logo badge
        Box(
            Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SpotifyGreen),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )
        }
        // Static music note icon — top-right accent
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(SpotifyGreen.copy(alpha = 0.18f))
                .align(Alignment.TopEnd)
                .padding(end = 32.dp, top = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 1 — Name
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.NamePage(
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val keyboardUp = WindowInsets.isImeVisible

    // Static avatar — no scale animation, no AnimatedVisibility
    if (!keyboardUp) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.height(16.dp))
            val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (name.isNotBlank()) SpotifyGreen else Color.White.copy(alpha = 0.08f))
                    .border(
                        width = 2.dp,
                        color = SpotifyGreen,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (initial.isNotEmpty()) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (name.isNotBlank()) Color(0xFF041A12) else Color.White,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    BackButton(onBack)
    Spacer(Modifier.height(14.dp))

    CategoryKicker("LISTENER IDENTITY")
    Spacer(Modifier.height(6.dp))

    SectionTitle("Define your\nsound presence.")
    Spacer(Modifier.height(10.dp))
    Text(
        text = "This name represents your profile during Listen Together sessions, party queues, and synchronized playlists.",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.72f),
        lineHeight = 23.sp,
    )

    Spacer(Modifier.height(26.dp))

    Text(
        text = "Your Display Name",
        style = MaterialTheme.typography.titleSmall,
        color = SpotifyGreen,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
    )

    NameInputField(value = name, onValueChange = onNameChange, onDone = onNext)

    Spacer(Modifier.height(10.dp))
    Text(
        text = "Visible to friends in active sessions. You can update this at any time in Settings.",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.48f),
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(32.dp))

    PrimaryButton(
        label   = "Continue",
        onClick = onNext,
        enabled = name.isNotBlank(),
    )

    Spacer(Modifier.height(24.dp))
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 2 — Connect
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.ConnectPage(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSkip: () -> Unit,
) {
    Spacer(Modifier.height(16.dp))
    BackButton(onBack)
    Spacer(Modifier.height(14.dp))

    ConnectHeroVisual()

    Spacer(Modifier.height(24.dp))

    CategoryKicker("UNIFIED LIBRARY")
    Spacer(Modifier.height(6.dp))

    SectionTitle("Infinite streams,\none library.")

    Spacer(Modifier.height(10.dp))
    Text(
        text = "Connect your YouTube Music account to blend cloud recommendations and curated playlists with your offline lossless collection. Local music stays completely private.",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.75f),
        lineHeight = 24.sp,
    )

    Spacer(Modifier.height(22.dp))

    // Glass benefit card
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                SpotifyGreen.copy(alpha = 0.25f),
                RoundedCornerShape(20.dp),
            )
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BenefitRow(
                icon  = Icons.AutoMirrored.Rounded.QueueMusic,
                title = "Cloud & Local Harmonization",
                desc  = "Instant access to millions of tracks alongside your device audio archives.",
            )
            BenefitRow(
                icon  = Icons.Rounded.Security,
                title = "Private & Secure Sign-In",
                desc  = "Direct, encrypted Google authentication. NevTune+ never stores your password.",
            )
        }
    }

    Spacer(Modifier.height(32.dp))

    PrimaryButton(
        label = "Connect with Google",
        onClick = onSignIn,
        leading = {
            Image(
                painter = painterResource(Res.drawable.ic_google_g),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
        },
    )
    Spacer(Modifier.height(12.dp))
    SecondaryButton(label = "Continue Offline", onClick = onSkip)
    Spacer(Modifier.height(24.dp))
}

// Static connect hero — 2×3 grid of static shelf cards, no scroll animation
@Composable
private fun ConnectHeroVisual() {
    val cards = listOf(
        Color(0xFF1A3024) to "High-Res Mix",
        Color(0xFF142B1F) to "Master Releases",
        Color(0xFF1C3528) to "Offline Vault",
        Color(0xFF112518) to "Saved Library",
        Color(0xFF18302A) to "Live Session",
        Color(0xFF152C20) to "Audiophile FLAC",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(105.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            cards.take(5).forEach { (color, label) ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color)
                        .border(0.5.dp, SpotifyGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = SpotifyGreen.copy(alpha = 0.30f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(16.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.padding(6.dp),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
) {
    Row(
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(SpotifyGreen.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(16.dp),
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                lineHeight = 17.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 3 — Permissions
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.PermissionsPage(
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current

    var hasAudioPermission by remember {
        mutableStateOf(LocalMediaRepository.hasStoragePermission(context))
    }
    var hasNotifyPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else true,
        )
    }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        hasAudioPermission = LocalMediaRepository.hasStoragePermission(context)
    }

    val notifyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifyPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    Spacer(Modifier.height(16.dp))
    BackButton(onBack)
    Spacer(Modifier.height(14.dp))

    CategoryKicker("SYSTEM PRIVILEGES")
    Spacer(Modifier.height(6.dp))

    SectionTitle("Unrestricted\nmedia access.")
    Spacer(Modifier.height(10.dp))
    Text(
        text = "NevTune+ operates locally on your device. Grant access to index high-resolution music and maintain fluid background controls.",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.75f),
        lineHeight = 24.sp,
    )

    Spacer(Modifier.height(24.dp))

    PermissionCard(
        icon        = Icons.Rounded.FolderSpecial,
        title       = "Local Audio Vault",
        description = "Indexes lossless FLAC, ALAC, WAV, and MP3 audio files on your device with zero cloud tracking.",
        badge       = "Essential",
        isGranted   = hasAudioPermission,
        onGrant     = {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_AUDIO
            else
                Manifest.permission.READ_EXTERNAL_STORAGE
            audioLauncher.launch(perm)
        },
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            icon        = Icons.Rounded.NotificationsActive,
            title       = "Live Playback Hub",
            description = "Provides real-time lock screen controls, notification media widgets, and audio output selection.",
            badge       = "Recommended",
            isGranted   = hasNotifyPermission,
            onGrant     = { notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
        )
    }

    Spacer(Modifier.height(32.dp))

    PrimaryButton(label = "Proceed", onClick = onContinue)
    Spacer(Modifier.height(24.dp))
}

/**
 * Responsive Permission Card designed to never clip or squish text on any screen.
 */
@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    badge: String,
    isGranted: Boolean,
    onGrant: () -> Unit,
) {
    val accentColor = if (isGranted) SpotifyGreen else Color.White.copy(alpha = 0.60f)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (isGranted) SpotifyGreen.copy(alpha = 0.50f)
                else Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (badge == "Essential") SpotifyGreen else Color.White.copy(alpha = 0.60f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (badge == "Essential") SpotifyGreen.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.08f),
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.68f),
                    lineHeight = 18.sp,
                )
            }

            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isGranted) SpotifyGreen.copy(alpha = 0.16f)
                        else SpotifyGreen,
                    )
                    .clickable(enabled = !isGranted, onClick = onGrant)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isGranted) "Active ✓" else "Grant",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) SpotifyGreen else Color(0xFF041A12),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 4 — Ready
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.ReadyPage(onBack: () -> Unit, onFinish: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    BackButton(onBack)
    Spacer(Modifier.height(24.dp))

    // Static check mark hero — no pulsing rings
    ReadyCelebrationMark()

    Spacer(Modifier.height(32.dp))

    CategoryKicker("SYSTEM CONFIGURED")
    Spacer(Modifier.height(6.dp))

    SectionTitle("Experience the\ndifference.")
    Spacer(Modifier.height(10.dp))
    Text(
        text = "Your listening environment is primed. Prepare for astonishing clarity, dynamic range, and boundless musical freedom.",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.78f),
        lineHeight = 24.sp,
    )

    Spacer(Modifier.height(36.dp))

    PrimaryButton(label = "Enter NevTune+", onClick = onFinish)
    Spacer(Modifier.height(24.dp))
}

// Static celebration mark — solid green circle with check icon
@Composable
private fun ReadyCelebrationMark() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(125.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Static ambient ring
        Box(
            Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(SpotifyGreen.copy(alpha = 0.10f)),
        )
        // Central check circle
        Box(
            Modifier
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

// ─────────────────────────────────────────────────────────────────────────────
// Shared primitive UI components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryKicker(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.sp,
        color = SpotifyGreen,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize    = 34.sp,
            lineHeight  = 38.sp,
            letterSpacing = (-0.8).sp,
        ),
        color = Color.White,
        fontWeight = FontWeight.Bold,
    )
}

/**
 * Solid Spotify-green action button — no shimmer, no spring scale.
 */
@Composable
private fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) SpotifyGreen else SpotifyGreen.copy(alpha = 0.42f))
            .clickable(
                enabled = enabled,
                role    = Role.Button,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(
            text       = label,
            style      = MaterialTheme.typography.titleMedium,
            fontSize   = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = Color(0xFF041A12),
            textAlign  = TextAlign.Center,
            maxLines   = 1,
        )
    }
}

/**
 * Secondary ghost button.
 */
@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, SpotifyGreen.copy(alpha = 0.40f), RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = SpotifyGreen,
            maxLines   = 1,
        )
    }
}

/**
 * Back button.
 */
@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.5.dp, SpotifyGreen.copy(alpha = 0.30f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = NevTunePlusIcons.ChevronRight,
            contentDescription = stringResource(R.string.onboarding_back),
            tint = Color.White,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { scaleX = -1f },
        )
    }
}

/**
 * Name input field with Spotify green focus border.
 */
@Composable
private fun NameInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val hint  = stringResource(R.string.onboarding_name_hint)
    val style = MaterialTheme.typography.displayLarge.copy(
        fontSize      = 22.sp,
        lineHeight    = 28.sp,
        letterSpacing = (-0.5).sp,
        color         = Color.White,
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                1.5.dp,
                if (value.isNotBlank()) SpotifyGreen else Color.White.copy(alpha = 0.16f),
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(text = hint, style = style, color = Color.White.copy(alpha = 0.32f), maxLines = 1)
        }
        BasicTextField(
            value           = value,
            onValueChange   = onValueChange,
            singleLine      = true,
            textStyle       = style,
            cursorBrush     = SolidColor(SpotifyGreen),
            keyboardOptions = KeyboardOptions(
                capitalization   = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                imeAction        = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onDone() }),
            modifier        = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = hint },
        )
    }
}
