// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.data.spotify

import com.nevanstudio.nevtuneplus.data.DebugLog as Log
import com.nevanstudio.nevtuneplus.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** A Spotify playlist's page id in the detail stack: this plus the playlist id (or [SpotifyLibrary.LIKED_ID]). */
const val SPOTIFY_PAGE_PREFIX = "spotify:playlist:"

/** A Spotify album's page id in the detail stack: this plus the album id. */
const val SPOTIFY_ALBUM_PREFIX = "spotify:album:"

/** Prefix for a Spotify track identity before YouTube Music matching. */
const val SPOTIFY_TRACK_PREFIX = "spotify:track:"

/** Thrown when Spotify rejects the session or no active token is available. */
class SpotifyAuthException(message: String) : IllegalStateException(message)

@Serializable
data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val owner: String?,
    val imageUrl: String?,
)

@Serializable
data class SpotifyTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Int,
    val imageUrl: String?,
)

@Serializable
data class SpotifyAlbum(
    val id: String,
    val name: String,
    val artist: String,
    val imageUrl: String?,
    val releaseYear: String? = null,
)

@Serializable
data class SpotifyHomeFeed(
    val playlists: List<SpotifyPlaylist> = emptyList(),
    val likedSongs: List<SpotifyTrack> = emptyList(),
    val savedAlbums: List<SpotifyAlbum> = emptyList(),
    val personalizedMixes: List<SpotifyPlaylist> = emptyList(),
    val recentlyPlayed: List<SpotifyTrack> = emptyList(),
    val topTracks: List<SpotifyTrack> = emptyList(),
) {
    val hasLibraryContent: Boolean
        get() = playlists.isNotEmpty() || likedSongs.isNotEmpty() || savedAlbums.isNotEmpty()

    val hasPersonalizedContent: Boolean
        get() = personalizedMixes.isNotEmpty() || recentlyPlayed.isNotEmpty() || topTracks.isNotEmpty()

    val isEmpty: Boolean
        get() = !hasLibraryContent && !hasPersonalizedContent
}

object SpotifyLibrary {
    /** The web player's bearer, and its client token when there is one. */
    class Tokens(val bearer: String, val clientToken: String?)

    /**
     * Where the tokens come from: the phone mints them in a WebView, the desktop in JavaFX's.
     * Null tokens mean signed out or expired.
     */
    fun interface Auth {
        suspend fun tokens(): Tokens?
    }

    /** Installed by each application at start-up; until then every call fails as signed out. */
    @Volatile
    var auth: Auth? = null

    /** Optional hook invoked when Spotify returns HTTP 401 so cached tokens are invalidated. */
    @Volatile
    var onAuthExpired: (() -> Unit)? = null

    private const val TAG = "SpotifyLibrary"
    private const val GQL = "https://api-partner.spotify.com/pathfinder/v2/query"
    private const val REST_BASE = "https://api.spotify.com/v1"
    private const val LIBRARY = "973e511ca44261fda7eebac8b653155e7caee3675abb4fb110cc1b8c78b091c3"
    private const val PLAYLIST = "346811f856fb0b7e4f6c59f8ebea78dd081c6e2fb01b77c954b26259d5fc6763"
    /** The pseudo-playlist id for the Liked Songs collection, which has no playlist uri. */
    const val LIKED_ID = "liked"
    /** Spotify's own Liked Songs artwork, the purple heart; the collection has no cover of its own. */
    const val LIKED_COVER = "https://misc.scdn.co/liked-songs/liked-songs-640.png"
    private const val LIKED = "087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240"
    /** The phone's canvas client sent this one; kept so its requests look unchanged. */
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/122.0.0.0 Safari/537.36"
    private const val HOME_LIBRARY_LIMIT = 25
    private const val HOME_PLAYLIST_LIMIT = 50
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /** Every playlist in the library, however many pages it takes. */
    suspend fun playlists(): List<SpotifyPlaylist> = withContext(Dispatchers.IO) {
        val headers = authHeaders()
        val collected = mutableListOf<SpotifyPlaylist>()
        var offset = 0
        while (true) {
            val (items, total, raw) = playlistPage(headers, offset)
            collected += items
            offset += raw
            if (raw == 0 || offset >= total) break
        }
        // Liked Songs is not a playlist to Spotify's library query, so it is
        // always offered first rather than depending on it turning up there.
        listOf(SpotifyPlaylist(id = LIKED_ID, name = "Liked Songs", owner = null, imageUrl = LIKED_COVER)) + collected
    }

