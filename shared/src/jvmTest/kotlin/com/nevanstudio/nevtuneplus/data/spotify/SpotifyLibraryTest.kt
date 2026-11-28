// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.data.spotify

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpotifyLibraryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun playlistsIncludeFolderLeaves() {
        val root = json.parseToJsonElement(
            """
            {"data":{"me":{"libraryV3":{"totalCount":1,"items":[
              {"item":{"__typename":"PlaylistResponseWrapper","_uri":"spotify:playlist:abc","data":{
                "__typename":"Playlist","name":"Road","ownerV2":{"data":{"name":"Amit"}},
                "images":{"items":[{"sources":[{"url":"https://i.scdn.co/x"}]}]}
              }}}
            ]}}}}
            """.trimIndent(),
        ).jsonObject
        val (items, total) = parsePlaylistPage(root)
        assertEquals(1, total)
        assertEquals("abc", items.single().id)
        assertEquals("Road", items.single().name)
        assertEquals("Amit", items.single().owner)
        assertEquals("https://i.scdn.co/x", items.single().imageUrl)
    }

    @Test
    fun tracksReadTitleArtistAndDuration() {
        val root = json.parseToJsonElement(
            """
            {"data":{"playlistV2":{"content":{"totalCount":1,"items":[
              {"itemV2":{"data":{
                "name":"Song","uri":"spotify:track:t1",
                "artists":{"items":[{"uri":"spotify:artist:a","profile":{"name":"Artist"}}]},
                "albumOfTrack":{"name":"Album","coverArt":{"sources":[{"url":"https://img"}]}},
                "duration":{"totalMilliseconds":180000}
              }}}
            ]}}}}
            """.trimIndent(),
        ).jsonObject
        val track = parseTrackPage(root).first.single()
        assertEquals("t1", track.id)
        assertEquals("Song", track.title)
        assertEquals("Artist", track.artist)
        assertEquals("Album", track.album)
        assertEquals(180000, track.durationMs)
        assertEquals("https://img", track.imageUrl)
    }

    @Test
    fun playlistIdsComeFromLinksAndUris() {
        val id = "37i9dQZF1DXcBWIGoYBM5M"
        assertEquals(id, SpotifyImporter.extractPlaylistId("https://open.spotify.com/playlist/$id"))
        assertEquals(id, SpotifyImporter.extractPlaylistId("  https://open.spotify.com/playlist/$id?si=abc123  "))
        assertEquals(id, SpotifyImporter.extractPlaylistId("https://open.spotify.com/intl-id/playlist/$id"))
        assertEquals(id, SpotifyImporter.extractPlaylistId("spotify:playlist:$id"))
        assertNull(SpotifyImporter.extractPlaylistId("https://open.spotify.com/album/$id"))
        assertNull(SpotifyImporter.extractPlaylistId("https://notspotify.com/playlist/$id"))
        assertNull(SpotifyImporter.extractPlaylistId("ftp://open.spotify.com/playlist/$id"))
        assertNull(SpotifyImporter.extractPlaylistId("https://open.spotify.com/playlist/short"))
        assertNull(SpotifyImporter.extractPlaylistId(""))
    }

    @Test
    fun parsesSavedAlbumsFromPathfinderAndRest() {
        val gqlRoot = json.parseToJsonElement(
            """
            {"data":{"me":{"libraryV3":{"items":[
              {"item":{"__typename":"AlbumResponseWrapper","_uri":"spotify:album:alb1","data":{
                "__typename":"Album","name":"Midnight","date":{"year":2024},
                "artists":{"items":[{"profile":{"name":"Artist One"}}]},
                "coverArt":{"sources":[{"url":"https://i.scdn.co/small","width":64},{"url":"https://i.scdn.co/large","width":640}]}
              }}}
            ]}}}}
            """.trimIndent(),
        ).jsonObject
        val gqlAlbums = parseAlbumLibraryPage(gqlRoot)
        assertEquals(1, gqlAlbums.size)
        assertEquals("alb1", gqlAlbums.single().id)
        assertEquals("Midnight", gqlAlbums.single().name)
        assertEquals("Artist One", gqlAlbums.single().artist)
        assertEquals("2024", gqlAlbums.single().releaseYear)
        assertEquals("https://i.scdn.co/large", gqlAlbums.single().imageUrl)

        val restRoot = json.parseToJsonElement(
            """
            {"items":[{"album":{
              "id":"alb2","name":"Daybreak","release_date":"2023-05-12",
              "artists":[{"name":"Artist Two"}],
              "images":[{"url":"https://i.scdn.co/alb2","width":300}]
            }}]}
            """.trimIndent(),
        ).jsonObject
        val restAlbums = parseRestAlbumsPage(restRoot)
        assertEquals("alb2", restAlbums.single().id)
        assertEquals("2023", restAlbums.single().releaseYear)
    }

    @Test
    fun filtersPersonalizedSearchToOfficialSpotifyPlaylistsAndOrdersMixes() {
        val searchRoot = json.parseToJsonElement(
            """
            {"playlists":{"items":[
              {"id":"fake1","name":"Discover Weekly","owner":{"id":"random_user","display_name":"Clone"},"images":[]},
              {"id":"dw1","name":"Discover Weekly","owner":{"id":"spotify","display_name":"Spotify"},"images":[{"url":"https://img/dw","width":300}]},
              {"id":"dm1","name":"Daily Mix 1","owner":{"id":"spotify","display_name":"Spotify"},"images":[]},
              {"id":"rr1","name":"Release Radar","owner":{"id":"spotify","display_name":"Spotify"},"images":[]}
            ]}}
            """.trimIndent(),
        ).jsonObject
        val parsed = parsePersonalizedSearchPlaylists(searchRoot)
        assertEquals(setOf("dw1", "dm1", "rr1"), parsed.map { it.id }.toSet())
        assertFalse(parsed.any { it.id == "fake1" })

        val ordered = orderPersonalizedMixes(parsed.reversed())
        assertEquals(listOf("Discover Weekly", "Release Radar", "Daily Mix 1"), ordered.map { it.name })
    }

    @Test
    fun deduplicatesSpotifyTracksAcrossCollectionsByIdAndMetadata() {
        val seen = HashSet<String>()
        val firstBatch = listOf(
            SpotifyTrack("t1", "Hello", "Artist A", "Alb", 180000, null),
            SpotifyTrack("t1", "Hello", "Artist A", "Alb", 180000, null),
        )
        val secondBatch = listOf(
            SpotifyTrack("t2", "hello ", "artist a", "Alb Deluxe", 180000, null),
            SpotifyTrack("t3", "World", "Artist B", "Alb 2", 200000, null),
        )
        val deduped1 = deduplicateSpotifyTracks(firstBatch, seen)
        val deduped2 = deduplicateSpotifyTracks(secondBatch, seen)
        assertEquals(listOf("t1"), deduped1.map { it.id })
        assertEquals(listOf("t3"), deduped2.map { it.id })
        assertTrue(isSpotifyPersonalizedPlaylist(SpotifyPlaylist("1", "Daily Mix 2", "Spotify", null)))
        assertFalse(isSpotifyPersonalizedPlaylist(SpotifyPlaylist("2", "Daily Mix 2", "Alex", null)))
    }
}
