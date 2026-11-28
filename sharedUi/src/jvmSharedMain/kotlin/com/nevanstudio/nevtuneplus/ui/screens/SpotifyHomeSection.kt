// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nevanstudio.nevtuneplus.data.model.ShelfItem
import com.nevanstudio.nevtuneplus.data.model.Song
import com.nevanstudio.nevtuneplus.data.model.stableShelfComposeKeys
import com.nevanstudio.nevtuneplus.data.spotify.SPOTIFY_ALBUM_PREFIX
import com.nevanstudio.nevtuneplus.data.spotify.SPOTIFY_PAGE_PREFIX
import com.nevanstudio.nevtuneplus.data.spotify.SpotifyAlbum
import com.nevanstudio.nevtuneplus.data.spotify.SpotifyHomeFeed
import com.nevanstudio.nevtuneplus.data.spotify.SpotifyPlaylist
import com.nevanstudio.nevtuneplus.data.spotify.SpotifyTrack
import com.nevanstudio.nevtuneplus.sharedui.resources.Res
import com.nevanstudio.nevtuneplus.sharedui.resources.from_your_spotify
import com.nevanstudio.nevtuneplus.sharedui.resources.playlist
import com.nevanstudio.nevtuneplus.sharedui.resources.spotify_liked_songs
import com.nevanstudio.nevtuneplus.sharedui.resources.spotify_made_for_you
import com.nevanstudio.nevtuneplus.sharedui.resources.spotify_made_for_you_subtitle
import com.nevanstudio.nevtuneplus.sharedui.resources.spotify_playlists_label
import com.nevanstudio.nevtuneplus.sharedui.resources.spotify_recently_played
import com.nevanstudio.nevtuneplus.sharedui.resources.spotify_saved_albums
import com.nevanstudio.nevtuneplus.sharedui.resources.spotify_see_all
import com.nevanstudio.nevtuneplus.sharedui.resources.spotify_top_tracks
import com.nevanstudio.nevtuneplus.sharedui.resources.spotify_logo
import com.nevanstudio.nevtuneplus.ui.components.PAGE_GUTTER
import com.nevanstudio.nevtuneplus.ui.components.ShelfRow
import com.nevanstudio.nevtuneplus.ui.components.trackColumnWidth
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val SPOTIFY_TRACKS_PER_COLUMN = 3

internal fun SpotifyPlaylist.toHomeShelfItem(playlistLabel: String): ShelfItem = ShelfItem(
    title = name,
    subtitle = owner?.takeIf { it.isNotBlank() } ?: playlistLabel,
    thumbnailUrl = imageUrl,
    videoId = null,
    browseId = SPOTIFY_PAGE_PREFIX + id,
)

internal fun SpotifyAlbum.toHomeShelfItem(): ShelfItem = ShelfItem(
    title = name,
    subtitle = listOfNotNull(artist.takeIf { it.isNotBlank() }, releaseYear).joinToString(" • "),
    thumbnailUrl = imageUrl,
    videoId = null,
    browseId = SPOTIFY_ALBUM_PREFIX + id,
)

internal fun SpotifyHomeFeed.hasVisibleLibraryFor(filter: HomeContentFilter): Boolean = when (filter) {
    HomeContentFilter.ALL -> hasLibraryContent
    HomeContentFilter.PLAYLISTS -> playlists.isNotEmpty()
    HomeContentFilter.SONGS -> likedSongs.isNotEmpty()
    HomeContentFilter.ALBUMS -> savedAlbums.isNotEmpty()
    HomeContentFilter.ARTISTS -> false
}

internal fun SpotifyHomeFeed.hasVisiblePersonalizedFor(filter: HomeContentFilter): Boolean = when (filter) {
    HomeContentFilter.ALL -> hasPersonalizedContent
    HomeContentFilter.PLAYLISTS -> personalizedMixes.isNotEmpty()
    HomeContentFilter.SONGS -> recentlyPlayed.isNotEmpty() || topTracks.isNotEmpty()
    HomeContentFilter.ALBUMS, HomeContentFilter.ARTISTS -> false
}