    /**
     * Fetches the Home-screen Spotify payload: library playlists, recent liked songs,
     * saved albums, and any accessible Spotify personalized mixes/listening feeds.
     *
     * Retries once after invalidating cached tokens if Spotify answers with 401.
     */
    suspend fun homeFeed(): SpotifyHomeFeed = withContext(Dispatchers.IO) {
        try {
            fetchHomeFeed(authHeaders())
        } catch (authError: SpotifyAuthException) {
            onAuthExpired?.invoke()
            val refreshed = authHeaders()
            fetchHomeFeed(refreshed)
        }
    }

    private suspend fun fetchHomeFeed(headers: Map<String, String>): SpotifyHomeFeed = coroutineScope {
        val playlistsDeferred = async { fetchLibraryPlaylistsSafe(headers) }
        val likedDeferred = async { fetchLikedTracksSafe(headers, HOME_LIBRARY_LIMIT) }
        val albumsDeferred = async { fetchSavedAlbumsSafe(headers, HOME_LIBRARY_LIMIT) }
        val madeForYouDeferred = async { fetchMadeForYouSafe(headers) }
        val recentDeferred = async { fetchRecentlyPlayedSafe(headers) }
        val topDeferred = async { fetchTopTracksSafe(headers) }

        val rawPlaylists = playlistsDeferred.await()
        val likedTracks = likedDeferred.await()
        val savedAlbums = albumsDeferred.await()
        val madeForYouFromView = madeForYouDeferred.await()
        val rawRecent = recentDeferred.await()
        val rawTop = topDeferred.await()

        val libraryPersonalized = rawPlaylists.filter(::isSpotifyPersonalizedPlaylist)
        val combinedPersonalized = (madeForYouFromView + libraryPersonalized)
            .distinctBy { it.id }

        // If Made For You view wasn't accessible or missed core mixes, search with the
        // user's own authenticated token for Spotify-owned personalized mixes.
        val needsMixSearch = !hasCorePersonalizedMixes(combinedPersonalized)
        val searchedMixes = if (needsMixSearch) {
            fetchPersonalizedMixesViaSearch(headers)
        } else {
            emptyList()
        }

        val personalizedMixes = orderPersonalizedMixes(
            (combinedPersonalized + searchedMixes).distinctBy { it.id },
        )
        val personalizedIds = personalizedMixes.mapTo(HashSet()) { it.id }

        val libraryOnlyPlaylists = rawPlaylists
            .filterNot { it.id in personalizedIds }
            .distinctBy { it.id }

        val playlistsWithLiked = if (likedTracks.isNotEmpty()) {
            val likedCard = SpotifyPlaylist(
                id = LIKED_ID,
                name = "Liked Songs",
                owner = null,
                imageUrl = LIKED_COVER,
            )
            listOf(likedCard) + libraryOnlyPlaylists.filterNot { it.id == LIKED_ID }
        } else {
            libraryOnlyPlaylists
        }

        val seenTrackKeys = HashSet<String>()
        val dedupedLiked = deduplicateSpotifyTracks(likedTracks, seenTrackKeys)
        val dedupedRecent = deduplicateSpotifyTracks(rawRecent, seenTrackKeys)
        val dedupedTop = deduplicateSpotifyTracks(rawTop, seenTrackKeys)

        SpotifyHomeFeed(
            playlists = playlistsWithLiked,
            likedSongs = dedupedLiked,
            savedAlbums = savedAlbums.distinctBy { it.id },
            personalizedMixes = personalizedMixes,
            recentlyPlayed = dedupedRecent,
            topTracks = dedupedTop,
        )
    }

