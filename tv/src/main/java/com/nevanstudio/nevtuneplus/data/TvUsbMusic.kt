// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.nevanstudio.nevtuneplus.data.DebugLog as Log
import com.nevanstudio.nevtuneplus.data.model.Song
import java.io.File
import java.util.Locale

/**
 * Music on USB drives and other mounted storage, which MediaStore on a TV box
 * often does not index. Plugged into [LocalMediaRepository.extraLocalMusic], so
 * the phone's MediaStore scan stays the single source for everything else.
 */
object TvUsbMusic {
    private const val TAG = "NevTunePlus"
    private const val MAX_DEPTH = 3
    private const val MAX_USB_SONGS = 500

    private val audioExtensions = setOf(
        "mp3", "m4a", "flac", "wav", "ogg", "opus", "aac", "webm", "3gp",
    )

    fun install() {
        LocalMediaRepository.extraLocalMusic = ::withUsbDrives
    }

    /** [found] (what MediaStore knows) plus any audio files on mounted storage it missed. */
    private fun withUsbDrives(context: Context, found: List<Song>): List<Song> {
        val songs = found.toMutableList()
        val knownPaths = found.mapNotNullTo(mutableSetOf()) { it.localPath }

        runCatching {
            File("/storage").takeIf { it.isDirectory }?.listFiles()?.forEach { volume ->
                if (volume.isDirectory && volume.canRead()) {
                    scan(context, volume, songs, knownPaths, MAX_DEPTH)
                }
            }
        }.onFailure { Log.w(TAG, "Failed scanning /storage/ for USB drives: ${it.message}") }

        // Drives the app can reach through its own per-volume directories.
        runCatching {
            context.getExternalFilesDirs(null)?.forEach { externalDir ->
                var root: File = externalDir ?: return@forEach
                while (root.parentFile != null &&
                    root.parentFile?.name != "storage" &&
                    root.parentFile?.name != "Android"
                ) {
                    root = root.parentFile ?: break
                }
                if (root.canRead()) scan(context, root, songs, knownPaths, MAX_DEPTH)
            }
        }

        return songs.distinctBy { it.localPath ?: it.localUri ?: it.videoId }
    }

    private fun scan(
        context: Context,
        dir: File,
        results: MutableList<Song>,
        knownPaths: MutableSet<String>,
        maxDepth: Int,
    ) {
        if (results.size >= MAX_USB_SONGS || maxDepth <= 0 || !dir.canRead() || dir.name.startsWith(".")) return

        try {
            val children = dir.listFiles() ?: return
            for (file in children) {
                if (results.size >= MAX_USB_SONGS) break
                if (file.isDirectory) {
                    scan(context, file, results, knownPaths, maxDepth - 1)
                } else if (file.isFile && file.extension.lowercase(Locale.ROOT) in audioExtensions) {
                    if (knownPaths.add(file.absolutePath)) {
                        results.add(songFromFile(context, file))
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed reading USB directory ${dir.absolutePath}: ${e.message}")
        }
    }

    private fun songFromFile(context: Context, file: File): Song {
        val uri = Uri.fromFile(file).toString()
        var title = file.nameWithoutExtension
        var artist = "Unknown Artist"
        var albumName: String? = null
        var durationText: String? = null

        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri.toUri())
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { it.isNotBlank() }?.let { title = it }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf { it.isNotBlank() }?.let { artist = it }
                albumName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.takeUnless { it.isBlank() || it == "<unknown>" }
                durationText = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.takeIf { it > 0 }?.let(::formatDuration)
            } finally {
                retriever.release()
            }
        }

        return Song(
            videoId = uri,
            title = title,
            thumbnailUrl = null,
            artist = artist,
            durationText = durationText,
            albumName = albumName,
            localUri = uri,
            localPath = file.absolutePath,
            fromAutoplay = false,
        )
    }

    private fun formatDuration(ms: Long): String {
        val totalSecs = ms / 1000
        return String.format(Locale.ROOT, "%d:%02d", totalSecs / 60, totalSecs % 60)
    }
}
