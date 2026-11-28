// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.data.spotify

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

internal const val SPOTIFY_HOME_CACHE_TTL_MS = 30L * 60L * 1000L

@Serializable
internal data class CachedSpotifyHome(
    val tokenHash: String,
    val fetchedAtMs: Long,
    val feed: SpotifyHomeFeed,
)

internal object SpotifyHomeCache {
    private const val FILE_NAME = "spotify_home_cache.json"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val lock = Any()

    fun isFresh(fetchedAtMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean =
        fetchedAtMs > 0L && (nowMs - fetchedAtMs) in 0L until SPOTIFY_HOME_CACHE_TTL_MS

    suspend fun read(context: Context, spdcToken: String): CachedSpotifyHome? = withContext(Dispatchers.IO) {
        if (spdcToken.isBlank()) return@withContext null
        val expectedHash = sha256(spdcToken)
        synchronized(lock) {
            val file = File(context.cacheDir, FILE_NAME)
            if (!file.isFile) return@withContext null
            runCatching {
                val cached = json.decodeFromString<CachedSpotifyHome>(file.readText())
                cached.takeIf { it.tokenHash == expectedHash && !it.feed.isEmpty }
            }.getOrNull()
        }
    }

    suspend fun write(
        context: Context,
        spdcToken: String,
        feed: SpotifyHomeFeed,
        fetchedAtMs: Long = System.currentTimeMillis(),
    ) = withContext(Dispatchers.IO) {
        if (spdcToken.isBlank()) return@withContext
        val entry = CachedSpotifyHome(
            tokenHash = sha256(spdcToken),
            fetchedAtMs = fetchedAtMs,
            feed = feed,
        )
        synchronized(lock) {
            runCatching {
                val file = File(context.cacheDir, FILE_NAME)
                file.writeText(json.encodeToString(entry))
            }
        }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            runCatching {
                File(context.cacheDir, FILE_NAME).delete()
            }
        }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