    /**
     * Every track of a playlist, however long. [onPage] gets the running list
     * after each page, so a thousand-song playlist fills in as it loads
     * rather than appearing all at once at the end.
     */
    suspend fun tracks(
        playlistId: String,
        onPage: (List<SpotifyTrack>) -> Unit = {},
    ): List<SpotifyTrack> = withContext(Dispatchers.IO) {
        val headers = authHeaders()
        val collected = mutableListOf<SpotifyTrack>()
        var offset = 0
        while (true) {
            val (items, total, raw) = if (playlistId == LIKED_ID) {
                likedPage(headers, offset)
            } else {
                trackPage(headers, playlistId, offset)
            }
            collected += items
            offset += raw
            onPage(collected.toList())
            if (raw == 0 || offset >= total) break
        }
        collected
    }

    /** Tracks and cover image for a saved Spotify album. */
    suspend fun albumDetail(albumId: String): Pair<String?, List<SpotifyTrack>> = withContext(Dispatchers.IO) {
        val headers = authHeaders()
        val body = restGet("$REST_BASE/albums/$albumId", headers)
            ?: throw IllegalStateException("Spotify album request failed")
        val root = json.parseToJsonElement(body).jsonObject
        parseRestAlbumDetail(root)
    }

    private fun fetchLibraryPlaylistsSafe(headers: Map<String, String>): List<SpotifyPlaylist> {
        val gqlResult = runCatching {
            val collected = mutableListOf<SpotifyPlaylist>()
            var offset = 0
            while (collected.size < HOME_PLAYLIST_LIMIT) {
                val (items, total, raw) = playlistPage(headers, offset)
                collected += items
                offset += raw
                if (raw == 0 || offset >= total) break
            }
            collected.toList()
        }
        if (gqlResult.isSuccess) return gqlResult.getOrThrow()
        if (gqlResult.exceptionOrNull() is SpotifyAuthException) throw gqlResult.exceptionOrNull()!!

        val body = restGetSafe("$REST_BASE/me/playlists?limit=$HOME_PLAYLIST_LIMIT", headers) ?: return emptyList()
        return runCatching { parseRestPlaylistsPage(json.parseToJsonElement(body).jsonObject) }.getOrDefault(emptyList())
    }

    private fun fetchLikedTracksSafe(headers: Map<String, String>, limit: Int): List<SpotifyTrack> {
        val gqlResult = runCatching {
            val variables = buildJsonObject {
                put("offset", 0)
                put("limit", limit)
            }
            parseLikedPage(gql("fetchLibraryTracks", LIKED, variables, headers)).first
        }
        if (gqlResult.isSuccess) return gqlResult.getOrThrow()
        if (gqlResult.exceptionOrNull() is SpotifyAuthException) throw gqlResult.exceptionOrNull()!!

        val body = restGetSafe("$REST_BASE/me/tracks?limit=$limit", headers) ?: return emptyList()
        return runCatching { parseRestSavedTracksPage(json.parseToJsonElement(body).jsonObject) }.getOrDefault(emptyList())
    }

    private fun fetchSavedAlbumsSafe(headers: Map<String, String>, limit: Int): List<SpotifyAlbum> {
        val gqlResult = runCatching {
            val variables = buildJsonObject {
                putJsonArray("filters") { add("Albums") }
                put("order", null as String?)
                put("textFilter", "")
                putJsonArray("features") {
                    add("LIKED_SONGS")
                    add("YOUR_EPISODES_V2")
                    add("PRERELEASES")
                    add("EVENTS")
                }
                put("limit", limit)
                put("offset", 0)
                put("flatten", true)
                putJsonArray("expandedFolders") {}
                put("folderUri", null as String?)
                put("includeFoldersWhenFlattening", false)
            }
            parseAlbumLibraryPage(gql("libraryV3", LIBRARY, variables, headers))
        }
        val gqlAlbums = gqlResult.getOrNull().orEmpty()
        if (gqlAlbums.isNotEmpty()) return gqlAlbums
        if (gqlResult.exceptionOrNull() is SpotifyAuthException) throw gqlResult.exceptionOrNull()!!

        val body = restGetSafe("$REST_BASE/me/albums?limit=$limit", headers) ?: return emptyList()
        return runCatching { parseRestAlbumsPage(json.parseToJsonElement(body).jsonObject) }.getOrDefault(emptyList())
    }

