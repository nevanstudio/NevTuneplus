// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus

import com.nevanstudio.nevtuneplus.ui.motion.MotionTokens

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import com.nevanstudio.nevtuneplus.ui.performance.AdaptivePerformanceConfig
import com.nevanstudio.nevtuneplus.ui.performance.AdaptivePerformanceEngine
import com.nevanstudio.nevtuneplus.ui.player.clearArtworkBackdropCaches
import com.nevanstudio.nevtuneplus.ui.theme.clearArtworkPaletteCaches
import com.nevanstudio.nevtuneplus.auth.AuthStore
import com.nevanstudio.nevtuneplus.data.DebugLog
import com.nevanstudio.nevtuneplus.data.Http
import com.nevanstudio.nevtuneplus.data.lyrics.LyricsTranslation
import androidx.appcompat.app.AppCompatDelegate
import com.nevanstudio.nevtuneplus.ui.player.AndroidPlayerHost
import com.nevanstudio.nevtuneplus.ui.player.PlayerPlatform
import com.nevanstudio.nevtuneplus.data.canvas.CanvasCache
import com.nevanstudio.nevtuneplus.data.smb.SmbCoverFetcher
import com.nevanstudio.nevtuneplus.data.webdav.WebDavCoilAuth
import com.nevanstudio.nevtuneplus.data.canvas.SpotifyToken
import com.nevanstudio.nevtuneplus.data.spotify.SpotifyLibrary
import com.nevanstudio.nevtuneplus.playback.AudioCache
import com.nevanstudio.nevtuneplus.playback.LastPlayed
import com.nevanstudio.nevtuneplus.playback.BoundedContextPersistence
import com.nevanstudio.nevtuneplus.playback.OriginalVersion
import com.nevanstudio.nevtuneplus.data.innertube.Innertube
import com.nevanstudio.nevtuneplus.data.innertube.AndroidStreamHooks
import com.nevanstudio.nevtuneplus.data.listentogether.ListenTogether
import com.nevanstudio.nevtuneplus.data.scrobbling.LastFM
import com.nevanstudio.nevtuneplus.data.settings.AppSettings
import com.nevanstudio.nevtuneplus.data.settings.SearchHistory
import com.nevanstudio.nevtuneplus.data.sources.SourceRegistry
import com.nevanstudio.nevtuneplus.data.stats.ArtistFacts
import com.nevanstudio.nevtuneplus.data.stats.ListeningStats
import com.nevanstudio.nevtuneplus.download.Downloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

internal const val IMAGE_MEMORY_CACHE_HEAP_FRACTION = 0.25
internal const val LOW_RAM_IMAGE_MEMORY_CACHE_HEAP_FRACTION = 0.15

internal fun imageMemoryCacheHeapFraction(isLowRamDevice: Boolean): Double =
    if (isLowRamDevice) LOW_RAM_IMAGE_MEMORY_CACHE_HEAP_FRACTION else IMAGE_MEMORY_CACHE_HEAP_FRACTION

internal fun imageMemoryCacheHeapFraction(config: AdaptivePerformanceConfig): Double =
    config.imageMemoryCacheFraction

internal fun imageDiskCacheMaxBytes(config: AdaptivePerformanceConfig): Long =
    config.imageDiskCacheMb * 1024L * 1024L

