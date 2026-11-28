// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nevanstudio.nevtuneplus.R
import com.nevanstudio.nevtuneplus.auth.clearSpotifyWebSession
import com.nevanstudio.nevtuneplus.data.model.Account
import com.nevanstudio.nevtuneplus.data.settings.AppSettings
import com.nevanstudio.nevtuneplus.sharedui.resources.Res
import com.nevanstudio.nevtuneplus.sharedui.resources.spotify_logo
import com.nevanstudio.nevtuneplus.ui.theme.NevTuneColors
import org.jetbrains.compose.resources.painterResource
import java.util.Locale
import kotlin.math.roundToInt

private val SPOTIFY_GREEN = Color(0xFF1DB954)
private val DISCORD_BLURPLE = Color(0xFF5865F2)
private val LASTFM_RED = Color(0xFFD51007)
private val LISTENBRAINZ_ORANGE = Color(0xFFEB743B)

@Composable
fun AccountAndScrobblingScreen(
    signedIn: Boolean,
    account: Account?,
    channelName: String?,
    onSignIn: () -> Unit,
    onSwitchChannel: () -> Unit,
    onSignOut: () -> Unit,
    onOpenListenBrainzLogin: () -> Unit,
    onOpenLastfmLogin: () -> Unit,
    onOpenDiscord: () -> Unit,
    onOpenSpotify: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val lastfmEnabled by AppSettings.lastfmEnabled.collectAsStateWithLifecycle()
    val lastfmUsername by AppSettings.lastfmUsername.collectAsStateWithLifecycle()
    val lastfmSessionKey by AppSettings.lastfmSessionKey.collectAsStateWithLifecycle()
    val lastfmScrobbleEnabled by AppSettings.lastfmScrobbleEnabled.collectAsStateWithLifecycle()
    val lastfmNowPlayingEnabled by AppSettings.lastfmNowPlaying.collectAsStateWithLifecycle()
    val lastfmPrimaryArtistOnly by AppSettings.lastfmPrimaryArtistOnly.collectAsStateWithLifecycle()
    val scrobbleMinDuration by AppSettings.scrobbleMinDuration.collectAsStateWithLifecycle()
    val scrobbleDelayPercent by AppSettings.scrobbleDelayPercent.collectAsStateWithLifecycle()
    val scrobbleDelaySeconds by AppSettings.scrobbleDelaySeconds.collectAsStateWithLifecycle()
    val listenBrainzEnabled by AppSettings.listenBrainzEnabled.collectAsStateWithLifecycle()
    val listenBrainzToken by AppSettings.listenBrainzToken.collectAsStateWithLifecycle()
    val listenBrainzPrimaryArtistOnly by AppSettings.listenBrainzPrimaryArtistOnly.collectAsStateWithLifecycle()
    val discordToken by AppSettings.discordToken.collectAsStateWithLifecycle()
    val discordUsername by AppSettings.discordUsername.collectAsStateWithLifecycle()
    val discordRpcEnabled by AppSettings.discordRpcEnabled.collectAsStateWithLifecycle()
    val spotifyConnected by AppSettings.spotifySpdcToken.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        Text(
            text = stringResource(R.string.account_integrations),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
        )

        AccountHeroSection(
            signedIn = signedIn,
            account = account,
            channelName = channelName,
            onSignIn = onSignIn,
            onSwitchChannel = onSwitchChannel,
            onSignOut = onSignOut,
        )

        SectionHeader(stringResource(R.string.spotify))
        SpotifyIntegrationCard(
            connected = spotifyConnected.isNotBlank(),
            onOpenSpotify = onOpenSpotify,
            onDisconnect = {
                clearSpotifyWebSession()
                AppSettings.setSpotifySpdcToken("")
            },
        )

        SectionHeader(stringResource(R.string.rich_presence))
        DiscordIntegrationCard(
            connected = discordToken.isNotEmpty(),
            rpcEnabled = discordRpcEnabled,
            username = discordUsername,
            onClick = onOpenDiscord,
        )

        SectionHeader(stringResource(R.string.scrobbling))
        if (AppSettings.scrobblingAvailable) {
            ListenBrainzCard(
                enabled = listenBrainzEnabled,
                token = listenBrainzToken,
                primaryArtistOnly = listenBrainzPrimaryArtistOnly,
                onOpenLogin = onOpenListenBrainzLogin,
                onToggleEnabled = { checked ->
                    if (checked && listenBrainzToken.isBlank()) {
                        onOpenListenBrainzLogin()
                    } else {
                        AppSettings.setListenBrainzEnabled(checked)
                    }
                },
                onTogglePrimaryArtistOnly = AppSettings::setListenBrainzPrimaryArtistOnly,
            )

            Spacer(Modifier.height(10.dp))

            LastfmCard(
                enabled = lastfmEnabled,
                username = lastfmUsername,
                sessionKey = lastfmSessionKey,
                scrobbleEnabled = lastfmScrobbleEnabled,
                nowPlayingEnabled = lastfmNowPlayingEnabled,
                primaryArtistOnly = lastfmPrimaryArtistOnly,
                onOpenLogin = onOpenLastfmLogin,
                onToggleEnabled = { checked ->
                    if (checked && lastfmSessionKey.isBlank()) {
                        onOpenLastfmLogin()
                    } else {
                        AppSettings.setLastfmEnabled(checked)
                    }
                },
                onToggleScrobble = AppSettings::setLastfmScrobbleEnabled,
                onTogglePrimaryArtistOnly = AppSettings::setLastfmPrimaryArtistOnly,
                onToggleNowPlaying = AppSettings::setLastfmNowPlaying,
                onSignOut = {
                    AppSettings.setLastfmSessionKey("")
                    AppSettings.setLastfmUsername("")
                    AppSettings.setLastfmEnabled(false)
                    AppSettings.setLastfmScrobbleEnabled(false)
                    AppSettings.setLastfmNowPlaying(false)
                },
            )

            AnimatedVisibility(
                visible = lastfmEnabled && lastfmSessionKey.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    SectionHeader(stringResource(R.string.scrobble_timing))
                    ScrobbleTimingCard(
                        minDuration = scrobbleMinDuration,
                        delayPercent = scrobbleDelayPercent,
                        delaySeconds = scrobbleDelaySeconds,
                        onMinDurationChange = AppSettings::setScrobbleMinDuration,
                        onDelayPercentChange = AppSettings::setScrobbleDelayPercent,
                        onDelaySecondsChange = AppSettings::setScrobbleDelaySeconds,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            InfoBannerCard(stringResource(R.string.scrobbling_footer))
        } else {
            DisabledScrobblingCards()
            Spacer(Modifier.height(8.dp))
            InfoBannerCard(stringResource(R.string.scrobbling_paused_footer))
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun AccountHeroSection(
    signedIn: Boolean,
    account: Account?,
    channelName: String?,
    onSignIn: () -> Unit,
    onSwitchChannel: () -> Unit,
    onSignOut: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = if (signedIn) onSwitchChannel else onSignIn)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (account?.thumbnailUrl != null) {
                    AsyncImage(
                        model = account.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account?.name
                                ?: stringResource(if (signedIn) R.string.signed_in else R.string.not_signed_in),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(
                            text = stringResource(if (signedIn) R.string.connected else R.string.not_signed_in),
                            color = if (signedIn) NevTuneColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = account?.email?.takeIf { it.isNotBlank() }
                            ?: stringResource(
                                if (signedIn) R.string.youtube_music_account else R.string.tap_to_sign_in_google,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (signedIn) {
                Spacer(Modifier.height(14.dp))
                CardInnerDivider()
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                        .clickable(onClick = onSwitchChannel)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(
                        icon = Icons.Rounded.SwitchAccount,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 36.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.listen_as),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = channelName ?: stringResource(R.string.default_youtube_profile),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sign_out),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onSignIn,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.sign_in),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    if (signedIn) {
        Text(
            text = stringResource(R.string.account_profiles_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SpotifyIntegrationCard(
    connected: Boolean,
    onOpenSpotify: () -> Unit,
    onDisconnect: () -> Unit,
) {
    IntegrationCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSpotify)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                painter = painterResource(Res.drawable.spotify_logo),
                tint = SPOTIFY_GREEN,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.spotify),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(
                        text = stringResource(if (connected) R.string.connected else R.string.tap_to_connect),
                        color = if (connected) SPOTIFY_GREEN else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.spotify_connect_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = connected,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                CardInnerDivider(Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDisconnect)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LinkOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.spotify_disconnect),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscordIntegrationCard(
    connected: Boolean,
    rpcEnabled: Boolean,
    username: String,
    onClick: () -> Unit,
) {
    val active = connected && rpcEnabled
    IntegrationCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                icon = ImageVector.vectorResource(R.drawable.ic_discord),
                tint = DISCORD_BLURPLE,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Discord",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(
                        text = when {
                            active -> stringResource(R.string.connected)
                            connected -> stringResource(R.string.connected_presence_off)
                            else -> stringResource(R.string.tap_to_connect)
                        },
                        color = when {
                            active -> DISCORD_BLURPLE
                            connected -> NevTuneColors.warning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when {
                        !connected -> stringResource(R.string.rich_presence_footer)
                        !rpcEnabled -> stringResource(R.string.connected_presence_off)
                        username.isNotEmpty() -> stringResource(R.string.sharing_as, username)
                        else -> stringResource(R.string.sharing_listens)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ListenBrainzCard(
    enabled: Boolean,
    token: String,
    primaryArtistOnly: Boolean,
    onOpenLogin: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onTogglePrimaryArtistOnly: (Boolean) -> Unit,
) {
    val active = enabled && token.isNotBlank()
    IntegrationCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenLogin)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                icon = Icons.Rounded.Cloud,
                tint = LISTENBRAINZ_ORANGE,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ListenBrainz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (active) {
                        StatusBadge(
                            text = stringResource(R.string.connected),
                            color = LISTENBRAINZ_ORANGE,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (active) {
                        stringResource(R.string.connected)
                    } else {
                        stringResource(R.string.enter_token_to_enable)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        AnimatedVisibility(
            visible = active,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                CardInnerDivider(Modifier.padding(horizontal = 16.dp))
                CardToggleRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = stringResource(R.string.scrobble_primary_artist_only),
                    subtitle = stringResource(R.string.scrobble_primary_artist_only_subtitle),
                    checked = primaryArtistOnly,
                    onCheckedChange = onTogglePrimaryArtistOnly,
                )
            }
        }
    }
}

@Composable
private fun LastfmCard(
    enabled: Boolean,
    username: String,
    sessionKey: String,
    scrobbleEnabled: Boolean,
    nowPlayingEnabled: Boolean,
    primaryArtistOnly: Boolean,
    onOpenLogin: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleScrobble: (Boolean) -> Unit,
    onTogglePrimaryArtistOnly: (Boolean) -> Unit,
    onToggleNowPlaying: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    val signedIn = sessionKey.isNotBlank()
    val active = enabled && signedIn
    IntegrationCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    if (signedIn) onToggleEnabled(!enabled) else onOpenLogin()
                })
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                icon = Icons.Rounded.History,
                tint = LASTFM_RED,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Last.fm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (signedIn) {
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(
                            text = if (active) stringResource(R.string.connected) else stringResource(R.string.signed_in),
                            color = if (active) LASTFM_RED else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (signedIn) {
                        stringResource(R.string.signed_in_as, username)
                    } else {
                        stringResource(R.string.tap_to_sign_in)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        AnimatedVisibility(
            visible = active,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                CardInnerDivider(Modifier.padding(horizontal = 16.dp))
                CardToggleRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = stringResource(R.string.scrobble_tracks),
                    subtitle = stringResource(R.string.scrobble_tracks_subtitle),
                    checked = scrobbleEnabled,
                    onCheckedChange = onToggleScrobble,
                )
                CardInnerDivider(Modifier.padding(start = 54.dp, end = 16.dp))
                CardToggleRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = stringResource(R.string.scrobble_primary_artist_only),
                    subtitle = stringResource(R.string.scrobble_primary_artist_only_subtitle),
                    checked = primaryArtistOnly,
                    onCheckedChange = onTogglePrimaryArtistOnly,
                )
                CardInnerDivider(Modifier.padding(start = 54.dp, end = 16.dp))
                CardToggleRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = stringResource(R.string.now_playing),
                    subtitle = stringResource(R.string.lastfm_now_playing_subtitle),
                    checked = nowPlayingEnabled,
                    onCheckedChange = onToggleNowPlaying,
                )
            }
        }

        AnimatedVisibility(
            visible = signedIn,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                CardInnerDivider(Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSignOut)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sign_out),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrobbleTimingCard(
    minDuration: Int,
    delayPercent: Float,
    delaySeconds: Int,
    onMinDurationChange: (Int) -> Unit,
    onDelayPercentChange: (Float) -> Unit,
    onDelaySecondsChange: (Int) -> Unit,
) {
    IntegrationCard {
        CardSliderItem(
            icon = Icons.Rounded.Tune,
            title = stringResource(R.string.min_song_duration),
            subtitle = stringResource(R.string.min_song_duration_subtitle),
            valueLabel = "${minDuration}s",
            value = minDuration.toFloat(),
            onValueChange = { onMinDurationChange(it.roundToInt()) },
            valueRange = 15f..120f,
            steps = 20,
        )
        CardInnerDivider(Modifier.padding(horizontal = 16.dp))
        CardSliderItem(
            icon = Icons.Rounded.Tune,
            title = stringResource(R.string.scrobble_delay),
            subtitle = stringResource(R.string.scrobble_delay_subtitle),
            valueLabel = "${(delayPercent * 100).roundToInt()}%",
            value = delayPercent,
            onValueChange = onDelayPercentChange,
            valueRange = 0.1f..1.0f,
            steps = 8,
        )
        CardInnerDivider(Modifier.padding(horizontal = 16.dp))
        CardSliderItem(
            icon = Icons.Rounded.Tune,
            title = stringResource(R.string.max_delay),
            subtitle = stringResource(R.string.max_delay_subtitle),
            valueLabel = "${delaySeconds}s",
            value = delaySeconds.toFloat(),
            onValueChange = { onDelaySecondsChange(it.roundToInt()) },
            valueRange = 30f..300f,
            steps = 26,
        )
    }
}

@Composable
private fun DisabledScrobblingCards() {
    IntegrationCard(modifier = Modifier.alpha(0.6f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon = Icons.Rounded.Cloud, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ListenBrainz",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.back_in_future_version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CardInnerDivider(Modifier.padding(horizontal = 16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon = Icons.Rounded.History, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Last.fm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.back_in_future_version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IntegrationCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .animateContentSize(),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun CardToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun CardSliderItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusBadge(text = valueLabel, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun IconBadge(
    tint: Color,
    icon: ImageVector? = null,
    painter: Painter? = null,
    size: androidx.compose.ui.unit.Dp = 42.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(size * 0.55f),
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun InfoBannerCard(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun CardInnerDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    )
}