    private fun fetchMadeForYouSafe(headers: Map<String, String>): List<SpotifyPlaylist> {
        val body = restGetSafe("$REST_BASE/views/made-for-you?content_limit=20", headers) ?: return emptyList()
        return runCatching { parseMadeForYouView(json.parseToJsonElement(body).jsonObject) }.getOrDefault(emptyList())
    }

    private suspend fun fetchPersonalizedMixesViaSearch(headers: Map<String, String>): List<SpotifyPlaylist> = coroutineScope {
        val queries = listOf("Daily Mix", "Discover Weekly", "Release Radar", "On Repeat")
        queries.map { query ->
            async(Dispatchers.IO) {
                val url = "$REST_BASE/search".toHttpUrl().newBuilder()
                    .addQueryParameter("q", query)
                    .addQueryParameter("type", "playlist")
                    .addQueryParameter("limit", "8")
                    .build()
                    .toString()
                val body = restGetSafe(url, headers) ?: return@async emptyList()
                runCatching { parsePersonalizedSearchPlaylists(json.parseToJsonElement(body).jsonObject) }
                    .getOrDefault(emptyList())
            }
        }.flatMap { it.await() }
    }

    private fun fetchRecentlyPlayedSafe(headers: Map<String, String>): List<SpotifyTrack> {
        val body = restGetSafe("$REST_BASE/me/player/recently-played?limit=20", headers) ?: return emptyList()
        return runCatching { parseRestRecentlyPlayedPage(json.parseToJsonElement(body).jsonObject) }.getOrDefault(emptyList())
    }

    private fun fetchTopTracksSafe(headers: Map<String, String>): List<SpotifyTrack> {
        val body = restGetSafe("$REST_BASE/me/top/tracks?limit=15&time_range=short_term", headers) ?: return emptyList()
        return runCatching { parseRestTopTracksPage(json.parseToJsonElement(body).jsonObject) }.getOrDefault(emptyList())
    }

    private fun playlistPage(headers: Map<String, String>, offset: Int): Triple<List<SpotifyPlaylist>, Int, Int> {
        val variables = buildJsonObject {
            putJsonArray("filters") { add("Playlists") }
            put("order", null as String?)
            put("textFilter", "")
            putJsonArray("features") {
                add("LIKED_SONGS")
                add("YOUR_EPISODES_V2")
                add("PRERELEASES")
                add("EVENTS")
            }
            put("limit", 50)
            put("offset", offset)
            put("flatten", true)
            putJsonArray("expandedFolders") {}
            put("folderUri", null as String?)
            put("includeFoldersWhenFlattening", false)
        }
        return parsePlaylistPage(gql("libraryV3", LIBRARY, variables, headers))
    }

    private fun trackPage(
        headers: Map<String, String>,
        playlistId: String,
        offset: Int,
    ): Triple<List<SpotifyTrack>, Int, Int> {
        val variables = buildJsonObject {
            put("uri", "spotify:playlist:$playlistId")
            put("offset", offset)
            put("limit", 100)
            put("enableWatchFeedEntrypoint", false)
        }
        return parseTrackPage(gql("fetchPlaylist", PLAYLIST, variables, headers))
    }

    /**
     * The playlist's cover at its largest. The library list only carries a
     * thumbnail, which looks soft once it fills a playlist page's header, so
     * the page asks for the real image after it opens.
     */
    suspend fun cover(playlistId: String): String? = withContext(Dispatchers.IO) {
        if (playlistId == LIKED_ID) return@withContext LIKED_COVER
        val variables = buildJsonObject {
            put("uri", "spotify:playlist:$playlistId")
            put("offset", 0)
            put("limit", 1)
            put("enableWatchFeedEntrypoint", false)
        }
        val root = gql("fetchPlaylist", PLAYLIST, variables, authHeaders())
        largestSource(root.obj("data")?.obj("playlistV2")?.obj("images"))
    }

    private fun likedPage(headers: Map<String, String>, offset: Int): Triple<List<SpotifyTrack>, Int, Int> {
        val variables = buildJsonObject {
            put("offset", offset)
            put("limit", 100)
        }
        return parseLikedPage(gql("fetchLibraryTracks", LIKED, variables, headers))
    }

