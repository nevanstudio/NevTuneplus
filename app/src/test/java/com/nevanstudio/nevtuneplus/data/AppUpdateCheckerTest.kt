// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class AppUpdateCheckerTest {

    @Test
    fun prefersTheMatchingAbiOverTheUniversalApk() {
        val release = release(
            "NevTune+-v1.8.3.apk" to "https://example.test/universal.apk",
            "NevTune+-v1.8.3_arm64-v8a.apk" to "https://example.test/arm64.apk",
            "NevTune+-v1.8.3_x86_64.apk" to "https://example.test/x86.apk",
        )

        assertEquals(
            "https://example.test/arm64.apk",
            AppUpdateChecker.apkAssetUrl(release, listOf("arm64-v8a", "armeabi-v7a")),
        )
    }

    @Test
    fun followsDeviceAbiPreferenceOrder() {
        val release = release(
            "NevTune+-v1.8.3_arm64-v8a.apk" to "https://example.test/arm64.apk",
            "NevTune+-v1.8.3_x86_64.apk" to "https://example.test/x86.apk",
        )

        assertEquals(
            "https://example.test/x86.apk",
            AppUpdateChecker.apkAssetUrl(release, listOf("x86_64", "arm64-v8a")),
        )
    }

    @Test
    fun fallsBackToUniversalWhenNoSupportedAbiIsPublished() {
        val release = release(
            "NevTune+-v1.8.3.apk" to "https://example.test/universal.apk",
            "NevTune+-v1.8.3_arm64-v8a.apk" to "https://example.test/arm64.apk",
        )

        assertEquals(
            "https://example.test/universal.apk",
            AppUpdateChecker.apkAssetUrl(release, listOf("x86_64")),
        )
    }

    @Test
    fun neverSelectsTheTvApk() {
        val release = release(
            "NevTunePlusTV-v1.8.3.apk" to "https://example.test/tv.apk",
            "NevTune+ TV - 26.11.25.apk" to "https://example.test/tv-new.apk",
            "NevTune+ Wear OS - 26.11.25.apk" to "https://example.test/wear.apk",
        )

        assertEquals(null, AppUpdateChecker.apkAssetUrl(release, listOf("arm64-v8a")))
    }

    @Test
    fun selectsReadableMultiPlatformAndSinglePlatformApkNames() {
        val multiRelease = release(
            "NevTune+ Mobile - 26.11.25.apk" to "https://example.test/mobile.apk",
            "NevTune+ TV - 26.11.25.apk" to "https://example.test/tv.apk",
            "NevTune+ Wear OS - 26.11.25.apk" to "https://example.test/wear.apk",
        )
        assertEquals(
            "https://example.test/mobile.apk",
            AppUpdateChecker.apkAssetUrl(multiRelease, listOf("arm64-v8a"), apkPrefix = "NevTune+-"),
        )
        assertEquals(
            "https://example.test/tv.apk",
            AppUpdateChecker.apkAssetUrl(multiRelease, listOf("arm64-v8a"), apkPrefix = "NevTunePlusTV-"),
        )

        val singleMobileRelease = release(
            "NevTune+ - 26.11.25.apk" to "https://example.test/single.apk",
        )
        assertEquals(
            "https://example.test/single.apk",
            AppUpdateChecker.apkAssetUrl(singleMobileRelease, listOf("arm64-v8a"), apkPrefix = "NevTune+-"),
        )
        assertEquals(
            null,
            AppUpdateChecker.apkAssetUrl(singleMobileRelease, listOf("arm64-v8a"), apkPrefix = "NevTunePlusTV-"),
        )
    }

    @Test
    fun ignoresAssetsThatAreNotUploadedPhoneApks() {
        val release = Json.parseToJsonElement(
            """{"assets":[
                {"name":"NevTune+-v1.8.4.apk","browser_download_url":"https://example.test/pending.apk","state":"new"},
                {"name":"NevTune+-v1.8.4.zip","browser_download_url":"https://example.test/archive.zip","state":"uploaded"},
                {"name":"NevTunePlusTV-v1.8.4.apk","browser_download_url":"https://example.test/tv.apk","state":"uploaded"}
            ]}""",
        ).jsonObject

        assertEquals(null, AppUpdateChecker.apkAssetUrl(release, listOf("arm64-v8a")))
    }

    @Test
    fun comparesNumericVersionPartsAndStableOverPrerelease() {
        assertTrue(AppUpdateChecker.isNewer("1.8.10", "1.8.9"))
        assertTrue(AppUpdateChecker.isNewer("1.8.3", "1.8.3-beta2"))
        assertFalse(AppUpdateChecker.isNewer("1.8.3", "1.8.3"))
        assertFalse(AppUpdateChecker.isNewer("1.8.2", "1.8.3"))
    }

    @Test
    fun releaseComparisonCoversFreshInstallCurrentReleaseAndAvailableUpgrade() {
        // A fresh install runs the same comparison against its bundled version.
        assertFalse(AppUpdateChecker.isNewer("1.8.3", "1.8.3"))
        assertFalse(AppUpdateChecker.isNewer("1.8.2", "1.8.3"))
        assertTrue(AppUpdateChecker.isNewer("1.8.4", "1.8.3"))
    }

    @Test
    fun oldCertificateInstallationsRequireManualMigrationButNewerOnesDoNot() {
        assertTrue(AppUpdateChecker.requiresManualMigration("1.8.7", "1.8.9"))
        assertTrue(AppUpdateChecker.requiresManualMigration("1.8.6", "1.8.9"))
        assertTrue(AppUpdateChecker.requiresManualMigration("1.8.7", "1.8.10"))
        assertFalse(AppUpdateChecker.requiresManualMigration("1.8.8", "1.8.9"))
        assertFalse(AppUpdateChecker.requiresManualMigration("1.8.9", "1.8.10"))
        assertFalse(AppUpdateChecker.requiresManualMigration("1.8.7", "1.8.8"))
    }

    @Test
    fun failedDownloadCanBeRetriedAndProducesTheCompleteApk() {
        val server = MockWebServer()
        val directory = Files.createTempDirectory("nevtuneplus-updater-test").toFile()
        try {
            server.enqueue(MockResponse().setResponseCode(503).setBody("temporarily unavailable"))
            server.enqueue(MockResponse().setResponseCode(200).setBody("valid-apk-payload"))
            server.start()

            val target = directory.resolve("nevtuneplus-1.8.4.apk")
            val client = OkHttpClient()
            assertThrows(IllegalStateException::class.java) {
                AppUpdateChecker.downloadToFile(server.url("/update.apk").toString(), target, client)
            }
            assertFalse(target.exists())

            assertTrue(
                AppUpdateChecker.downloadToFile(server.url("/update.apk").toString(), target, client),
            )
            assertEquals("valid-apk-payload", target.readText())
            assertEquals(2, server.requestCount)
        } finally {
            server.shutdown()
            directory.deleteRecursively()
        }
    }

    @Test
    fun cancelledDownloadDoesNotReportACompletedFile() {
        val server = MockWebServer()
        val directory = Files.createTempDirectory("nevtuneplus-updater-cancel-test").toFile()
        try {
            server.enqueue(MockResponse().setBody("payload-that-must-not-complete"))
            server.start()
            val target = directory.resolve("cancelled.apk")

            assertFalse(
                AppUpdateChecker.downloadToFile(
                    url = server.url("/update.apk").toString(),
                    target = target,
                    isCancelled = { true },
                ),
            )
            assertFalse("A cancelled download must not leave a partial APK", target.exists())
        } finally {
            server.shutdown()
            directory.deleteRecursively()
        }
    }

    private fun release(vararg assets: Pair<String, String>) = Json.parseToJsonElement(
        """{"assets":[${assets.joinToString(",") { (name, url) ->
            """{"name":"$name","browser_download_url":"$url","state":"uploaded"}"""
        }}]}""",
    ).jsonObject
}
