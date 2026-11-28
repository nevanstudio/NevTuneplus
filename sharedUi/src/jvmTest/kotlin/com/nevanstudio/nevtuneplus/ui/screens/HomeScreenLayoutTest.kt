// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.screens

import com.nevanstudio.nevtuneplus.data.model.HomeShelf
import com.nevanstudio.nevtuneplus.data.model.ShelfItem
import com.nevanstudio.nevtuneplus.data.model.ShelfLayout
import com.nevanstudio.nevtuneplus.data.spotify.SPOTIFY_ALBUM_PREFIX
import com.nevanstudio.nevtuneplus.data.spotify.SPOTIFY_PAGE_PREFIX
import com.nevanstudio.nevtuneplus.data.spotify.SpotifyAlbum
import com.nevanstudio.nevtuneplus.data.spotify.SpotifyHomeFeed
import com.nevanstudio.nevtuneplus.data.spotify.SpotifyPlaylist
import com.nevanstudio.nevtuneplus.data.spotify.SpotifyTrack
import com.nevanstudio.nevtuneplus.ui.components.SHELF_CARD_WIDTH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenLayoutTest {

    @Test
    fun homeCardsAreSignificantlyMoreCompactThanLegacyShelfCards() {
        assertTrue(HOME_COMPACT_CARD_WIDTH < SHELF_CARD_WIDTH)
        assertTrue(HOME_TONAL_CARD_WIDTH < SHELF_CARD_WIDTH)
        assertTrue(HOME_ARTIST_CIRCLE_SIZE < HOME_COMPACT_CARD_WIDTH)
    }

    @Test
    fun itemClassificationIdentifiesArtistsAlbumsPlaylistsAndSongs() {
        val song = ShelfItem("Song", "Artist • 3:20", null, videoId = "v1", browseId = null)
        val artist = ShelfItem("Artist Name", "Artist", null, videoId = null, browseId = "UC12345")
        val album = ShelfItem("Album Name", "Album • 2025", null, videoId = null, browseId = "MPREb_123")
        val playlist = ShelfItem("Chill Mix", "Playlist • NevTune", null, videoId = null, browseId = "VLPL123")

        assertTrue(song.matchesHomeFilter(HomeContentFilter.SONGS))
        assertFalse(song.matchesHomeFilter(HomeContentFilter.ALBUMS))

        assertTrue(artist.isArtistItem())
        assertTrue(artist.matchesHomeFilter(HomeContentFilter.ARTISTS))

        assertTrue(album.isAlbumItem())
        assertTrue(album.matchesHomeFilter(HomeContentFilter.ALBUMS))

        assertTrue(playlist.isPlaylistItem())
        assertTrue(playlist.matchesHomeFilter(HomeContentFilter.PLAYLISTS))
    }

    @Test
    fun availableFiltersIncludeOnlyPresentContentCategories() {
        val shelves = listOf(
            HomeShelf("Recents", listOf(ShelfItem("Track 1", "Artist", null, "v1", null))),
            HomeShelf("Albums", listOf(ShelfItem("Album 1", "Album", null, null, "MPREb_1"))),
        )
        val filters = availableHomeFilters(shelves, timeAwareShelf = null)
        assertEquals(
            listOf(HomeContentFilter.ALL, HomeContentFilter.SONGS, HomeContentFilter.ALBUMS),
            filters,
        )
    }

    @Test
    fun filterHomeShelvesKeepsOnlyMatchingItemsAndDropsEmptyShelves() {
        val shelves = listOf(
            HomeShelf(
                "Mixed",
                listOf(
                    ShelfItem("Track 1", "Artist", null, "v1", null),
                    ShelfItem("Album 1", "Album", null, null, "MPREb_1"),
                ),
            ),
            HomeShelf("Artists", listOf(ShelfItem("Artist 1", "Artist", null, null, "UC1"))),
        )
        val albumsOnly = filterHomeShelves(shelves, HomeContentFilter.ALBUMS)
        assertEquals(1, albumsOnly.size)
        assertEquals("Mixed", albumsOnly.single().title)
        assertEquals(listOf("Album 1"), albumsOnly.single().items.map { it.title })
    }

    @Test
    fun quickAccessShortcutsInterleaveRecentsAndCollectionsWithoutDuplicates() {
        val recents = HomeShelf(
            "Recents",
            listOf(
                ShelfItem("Song 1", "A", null, "v1", null),
                ShelfItem("Song 2", "B", null, "v2", null),
                ShelfItem("Song 3", "C", null, "v3", null),
            ),
        )
        val mixes = HomeShelf(
            "Mixed for you",
            listOf(
                ShelfItem("Mix 1", "Playlist", null, null, "VL1"),
                ShelfItem("Mix 2", "Playlist", null, null, "VL2"),
                ShelfItem("Song 1", "A", null, "v1", null),
                ShelfItem("Mix 3", "Playlist", null, null, "VL3"),
            ),
        )
        val shortcuts = selectQuickAccessShortcuts(listOf(recents, mixes), timeAwareShelf = null, maxItems = 6)
        assertEquals(6, shortcuts.size)
        assertEquals(
            setOf("v1", "v2", "v3", "VL1", "VL2", "VL3"),
            shortcuts.map { it.item.videoId ?: it.item.browseId }.toSet(),
        )
    }

    @Test
    fun presentationStyleAdaptsToArtistsVideosAndCuratedShelves() {
        val artists = HomeShelf(
            "Recommended artists",
            listOf(ShelfItem("A1", "Artist", null, null, "UC1")),
        )
        val videos = HomeShelf(
            "Music videos",
            listOf(ShelfItem("V1", "Artist", null, "v1", null, isVideo = true)),
            layout = ShelfLayout.VIDEO,
        )
        val madeForYou = HomeShelf(
            "Made for you",
            listOf(ShelfItem("P1", "Playlist", null, null, "VL1")),
        )
        val albums = HomeShelf(
            "New releases",
            listOf(ShelfItem("AL1", "Album", null, null, "MPREb_1")),
        )

        assertEquals(ShelfPresentationStyle.ARTIST_CIRCLES, shelfPresentationStyle(artists))
        assertEquals(ShelfPresentationStyle.VIDEO_CARDS, shelfPresentationStyle(videos))
        assertEquals(ShelfPresentationStyle.TONAL_CARDS, shelfPresentationStyle(madeForYou))
        assertEquals(ShelfPresentationStyle.COMPACT_CARDS, shelfPresentationStyle(albums))
    }

    @Test
    fun spotifyAlbumsAndPlaylistsParticipateInHomeFiltersAndQuickAccess() {
        val spotifyAlbum = ShelfItem("Midnight", "Artist", null, null, "${SPOTIFY_ALBUM_PREFIX}a1")
        val spotifyPlaylist = ShelfItem("Road Trip", "Me", null, null, "${SPOTIFY_PAGE_PREFIX}p1")
        assertTrue(spotifyAlbum.isAlbumItem())
        assertTrue(spotifyAlbum.matchesHomeFilter(HomeContentFilter.ALBUMS))
        assertTrue(spotifyPlaylist.isPlaylistItem())
        assertTrue(spotifyPlaylist.matchesHomeFilter(HomeContentFilter.PLAYLISTS))

        val feed = SpotifyHomeFeed(
            playlists = listOf(SpotifyPlaylist("p1", "Road Trip", "Me", null)),
            likedSongs = listOf(SpotifyTrack("t1", "Song 1", "Artist", "Alb", 180000, null)),
            savedAlbums = listOf(SpotifyAlbum("a1", "Midnight", "Artist", null, "2024")),
        )
        val filters = availableHomeFilters(emptyList(), null, spotifyHomeFeed = feed)
        assertEquals(
            listOf(HomeContentFilter.ALL, HomeContentFilter.SONGS, HomeContentFilter.PLAYLISTS, HomeContentFilter.ALBUMS),
            filters,
        )
        val shortcuts = selectQuickAccessShortcuts(emptyList(), null, maxItems = 6, spotifyHomeFeed = feed)
        assertEquals(2, shortcuts.size)
        assertEquals(setOf("${SPOTIFY_PAGE_PREFIX}p1", "${SPOTIFY_ALBUM_PREFIX}a1"), shortcuts.mapNotNull { it.item.browseId }.toSet())
    }
}