    private suspend fun authHeaders(): Map<String, String> {
        val tokens = auth?.tokens()
            ?: throw SpotifyAuthException("Spotify sign-in expired")
        return buildMap {
            put("Authorization", "Bearer ${tokens.bearer}")
            put("Accept", "application/json")
            put("app-platform", "WebPlayer")
            put("Origin", "https://open.spotify.com")
            put("Referer", "https://open.spotify.com/")
            put("User-Agent", USER_AGENT)
            tokens.clientToken?.let { put("Client-Token", it) }
        }
    }

    private fun restGet(url: String, headers: Map<String, String>): String? {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        return Http.client.newCall(request).execute().use { response ->
            if (response.code == 401) throw SpotifyAuthException("Spotify session expired (401)")
            if (!response.isSuccessful) {
                Log.d(TAG, "REST $url returned ${response.code}")
                return@use null
            }
            response.body.string()
        }
    }

    private fun restGetSafe(url: String, headers: Map<String, String>): String? = runCatching {
        restGet(url, headers)
    }.onFailure { error ->
        if (error is SpotifyAuthException) throw error
        Log.d(TAG, "Optional Spotify endpoint failed ($url): ${error.message}")
    }.getOrNull()

    private fun gql(operation: String, hash: String, variables: JsonObject, headers: Map<String, String>): JsonObject {
        val body = buildJsonObject {
            put("variables", variables)
            put("operationName", operation)
            putJsonObject("extensions") {
                putJsonObject("persistedQuery") {
                    put("version", 1)
                    put("sha256Hash", hash)
                }
            }
        }
        val request = Request.Builder()
            .url(GQL)
            .post(body.toString().toRequestBody(mediaType))
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
        val text = Http.client.newCall(request).execute().use { response ->
            val payload = response.body.string()
            if (response.code == 401) {
                throw SpotifyAuthException("Spotify session expired (401)")
            }
            if (!response.isSuccessful) {
                throw IllegalStateException("Spotify $operation failed (${response.code})")
            }
            payload
        }
        val root = json.parseToJsonElement(text).jsonObject
        val error = root.arr("errors")?.firstOrNull()?.jsonObject?.str("message")
        if (!error.isNullOrBlank()) throw IllegalStateException(error)
        return root
    }
}

private val PERSONALIZED_MIX_TITLES = listOf(
    "daily mix",
    "discover weekly",
    "release radar",
    "on repeat",
    "repeat rewind",
    "daylist",
    "time capsule",
)

internal fun isSpotifyPersonalizedPlaylist(playlist: SpotifyPlaylist): Boolean {
    val name = playlist.name.trim().lowercase()
    val owner = playlist.owner?.trim().orEmpty()
    val ownedBySpotify = owner.equals("Spotify", ignoreCase = true) ||
        owner.startsWith("Made for", ignoreCase = true)
    if (!ownedBySpotify && owner.isNotEmpty()) return false
    return PERSONALIZED_MIX_TITLES.any { title -> name.startsWith(title) || name == title }
}

private fun hasCorePersonalizedMixes(playlists: List<SpotifyPlaylist>): Boolean {
    val names = playlists.map { it.name.lowercase() }
    val hasDailyMix = names.any { it.startsWith("daily mix") }
    val hasDiscover = names.any { it.contains("discover weekly") }
    val hasRadar = names.any { it.contains("release radar") }
    return hasDailyMix && hasDiscover && hasRadar
}

internal fun orderPersonalizedMixes(playlists: List<SpotifyPlaylist>): List<SpotifyPlaylist> {
    fun priority(name: String): Int {
        val lower = name.trim().lowercase()
        return when {
            lower == "discover weekly" -> 0
            lower == "release radar" -> 1
            lower.startsWith("daily mix") -> 2
            lower == "daylist" -> 3
            lower == "on repeat" -> 4
            lower == "repeat rewind" -> 5
            else -> 6
        }
    }
    return playlists.sortedWith(compareBy({ priority(it.name) }, { it.name }))
}

internal fun deduplicateSpotifyTracks(
    tracks: List<SpotifyTrack>,
    seenKeys: MutableSet<String> = HashSet(),
): List<SpotifyTrack> = tracks.filter { track ->
    val idOk = track.id.isBlank() || seenKeys.add("id:${track.id}")
    val metaKey = "meta:${track.title.trim().lowercase()}|${track.artist.trim().lowercase()}"
    val metaOk = seenKeys.add(metaKey)
    idOk && metaOk
}

