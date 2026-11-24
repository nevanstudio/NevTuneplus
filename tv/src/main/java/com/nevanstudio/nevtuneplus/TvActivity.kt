// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch
import com.nevanstudio.nevtuneplus.data.listentogether.JamInviteLink
import com.nevanstudio.nevtuneplus.playback.MusicLink
import com.nevanstudio.nevtuneplus.playback.PlayerDeepLink
import com.nevanstudio.nevtuneplus.playback.rememberMediaController
import com.nevanstudio.nevtuneplus.playback.rememberPlayerState
import com.nevanstudio.nevtuneplus.ui.MainViewModel
import com.nevanstudio.nevtuneplus.ui.theme.NevTunePlusTheme
import com.nevanstudio.nevtuneplus.ui.tv.TvApp
import com.nevanstudio.nevtuneplus.ui.tv.display.TvRefreshRateController
import com.nevanstudio.nevtuneplus.ui.tv.theme.NevTunePlusTvTheme

/**
 * Dedicated TV Activity for Android TV, Google TV, and Fire TV devices.
 * Launched via `android.intent.category.LEANBACK_LAUNCHER`.
 */
@UnstableApi
class TvActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Attach TV Refresh Rate and Display Controller safely
        runCatching { TvRefreshRateController.attach(this) }

        // Observe audio session ID to attach Spatial Audio Virtualizer when enabled
        lifecycleScope.launch {
            com.nevanstudio.nevtuneplus.data.settings.AppSettings.audioSessionId.collect { sessionId ->
                if (sessionId > 0 && com.nevanstudio.nevtuneplus.data.settings.TvSettings.spatialAudioEnabled.value) {
                    com.nevanstudio.nevtuneplus.ui.tv.audio.TvSpatialAudioEngine.attach(sessionId)
                }
            }
        }

        // Make window full edge-to-edge for 1080p/4K television screens
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        setContent {
            NevTunePlusTvTheme {
                // The phone's Material theme too, so the components shared with
                // it (lyrics, mood sleeves) are set in SF Pro on its type scale.
                NevTunePlusTheme(darkTheme = true) {
                    val mediaController = rememberMediaController()
                    val playerState = rememberPlayerState(mediaController)

                    TvApp(
                        viewModel = viewModel,
                        mediaController = mediaController,
                        playerState = playerState,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLinks(intent)
    }

    private fun consumeLinks(intent: Intent) {
        PlayerDeepLink.consume(intent)
        JamInviteLink.consume(intent)
        MusicLink.consume(intent)
    }

    override fun onDestroy() {
        // Closing the app (back out of Home, or the task being swiped away)
        // pauses the nevanstudio rather than leaving it playing behind the launcher.
        if (isFinishing) {
            runCatching {
                val future = androidx.media3.session.MediaController.Builder(
                    applicationContext,
                    androidx.media3.session.SessionToken(
                        applicationContext,
                        android.content.ComponentName(
                            applicationContext,
                            com.nevanstudio.nevtuneplus.playback.PlaybackService::class.java,
                        ),
                    ),
                ).buildAsync()
                future.addListener({
                    runCatching { future.get().run { pause(); release() } }
                }, androidx.core.content.ContextCompat.getMainExecutor(applicationContext))
            }
        }
        super.onDestroy()
        runCatching { TvRefreshRateController.detach() }
        runCatching { com.nevanstudio.nevtuneplus.ui.tv.audio.TvSpatialAudioEngine.release() }
    }
}