internal fun LazyListScope.spotifyHomeSections(
    feed: SpotifyHomeFeed?,
    activeFilter: HomeContentFilter,
    currentSong: Song?,
    isPlaying: Boolean,
    resolvingTrackId: String?,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    onSpotifyTrackClick: ((SpotifyTrack, String) -> Unit)?,
    onSpotifyTrackLongPress: ((SpotifyTrack) -> Unit)?,
    onOpenSpotifyLibrary: (() -> Unit)?,
) {
    if (feed == null || feed.isEmpty) return

    if (feed.hasVisibleLibraryFor(activeFilter)) {
        item(key = "home-from-your-spotify") {
            FromYourSpotifySection(
                feed = feed,
                activeFilter = activeFilter,
                currentSong = currentSong,
                isPlaying = isPlaying,
                resolvingTrackId = resolvingTrackId,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                onSpotifyTrackClick = onSpotifyTrackClick,
                onSpotifyTrackLongPress = onSpotifyTrackLongPress,
                onOpenSpotifyLibrary = onOpenSpotifyLibrary,
            )
        }
    }

    if (feed.personalizedMixes.isNotEmpty() &&
        (activeFilter == HomeContentFilter.ALL || activeFilter == HomeContentFilter.PLAYLISTS)
    ) {
        item(key = "home-spotify-made-for-you") {
            SpotifyMadeForYouSection(
                mixes = feed.personalizedMixes,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
            )
        }
    }

    if (feed.recentlyPlayed.isNotEmpty() &&
        (activeFilter == HomeContentFilter.ALL || activeFilter == HomeContentFilter.SONGS)
    ) {
        item(key = "home-spotify-recently-played") {
            val sectionTitle = stringResource(Res.string.spotify_recently_played)
            SpotifyTrackCarouselSection(
                title = sectionTitle,
                tracks = feed.recentlyPlayed,
                currentSong = currentSong,
                isPlaying = isPlaying,
                resolvingTrackId = resolvingTrackId,
                onTrackClick = { track -> onSpotifyTrackClick?.invoke(track, sectionTitle) },
                onTrackLongPress = onSpotifyTrackLongPress,
            )
        }
    }

    if (feed.topTracks.isNotEmpty() &&
        (activeFilter == HomeContentFilter.ALL || activeFilter == HomeContentFilter.SONGS)
    ) {
        item(key = "home-spotify-top-tracks") {
            val sectionTitle = stringResource(Res.string.spotify_top_tracks)
            SpotifyTrackCarouselSection(
                title = sectionTitle,
                tracks = feed.topTracks,
                currentSong = currentSong,
                isPlaying = isPlaying,
                resolvingTrackId = resolvingTrackId,
                onTrackClick = { track -> onSpotifyTrackClick?.invoke(track, sectionTitle) },
                onTrackLongPress = onSpotifyTrackLongPress,
            )
        }
    }
}

@Composable
private fun FromYourSpotifySection(
    feed: SpotifyHomeFeed,
    activeFilter: HomeContentFilter,
    currentSong: Song?,
    isPlaying: Boolean,
    resolvingTrackId: String?,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    onSpotifyTrackClick: ((SpotifyTrack, String) -> Unit)?,
    onSpotifyTrackLongPress: ((SpotifyTrack) -> Unit)?,
    onOpenSpotifyLibrary: (() -> Unit)?,
) {
    val sectionTitle = stringResource(Res.string.from_your_spotify)
    val playlistLabel = stringResource(Res.string.playlist)
    val showPlaylists = feed.playlists.isNotEmpty() &&
        (activeFilter == HomeContentFilter.ALL || activeFilter == HomeContentFilter.PLAYLISTS)
    val showLikedSongs = feed.likedSongs.isNotEmpty() &&
        (activeFilter == HomeContentFilter.ALL || activeFilter == HomeContentFilter.SONGS)
    val showSavedAlbums = feed.savedAlbums.isNotEmpty() &&
        (activeFilter == HomeContentFilter.ALL || activeFilter == HomeContentFilter.ALBUMS)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = HOME_SECTION_SPACING),
    ) {
        FromYourSpotifyHeader(
            title = sectionTitle,
            onSeeAll = onOpenSpotifyLibrary,
        )

        if (showPlaylists) {
            val playlistItems = remember(feed.playlists, playlistLabel) {
                feed.playlists.map { it.toHomeShelfItem(playlistLabel) }
            }
            val itemKeys = remember(playlistItems) { playlistItems.stableShelfComposeKeys() }
            if (showLikedSongs || showSavedAlbums) {
                SpotifySubSectionLabel(stringResource(Res.string.spotify_playlists_label))
            }
            ShelfRow(
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(HOME_CARD_SPACING),
            ) {
                itemsIndexed(playlistItems, key = { index, _ -> "sp_pl:${itemKeys[index]}" }) { _, item ->
                    CompactDiscoveryCard(
                        item = item,
                        onClick = { onItemClick(item, sectionTitle) },
                        onLongPress = onItemLongPress?.let { { it(item) } },
                        width = HOME_COMPACT_CARD_WIDTH,
                        tonalContainer = false,
                        isCurrent = false,
                        isPlaying = false,
                    )
                }
            }
        }

        if (showLikedSongs) {
            if (showPlaylists) Spacer(Modifier.height(12.dp))
            val likedTitle = stringResource(Res.string.spotify_liked_songs)
            SpotifySubSectionLabel(likedTitle)
            SpotifyTrackColumns(
                tracks = feed.likedSongs,
                currentSong = currentSong,
                isPlaying = isPlaying,
                resolvingTrackId = resolvingTrackId,
                onTrackClick = { track -> onSpotifyTrackClick?.invoke(track, likedTitle) },
                onTrackLongPress = onSpotifyTrackLongPress,
            )
        }

        if (showSavedAlbums) {
            if (showPlaylists || showLikedSongs) Spacer(Modifier.height(12.dp))
            val albumItems = remember(feed.savedAlbums) {
                feed.savedAlbums.map { it.toHomeShelfItem() }
            }
            val albumKeys = remember(albumItems) { albumItems.stableShelfComposeKeys() }
            SpotifySubSectionLabel(stringResource(Res.string.spotify_saved_albums))
            ShelfRow(
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(HOME_CARD_SPACING),
            ) {
                itemsIndexed(albumItems, key = { index, _ -> "sp_al:${albumKeys[index]}" }) { _, item ->
                    CompactDiscoveryCard(
                        item = item,
                        onClick = { onItemClick(item, sectionTitle) },
                        onLongPress = onItemLongPress?.let { { it(item) } },
                        width = HOME_COMPACT_CARD_WIDTH,
                        tonalContainer = false,
                        isCurrent = false,
                        isPlaying = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun FromYourSpotifyHeader(
    title: String,
    onSeeAll: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.spotify_logo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onSeeAll != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                modifier = Modifier
                    .padding(start = 10.dp)
                    .defaultMinSize(minHeight = 32.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onSeeAll),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.spotify_see_all),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifySubSectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 4.dp),
    )
}

@Composable
private fun SpotifyMadeForYouSection(
    mixes: List<SpotifyPlaylist>,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
) {
    val sectionTitle = stringResource(Res.string.spotify_made_for_you)
    val sectionSubtitle = stringResource(Res.string.spotify_made_for_you_subtitle)
    val playlistLabel = stringResource(Res.string.playlist)
    val mixItems = remember(mixes, playlistLabel) {
        mixes.map { it.toHomeShelfItem(playlistLabel) }
    }
    val keys = remember(mixItems) { mixItems.stableShelfComposeKeys() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = HOME_SECTION_SPACING),
    ) {
        SectionHeader(
            title = sectionTitle,
            subtitle = sectionSubtitle,
        )
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(HOME_CARD_SPACING),
        ) {
            itemsIndexed(mixItems, key = { index, _ -> "sp_mix:${keys[index]}" }) { _, item ->
                CompactDiscoveryCard(
                    item = item,
                    onClick = { onItemClick(item, sectionTitle) },
                    onLongPress = onItemLongPress?.let { { it(item) } },
                    width = HOME_TONAL_CARD_WIDTH,
                    tonalContainer = true,
                    isCurrent = false,
                    isPlaying = false,
                )
            }
        }
    }
}