/** The playlists kept, the library's total, and how many entries the page held before filtering. */
internal fun parsePlaylistPage(root: JsonObject): Triple<List<SpotifyPlaylist>, Int, Int> {
    val library = root.obj("data")?.obj("me")?.obj("libraryV3")
        ?: throw IllegalStateException("Spotify library response was empty")
    val rawItems = library.arr("items").orEmpty()
    val items = rawItems.mapNotNull { element ->
        val wrapper = element.jsonObject.obj("item") ?: return@mapNotNull null
        val typeName = wrapper.str("__typename").orEmpty()
        if (!typeName.contains("Playlist", ignoreCase = true)) return@mapNotNull null
        val data = wrapper.obj("data") ?: return@mapNotNull null
        if (data.str("__typename") != "Playlist") return@mapNotNull null
        val uri = wrapper.str("_uri") ?: return@mapNotNull null
        val name = data.str("name").orEmpty()
        if (name.isBlank()) return@mapNotNull null
        SpotifyPlaylist(
            id = uri.substringAfterLast(":"),
            name = name,
            owner = data.obj("ownerV2")?.obj("data")?.str("name"),
            imageUrl = coverUrl(data.obj("images")),
        )
    }
    return Triple(items, library.int("totalCount") ?: rawItems.size, rawItems.size)
}

/** Saved albums from Pathfinder `libraryV3` (`filters: ["Albums"]`). */
internal fun parseAlbumLibraryPage(root: JsonObject): List<SpotifyAlbum> {
    val library = root.obj("data")?.obj("me")?.obj("libraryV3") ?: return emptyList()
    return library.arr("items").orEmpty().mapNotNull { element ->
        val wrapper = element.jsonObject.obj("item") ?: return@mapNotNull null
        val typeName = wrapper.str("__typename").orEmpty()
        if (!typeName.contains("Album", ignoreCase = true)) return@mapNotNull null
        val data = wrapper.obj("data") ?: return@mapNotNull null
        val uri = wrapper.str("_uri") ?: data.str("uri") ?: return@mapNotNull null
        val name = data.str("name").orEmpty()
        if (name.isBlank()) return@mapNotNull null
        val artist = data.obj("artists")?.arr("items").orEmpty().mapNotNull { artist ->
            artist.jsonObject.obj("profile")?.str("name")?.takeIf { it.isNotBlank() }
        }.joinToString(", ")
        val year = data.obj("date")?.int("year")?.toString()
        SpotifyAlbum(
            id = uri.substringAfterLast(":"),
            name = name,
            artist = artist,
            imageUrl = data.obj("coverArt")?.arr("sources")?.let(::largestImageUrl)
                ?: coverUrl(data.obj("images")),
            releaseYear = year,
        )
    }
}

/** The tracks kept, the playlist's total, and how many entries the page held before filtering. */
internal fun parseTrackPage(root: JsonObject): Triple<List<SpotifyTrack>, Int, Int> {
    val content = root.obj("data")?.obj("playlistV2")?.obj("content")
        ?: throw IllegalStateException("Spotify playlist response was empty")
    val rawItems = content.arr("items").orEmpty()
    val items = rawItems.mapNotNull { element ->
        val data = element.jsonObject.obj("itemV2")?.obj("data") ?: return@mapNotNull null
        val title = data.str("name").orEmpty()
        if (title.isBlank()) return@mapNotNull null
        val uri = data.str("uri") ?: data.str("_uri") ?: return@mapNotNull null
        val artist = data.obj("artists")?.arr("items").orEmpty().mapNotNull { artist ->
            artist.jsonObject.obj("profile")?.str("name")?.takeIf { it.isNotBlank() }
        }.joinToString(", ")
        val album = data.obj("albumOfTrack")
        SpotifyTrack(
            id = uri.substringAfterLast(":"),
            title = title,
            artist = artist,
            album = album?.str("name"),
            durationMs = data.obj("duration")?.int("totalMilliseconds") ?: 0,
            imageUrl = album?.obj("coverArt")?.arr("sources")?.lastUrl(),
        )
    }
    return Triple(items, content.int("totalCount") ?: rawItems.size, rawItems.size)
}