open class NevTunePlusApplication : Application(), SingletonImageLoader.Factory {

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        com.nevanstudio.nevtuneplus.data.CrashReporter.install(this)
        // The player is drawn by the shared UI module; this is what it reads
        // underneath — settings, the Canvas decoder, outputs, the party.
        AndroidStreamHooks.installEarly()
        PlayerPlatform.install(AndroidPlayerHost(this))
        com.nevanstudio.nevtuneplus.ui.AppUi.install(com.nevanstudio.nevtuneplus.ui.AndroidAppUiHost)
        // The shared data layer (lyrics, the YouTube Music client) logs to
        // logcat on debug builds only, as the app's own DebugLog always has.
        if (BuildConfig.DEBUG) {
            DebugLog.sink = DebugLog.Sink { level, tag, message, error ->
                when (level) {
                    'D' -> android.util.Log.d(tag, message, error)
                    'I' -> android.util.Log.i(tag, message, error)
                    'W' -> android.util.Log.w(tag, message, error)
                    else -> android.util.Log.e(tag, message, error)
                }
            }
        }
        // The Spotify library and import live in the shared module; the tokens are the WebView's.
        SpotifyLibrary.auth = SpotifyLibrary.Auth {
            SpotifyToken.accessToken()?.let { SpotifyLibrary.Tokens(it, SpotifyToken.clientToken()) }
        }
        SpotifyLibrary.onAuthExpired = { SpotifyToken.invalidate() }
        // The per-app language picker, which YouTube Music's `hl` follows.
        // Full tag (zh-Hant / zh-Hans preserved); Innertube maps to hl/gl.
        Innertube.appLanguage = { AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() }
        migrateScriptOnlyAppLocale()
        LyricsTranslation.cacheDir = cacheDir
        // PlaybackService shares this process, so seeding the cookie here means
        // stream resolution is authenticated from the first play onwards.
        authStore = AuthStore(this)
        // Opened off the main thread, alongside everything below: none of these
        // reads a setting or the session, and between them they are the slowest
        // opens at startup — SourceRegistry's encrypted store most of all.
        // Started only once [AuthStore] exists, because both encrypted stores
        // share one keystore master key and a first launch must not have two
        // threads racing to create it. Joined before onCreate returns, so
        // nothing that runs after startup can see any of them half open.
        val backgroundInit = thread(name = "startup-init") {
            SourceRegistry.init(this)
            AndroidStreamHooks.initInnerTubeX(this)
            // Its own directory: canvas clips are looping video, not audio, and
            // belong in a cache AudioCache's own limit and eviction policy were
            // never sized for. See CanvasCache's doc for why this one exists at
            // all — it is the fix for canvas clips re-fetching the same few
            // seconds of video from the network on every loop.
            CanvasCache.init(this)
        }
        // Migration-safe: an old single cookie becomes the first encrypted
        // session, while newer installs restore the profile the listener chose.
        val restoredSession = authStore.activeSession
        if (restoredSession != null && authStore.activeAccountId == null) {
            authStore.select(restoredSession.accountId, restoredSession.activeProfileId)
        }
        authStore.cookie = restoredSession?.cookie
        Innertube.cookie = restoredSession?.cookie
        // Which account that cookie actually acts as. Read here rather than on
        // demand so the answer is usually in hand before the first request needs
        // it: a play registered under the wrong account is indistinguishable, to
        // the listener, from one that was never registered at all. Fire and
        // forget — every caller works without it, just less precisely.
        if (restoredSession != null) {
            // After the cookie, never before: setting the cookie clears any
            // channel the last session was acting as, so restoring the choice
            // first would restore it into the value about to be wiped.
            restoredSession.profiles.firstOrNull { it.profileId == restoredSession.activeProfileId }
                ?.let { Innertube.selectChannel(it.pageId, it.dataSyncId, it.authUser) }
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { Innertube.ensureSessionScope() }
                    .onFailure { android.util.Log.w("NevTunePlus", "Failed to ensure session scope: ${it.message}") }
            }
        }
        AppSettings.init(this, authStore)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { com.nevanstudio.nevtuneplus.data.local.LocalFirstRepository.initialize(this@NevTunePlusApplication) }
                .onFailure { android.util.Log.e("NevTunePlus", "Local Room database initialization failed", it) }
        }
        // Before anything resolves a track: an addon with `checkValidLossless`
        // is gated on this, and the gate reads "no" until it has looked.
        com.nevanstudio.nevtuneplus.playback.audio.LosslessOutput.init(this)
        // Restores a party this device is still a member of, so a process death
        // mid-session is something the rest of the party never sees. The socket
        // and the clock offset are not restored — both are re-established on
        // the next connect, which is the only way to be sure they are current.
        ListenTogether.init(this)
        SearchHistory.init(this)
        LastPlayed.init(this)
        BoundedContextPersistence.init(this)
        com.nevanstudio.nevtuneplus.playback.PartyPersonalQueueStash.init(this)
        // Which tracks the listener has reverted to YouTube's own upload. Read
        // by [Song.toMediaItem], so it has to be open before the restart
        // snapshot below is turned back into queue items.
        OriginalVersion.init(this)
        // What's already saved to Downloads, so the song menu can say so
        // without a media-store query per row.
        Downloads.init(this)
        // The device's own listening record. Opened here rather than in
        // PlaybackService because the Replay page reads it from the UI side and
        // both live in this process — one owner, one directory.
        ListeningStats.init(this)
        // After AppSettings, whose switch decides whether half of it runs.
        ArtistFacts.init(this)
        // One cache directory can only be opened once per process, and
        // PlaybackService shares this one — so it's opened here, not there.
        AudioCache.init(this)
        // The offscreen WebView that mints a Spotify access token from the
        // listener's own session cookie needs a Context, and nothing in the
        // suspend call chain that reaches it (a track's canvas lookup) has
        // one to hand — see SpotifyToken's doc for why.
        SpotifyToken.init(this)
        // A sideloaded update is just a new APK over the old one, so app data —
        // including whatever the old build left in the image cache — survives
        // it untouched. Wipe that on the first launch of a higher versionCode so
        // a format change between builds can't serve stale bytes.
        //
        // The song cache is deliberately *not* wiped here any more. It is the
        // Cached songs folder and what plays offline, and clearing it on every
        // update is what made the whole folder disappear overnight whenever a
        // new build was installed. A build that really does change its keys or
        // its bytes says so through AudioCache's own cache format instead.
        if (AppSettings.consumeVersionUpdate(BuildConfig.VERSION_CODE)) {
            SingletonImageLoader.get(this).let { loader ->
                loader.memoryCache?.clear()
                loader.diskCache?.clear()
            }
        }
        // Initialize LastFM with saved settings if available
        initLastfm()
        // The public "apps open right now" count; see Presence.
        com.nevanstudio.nevtuneplus.data.presence.AndroidPresence.install(this)

        // Do not block the UI thread during cold startup; allow background initialization
        // to complete concurrently so MainActivity and TvActivity draw their initial frames immediately.
        // SourceRegistry and CanvasCache are thread-safe and await initialization on demand.
    }

    /**
     * Coil-specific request client derived from the shared playback transport.
     * It keeps Coil's identifying User-Agent while reusing Http's dispatcher
     * and connection pool rather than opening another pool for artwork.
     *
     * Wikimedia (which serves signature art) answers generic clients with
     * HTTP 403 under its User-Agent policy, so every image request carries an
     * identifying agent. Anything else falls through to Coil's own fetchers.
     */
    protected val coilHttpClient: OkHttpClient by lazy {
        Http.client.newBuilder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header(
                            "User-Agent",
                            "NevTune+/${BuildConfig.VERSION_NAME} (Android; +https://github.com/nevanstudio/NevTuneplus)",
                        )
                        .build(),
                )
            }
            .build()
    }

    /**
     * Artwork loading, which was previously left entirely on Coil's defaults.
     *
     * The defaults aren't unreasonable, but the disk cache is sized at 2% of
     * free space — which on a full phone is the 10MB floor, a few screens of
     * covers, and covers are exactly the thing worth still having tomorrow.
     * Naming a directory alongside it keeps that cache somewhere identifiable
     * rather than in the process's temp dir.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val perfConfig = AdaptivePerformanceEngine.config.value
        val builder = ImageLoader.Builder(context)
            // Covers on the WebDAV server need the credential or every one
            // of them 401s — which reads as "this track has no artwork".
            // Coil's own transport never sees Http.client's interceptor, so
            // the header is attached per request instead. See WebDavCoilAuth.
            .components {
                add(WebDavCoilAuth())
                // Covers filed on the SMB share; anything else falls
                // through to Coil's own fetchers. See SmbCoverFetcher.
                add(SmbCoverFetcher.Factory())
                // Artist signatures are usually SVG, which Coil can't
                // decode without this. See ArtistSignatureRepository.
                add(SvgDecoder.Factory())
                // Wikimedia 403s Coil's default client; see [coilHttpClient].
                add(OkHttpNetworkFetcherFactory(callFactory = { coilHttpClient }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(
                        context,
                        imageMemoryCacheHeapFraction(perfConfig),
                    )
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(imageDiskCacheMaxBytes(perfConfig))
                    .build()
            }
            .allowRgb565(perfConfig.useRgb565Artwork)
        return if (perfConfig.imageCrossfadeMs > 0) {
            builder.crossfade(perfConfig.imageCrossfadeMs).build()
        } else {
            builder.crossfade(false).build()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        com.nevanstudio.nevtuneplus.data.CrashReporter.recordLowMemory(level)
        AdaptivePerformanceEngine.onTrimMemory(level)
        @Suppress("DEPRECATION")
        if (level >= TRIM_MEMORY_RUNNING_LOW || level >= TRIM_MEMORY_BACKGROUND) {
            clearArtworkBackdropCaches()
            clearArtworkPaletteCaches()
            runCatching {
                val cache = SingletonImageLoader.get(this).memoryCache ?: return@runCatching
                @Suppress("DEPRECATION")
                if (level >= TRIM_MEMORY_RUNNING_CRITICAL || level >= TRIM_MEMORY_COMPLETE) {
                    cache.clear()
                } else {
                    cache.trimToSize(cache.size / 2)
                }
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        com.nevanstudio.nevtuneplus.data.CrashReporter.recordLowMemory(999)
        @Suppress("DEPRECATION")
        AdaptivePerformanceEngine.onTrimMemory(TRIM_MEMORY_COMPLETE)
        clearArtworkBackdropCaches()
        clearArtworkPaletteCaches()
        runCatching {
            SingletonImageLoader.get(this).memoryCache?.clear()
        }
    }

    private fun initLastfm() {
        val sessionKey = AppSettings.lastfmSessionKey.value
        if (sessionKey.isBlank()) return
        val endpoint = AppSettings.lastfmEndpoint.value.ifBlank { LastFM.DEFAULT_API_ENDPOINT }
        val apiKey = AppSettings.lastfmApiKey.value.trim()
        val secret = AppSettings.lastfmSecret.value.trim()
        if (apiKey.isBlank() || secret.isBlank()) return
        LastFM.configure(
            endpoint = endpoint,
            apiKey = apiKey,
            secret = secret,
            sessionKey = sessionKey,
        )
    }

    companion object {
        lateinit var authStore: AuthStore
            private set

        /**
         * One-time move from region-less script tags to regional ones.
         *
         * Early builds of the Simplified/Traditional picker stored `zh-Hant` /
         * `zh-Hans`. Android Views resolve those (script qualifiers), but the
         * shared Compose resources only match language + region — so Settings
         * read Traditional while the player read Simplified. Regional tags
         * match on both systems. Bare `zh` is left alone: it historically
         * meant Simplified.
         */
        private fun migrateScriptOnlyAppLocale() {
            val current = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
            val replacement = when (current) {
                "zh-Hant" -> "zh-TW"
                "zh-Hans" -> "zh-CN"
                else -> return
            }
            AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(replacement),
            )
        }
    }
}

class AudifyApplication : NevTunePlusApplication()