@Composable
private fun SpotifyTrackCarouselSection(
    title: String,
    tracks: List<SpotifyTrack>,
    currentSong: Song?,
    isPlaying: Boolean,
    resolvingTrackId: String?,
    onTrackClick: (SpotifyTrack) -> Unit,
    onTrackLongPress: ((SpotifyTrack) -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = HOME_SECTION_SPACING),
    ) {
        SectionHeader(title = title)
        SpotifyTrackColumns(
            tracks = tracks,
            currentSong = currentSong,
            isPlaying = isPlaying,
            resolvingTrackId = resolvingTrackId,
            onTrackClick = onTrackClick,
            onTrackLongPress = onTrackLongPress,
        )
    }
}

@Composable
private fun SpotifyTrackColumns(
    tracks: List<SpotifyTrack>,
    currentSong: Song?,
    isPlaying: Boolean,
    resolvingTrackId: String?,
    onTrackClick: (SpotifyTrack) -> Unit,
    onTrackLongPress: ((SpotifyTrack) -> Unit)?,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnWidth = if (maxWidth >= 480.dp) {
            minOf(maxWidth * 0.44f, 290.dp)
        } else {
            trackColumnWidth(maxWidth)
        }
        val columns = remember(tracks) { tracks.chunked(SPOTIFY_TRACKS_PER_COLUMN) }
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                columns,
                key = { column -> column.joinToString("|") { it.id.ifBlank { "${it.title}:${it.artist}" } } },
            ) { column ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.width(columnWidth),
                ) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        column.forEach { track ->
                            val active = isSpotifyTrackPlaying(track, currentSong)
                            val resolving = resolvingTrackId == track.id
                            Box(contentAlignment = Alignment.CenterEnd) {
                                CompactTrackRow(
                                    title = track.title,
                                    subtitle = track.artist,
                                    thumbnailUrl = track.imageUrl,
                                    onClick = { onTrackClick(track) },
                                    onLongPress = onTrackLongPress?.let { { it(track) } },
                                    isCurrent = active,
                                    isPlaying = active && isPlaying,
                                )
                                if (resolving) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier
                                            .padding(end = 38.dp)
                                            .size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isSpotifyTrackPlaying(track: SpotifyTrack, currentSong: Song?): Boolean {
    currentSong ?: return false
    return currentSong.title.equals(track.title, ignoreCase = true) &&
        (currentSong.artist.equals(track.artist, ignoreCase = true) ||
            currentSong.artist.contains(track.artist.substringBefore(",").trim(), ignoreCase = true))
}