/** The Liked Songs page: `me.library.tracks`, each entry wrapping the track under `track`. */
internal fun parseLikedPage(root: JsonObject): Triple<List<SpotifyTrack>, Int, Int> {
    val tracks = root.obj("data")?.obj("me")?.obj("library")?.obj("tracks")
        ?: throw IllegalStateException("Spotify liked songs response was empty")
    val rawItems = tracks.arr("items").orEmpty()
    val items = rawItems.mapNotNull { element ->
        val wrapper = element.jsonObject.obj("track") ?: return@mapNotNull null
        val data = wrapper.obj("data") ?: return@mapNotNull null
        val title = data.str("name").orEmpty()
        if (title.isBlank()) return@mapNotNull null
        val uri = wrapper.str("_uri") ?: data.str("uri") ?: return@mapNotNull null
        val album = data.obj("albumOfTrack")
        SpotifyTrack(
            id = uri.substringAfterLast(":"),
            title = title,
            artist = data.obj("artists")?.arr("items").orEmpty().mapNotNull { artist ->
                artist.jsonObject.obj("profile")?.str("name")?.takeIf { it.isNotBlank() }
            }.joinToString(", "),
            album = album?.str("name"),
            durationMs = (data.obj("trackDuration") ?: data.obj("duration"))?.int("totalMilliseconds") ?: 0,
            imageUrl = album?.obj("coverArt")?.arr("sources")?.lastUrl(),
        )
    }
    return Triple(items, tracks.int("totalCount") ?: rawItems.size, rawItems.size)
}

internal fun parseRestPlaylistsPage(root: JsonObject): List<SpotifyPlaylist> =
    root.arr("items").orEmpty().mapNotNull { element ->
        parseRestPlaylistObject(element as? JsonObject ?: return@mapNotNull null)
    }

internal fun parseMadeForYouView(root: JsonObject): List<SpotifyPlaylist> =
    root.obj("content")?.arr("items").orEmpty().mapNotNull { element ->
        val playlist = parseRestPlaylistObject(element as? JsonObject ?: return@mapNotNull null)
            ?: return@mapNotNull null
        playlist.takeIf { isSpotifyPersonalizedPlaylist(it) || it.owner.equals("Spotify", ignoreCase = true) }
    }

internal fun parsePersonalizedSearchPlaylists(root: JsonObject): List<SpotifyPlaylist> =
    root.obj("playlists")?.arr("items").orEmpty().mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val ownerObj = obj.obj("owner")
        val ownerId = ownerObj?.str("id").orEmpty()
        val ownerName = ownerObj?.str("display_name")
        val isSpotifyOwned = ownerId == "spotify" || ownerName.equals("Spotify", ignoreCase = true)
        if (!isSpotifyOwned) return@mapNotNull null
        val playlist = parseRestPlaylistObject(obj) ?: return@mapNotNull null
        playlist.takeIf { isSpotifyPersonalizedPlaylist(it) }
    }

internal fun parseRestAlbumsPage(root: JsonObject): List<SpotifyAlbum> =
    root.arr("items").orEmpty().mapNotNull { element ->
        val album = (element as? JsonObject)?.obj("album") ?: return@mapNotNull null
        val id = album.str("id") ?: return@mapNotNull null
        val name = album.str("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val artist = album.arr("artists").orEmpty().mapNotNull { artistEl ->
            (artistEl as? JsonObject)?.str("name")?.takeIf { it.isNotBlank() }
        }.joinToString(", ")
        val year = album.str("release_date")?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) }
        SpotifyAlbum(
            id = id,
            name = name,
            artist = artist,
            imageUrl = album.arr("images")?.let(::largestImageUrl),
            releaseYear = year,
        )
    }

internal fun parseRestSavedTracksPage(root: JsonObject): List<SpotifyTrack> =
    root.arr("items").orEmpty().mapNotNull { element ->
        val track = (element as? JsonObject)?.obj("track") ?: return@mapNotNull null
        parseRestTrackObject(track)
    }

internal fun parseRestRecentlyPlayedPage(root: JsonObject): List<SpotifyTrack> =
    root.arr("items").orEmpty().mapNotNull { element ->
        val track = (element as? JsonObject)?.obj("track") ?: return@mapNotNull null
        parseRestTrackObject(track)
    }

internal fun parseRestTopTracksPage(root: JsonObject): List<SpotifyTrack> =
    root.arr("items").orEmpty().mapNotNull { element ->
        parseRestTrackObject(element as? JsonObject ?: return@mapNotNull null)
    }

internal fun parseRestAlbumDetail(root: JsonObject): Pair<String?, List<SpotifyTrack>> {
    val albumName = root.str("name")
    val cover = root.arr("images")?.let(::largestImageUrl)
    val trackItems = root.obj("tracks")?.arr("items").orEmpty()
    val tracks = trackItems.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val id = obj.str("id") ?: obj.str("uri")?.substringAfterLast(":") ?: return@mapNotNull null
        val title = obj.str("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val artist = obj.arr("artists").orEmpty().mapNotNull { artistEl ->
            (artistEl as? JsonObject)?.str("name")?.takeIf { it.isNotBlank() }
        }.joinToString(", ")
        SpotifyTrack(
            id = id,
            title = title,
            artist = artist,
            album = albumName,
            durationMs = obj.int("duration_ms") ?: 0,
            imageUrl = cover,
        )
    }
    return cover to tracks
}

private fun parseRestPlaylistObject(obj: JsonObject): SpotifyPlaylist? {
    val id = obj.str("id") ?: obj.str("uri")?.substringAfterLast(":") ?: return null
    val name = obj.str("name")?.takeIf { it.isNotBlank() } ?: return null
    val owner = obj.obj("owner")?.str("display_name") ?: obj.obj("owner")?.str("id")
    val imageUrl = obj.arr("images")?.let(::largestImageUrl)
    return SpotifyPlaylist(id = id, name = name, owner = owner, imageUrl = imageUrl)
}

private fun parseRestTrackObject(track: JsonObject): SpotifyTrack? {
    val id = track.str("id") ?: track.str("uri")?.substringAfterLast(":") ?: return null
    val title = track.str("name")?.takeIf { it.isNotBlank() } ?: return null
    val artist = track.arr("artists").orEmpty().mapNotNull { artistEl ->
        (artistEl as? JsonObject)?.str("name")?.takeIf { it.isNotBlank() }
    }.joinToString(", ")
    val albumObj = track.obj("album")
    val imageUrl = albumObj?.arr("images")?.let(::largestImageUrl)
    return SpotifyTrack(
        id = id,
        title = title,
        artist = artist,
        album = albumObj?.str("name"),
        durationMs = track.int("duration_ms") ?: 0,
        imageUrl = imageUrl,
    )
}

/**
 * The widest source of an image's first entry. Spotify lists a playlist's
 * sizes in no fixed order — a 60px thumbnail can come last — so the list is
 * read by width rather than by position.
 */
private fun largestSource(images: JsonObject?): String? =
    images?.arr("items")?.firstOrNull()?.jsonObject?.arr("sources")?.let(::largestImageUrl)

private fun largestImageUrl(sources: JsonArray): String? =
    sources.mapNotNull { source ->
        val obj = source as? JsonObject ?: return@mapNotNull null
        val url = obj.str("url") ?: return@mapNotNull null
        url to (obj.int("width") ?: 0)
    }.maxByOrNull { it.second }?.first

private fun coverUrl(images: JsonObject?): String? = largestSource(images)

private fun JsonArray.lastUrl(): String? =
    mapNotNull { it.jsonObject.str("url") }.lastOrNull()

private fun JsonObject.obj(key: String): JsonObject? =
    this[key]?.takeIf { it !is JsonNull }?.let { runCatching { it.jsonObject }.getOrNull() }

private fun JsonObject.arr(key: String): JsonArray? =
    this[key]?.takeIf { it !is JsonNull }?.let { runCatching { it.jsonArray }.getOrNull() }

private fun JsonObject.str(key: String): String? =
    this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull

private fun JsonObject.int(key: String): Int? =
    this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.intOrNull
