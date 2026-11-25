// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.data

import androidx.core.net.toUri

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.nevanstudio.nevtuneplus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import java.io.File

/**
 * NevTune+ ships as a sideloaded APK off GitHub Releases rather than through
 * a store, so there's nothing to push an update notice on its own — this
 * polls the repo's "latest release" once per launch and compares its tag
 * against the running build.
 *
 * The update itself is also handled here: the release's `.apk` asset is
 * downloaded into the app's cache and handed to the system package installer,
 * so the whole round trip stays inside the app instead of bouncing out to a
 * browser.
 */
object AppUpdateChecker {

    data class UpdateInfo(
        val version: String,
        val releaseUrl: String,
        val apkUrl: String?,
        /** The release's own Markdown body, shown as this update's "what's new". */
        val notes: String?,
        /** Old installations cannot be updated in place because the release certificate changed. */
        val requiresManualMigration: Boolean,
    )

    data class LatestReleaseInfo(
        val version: String,
        val releaseUrl: String,
        val apkUrl: String?,
        val notes: String?,
        val updateAvailable: Boolean,
    )

    private const val CACHE_SUBDIR = "updates"

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/nevanstudio/NevTuneplus/releases/latest"
    private const val WEB_LATEST_RELEASE_URL =
        "https://github.com/nevanstudio/NevTuneplus/releases/latest"
    private const val WEB_REPO_PATH = "/nevanstudio/NevTuneplus"

    private val json = Json { ignoreUnknownKeys = true }

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available = _available.asStateFlow()

    private val _latestRelease = MutableStateFlow<LatestReleaseInfo?>(null)
    val latestRelease = _latestRelease.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking = _checking.asStateFlow()

    /** Where this update's APK download currently stands, for the dialog's progress row. */
    sealed interface DownloadState {
        data object Idle : DownloadState
        data class Downloading(val fraction: Float) : DownloadState
        data class Ready(val file: File) : DownloadState
        data class Failed(val message: String) : DownloadState
    }

    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download = _download.asStateFlow()

    /** Set from the UI thread when the user cancels; polled between network reads. */
    @Volatile
    private var downloadCancelled = false

    @Volatile
    private var activeDownloadCall: Call? = null

    suspend fun check(): Boolean? = withContext(Dispatchers.IO) {
        _checking.value = true
        try {
            runCatching {
            val release = fetchLatestReleaseJson()
            val tag = release["tag_name"]?.jsonPrimitive?.contentOrNull
                ?: error("Release tag missing")
            val url = release["html_url"]?.jsonPrimitive?.contentOrNull
                ?: "https://github.com$WEB_REPO_PATH/releases/tag/$tag"
            val apkUrl = apkAssetUrl(release)
            val notes = release["body"]?.jsonPrimitive?.contentOrNull
            val latest = tag.removePrefix("v")
            val updateAvailable = isNewer(latest, BuildConfig.VERSION_NAME)
            val latestInfo = LatestReleaseInfo(
                version = latest,
                releaseUrl = url,
                apkUrl = apkUrl,
                notes = notes,
                updateAvailable = updateAvailable,
            )
            val result = if (updateAvailable) {
                _available.value = UpdateInfo(
                    version = latest,
                    releaseUrl = url,
                    apkUrl = apkUrl,
                    notes = notes,
                    requiresManualMigration = requiresManualMigration(BuildConfig.VERSION_NAME, latest),
                )
                true
            } else {
                _available.value = null
                false
            }
            _latestRelease.value = latestInfo
            result
            }.getOrNull()
        } finally {
            _checking.value = false
        }
    }

    /**
     * Queries the GitHub REST API first and falls back to GitHub's web release
     * endpoints when unauthenticated API requests are rate-limited (HTTP 403/429).
     */
    private fun fetchLatestReleaseJson(): JsonObject {
        val apiRequest = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        val apiFailure = runCatching {
            Http.client.newCall(apiRequest).execute().use { response ->
                check(response.isSuccessful) { "Update check failed: HTTP ${response.code}" }
                val body = response.body?.string() ?: error("Empty update response")
                return json.parseToJsonElement(body) as? JsonObject
                    ?: error("Invalid update response")
            }
        }.exceptionOrNull()

        val webRequest = Request.Builder()
            .url(WEB_LATEST_RELEASE_URL)
            .header("Accept", "application/json")
            .build()
        val tag = Http.client.newCall(webRequest).execute().use { response ->
            check(response.isSuccessful) {
                apiFailure?.message ?: "Update check failed: HTTP ${response.code}"
            }
            val body = response.body?.string() ?: error("Empty release metadata")
            val obj = json.parseToJsonElement(body) as? JsonObject
                ?: error("Invalid release metadata")
            obj["tag_name"]?.jsonPrimitive?.contentOrNull
                ?: error("Release tag missing")
        }

        val assetsHtml = runCatching {
            val eaRequest = Request.Builder()
                .url("https://github.com$WEB_REPO_PATH/releases/expanded_assets/$tag")
                .build()
            Http.client.newCall(eaRequest).execute().use { response ->
                if (response.isSuccessful) response.body?.string().orEmpty() else ""
            }
        }.getOrDefault("")

        return parseExpandedAssetsJson(tag, assetsHtml)
    }

    internal fun parseExpandedAssetsJson(tag: String, html: String): JsonObject {
        val pattern = Regex(
            """href="(${Regex.escape(WEB_REPO_PATH)}/releases/download/${Regex.escape(tag)}/([^"]+))"""",
            RegexOption.IGNORE_CASE,
        )
        val seen = HashSet<String>()
        val assetsArray = buildJsonArray {
            for (match in pattern.findAll(html)) {
                val relPath = match.groupValues[1]
                val name = match.groupValues[2]
                if (!seen.add(name)) continue
                addJsonObject {
                    put("name", name)
                    put("browser_download_url", "https://github.com$relPath")
                    put("state", "uploaded")
                }
            }
        }
        return buildJsonObject {
            put("tag_name", tag)
            put("html_url", "https://github.com$WEB_REPO_PATH/releases/tag/$tag")
            put("assets", assetsArray)
        }
    }

    /**
     * Wipes any APK left over from a previous run. Called once at cold start
     * so a downloaded update is only ever "Install Now" for the session that
     * downloaded it — the next launch starts clean rather than trying to work
     * out whether a leftover file is still good.
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        File(context.cacheDir, CACHE_SUBDIR).listFiles()?.forEach { it.delete() }
    }

    /**
     * One release carries the phone app and the TV app, each as a universal
     * APK plus per-ABI splits:
     *
     *     Audify-v1.8.apk     Audify-v1.8_arm64-v8a.apk     ...
     *     AudifyTV-v1.8.apk   AudifyTV-v1.8_arm64-v8a.apk   ...
     *
     * so this app's APKs are the ones named with its own
     * [BuildConfig.UPDATE_APK_PREFIX] — "Audify-" never matches a TV file,
     * since the prefix ends at the hyphen. The universal build (no `_ABI`
     * suffix) is used only as a fallback. The matching ABI split is preferred
     * to avoid downloading a much larger universal APK. A release with neither leaves
     * [UpdateInfo.apkUrl] null and the UI falls back to opening the releases
     * page.
     */
    private val TV_ASSET_REGEX = Regex(
        """(^|[-_.\s])tv([-_.\s]|$)|androidtv|nevtuneplustv|audifytv""",
        RegexOption.IGNORE_CASE,
    )
    private val WEAR_ASSET_REGEX = Regex(
        """(^|[-_.\s])(wear|watch)([-_.\s]|$)|nevtunepluswear""",
        RegexOption.IGNORE_CASE,
    )

    internal fun apkAssetUrl(
        release: JsonObject,
        supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList(),
        apkPrefix: String = BuildConfig.UPDATE_APK_PREFIX,
    ): String? = runCatching {
        val isTvApp = apkPrefix.contains("TV", ignoreCase = true)
        val apks = release["assets"]?.jsonArray
            ?.mapNotNull { it as? JsonObject }
            ?.mapNotNull { asset ->
                val name = asset["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val url = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val isNevTuneApk =
                    name.startsWith(apkPrefix, ignoreCase = true) ||
                        name.startsWith("NevTune+", ignoreCase = true) ||
                        name.startsWith("NevTunePlus", ignoreCase = true) ||
                        name.startsWith("Audify", ignoreCase = true)
                val baseName = name.removeSuffix(".apk").removeSuffix(".APK")
                val isTvAsset = TV_ASSET_REGEX.containsMatchIn(baseName)
                val isWearAsset = WEAR_ASSET_REGEX.containsMatchIn(baseName)
                val matchesTarget = if (isTvApp) isTvAsset && !isWearAsset else !isTvAsset && !isWearAsset
                val ours = isNevTuneApk &&
                    matchesTarget &&
                    name.endsWith(".apk", ignoreCase = true) &&
                    asset["state"]?.jsonPrimitive?.contentOrNull == "uploaded"
                if (ours) baseName to url else null
            }
            .orEmpty()
        supportedAbis.firstNotNullOfOrNull { abi ->
            apks.firstOrNull { (name, _) -> name.endsWith("_$abi") }?.second
        } ?: apks.firstOrNull { (name, _) -> '_' !in name }?.second
    }.getOrNull()

    /**
     * Streams the current update's APK into the app cache, reporting progress
     * through [download]. A finished file survives a cancelled dialog: until
     * the state is reset, "Install Now" comes straight back without a second
     * download.
     */
    suspend fun downloadApk(context: Context): Unit = withContext(Dispatchers.IO) {
        val info = _available.value ?: return@withContext
        val url = info.apkUrl ?: return@withContext
        downloadCancelled = false
        _download.value = DownloadState.Downloading(0f)

        runCatching {
            val dir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
            // Drop anything left over from an earlier attempt.
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "nevtuneplus-${info.version}.apk")

            val completed = downloadToFile(
                url = url,
                target = target,
                client = Http.client,
                isCancelled = { downloadCancelled },
                onProgress = { fraction -> _download.value = DownloadState.Downloading(fraction) },
                onCallCreated = { activeDownloadCall = it },
            )
            if (!completed) {
                target.delete()
                _download.value = DownloadState.Idle
                return@withContext
            }
            _download.value = DownloadState.Ready(target)
        }.onFailure { error ->
            File(context.cacheDir, CACHE_SUBDIR).listFiles()
                ?.filter { it.name == "nevtuneplus-${info.version}.apk" }
                ?.forEach { it.delete() }
            _download.value = if (downloadCancelled) {
                DownloadState.Idle
            } else {
                DownloadState.Failed(error.message ?: "Download failed")
            }
        }
    }

    /** Stops an in-flight download; the next read loop sees this and bails. */
    fun cancelDownload() {
        downloadCancelled = true
        activeDownloadCall?.cancel()
    }

    /** Back to square one after a failure, so the dialog offers Download again. */
    fun resetDownload() {
        _download.value = DownloadState.Idle
    }

    /** Performs the HTTP/file portion independently of Android Context and UI state. */
    internal fun downloadToFile(
        url: String,
        target: File,
        client: OkHttpClient = Http.client,
        isCancelled: () -> Boolean = { false },
        onProgress: (Float) -> Unit = {},
        onCallCreated: (Call?) -> Unit = {},
    ): Boolean {
        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)
        onCallCreated(call)
        try {
            call.execute().use { response ->
                check(response.isSuccessful) { "Download failed: HTTP ${response.code}" }
                val body = response.body ?: error("Empty download body")
                val total = body.contentLength().takeIf { it > 0 }

                body.byteStream().use { input ->
                    var cancelled = false
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var readTotal = 0L
                        while (true) {
                            if (isCancelled()) {
                                cancelled = true
                                break
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            readTotal += read
                            total?.let { onProgress((readTotal.toFloat() / it).coerceIn(0f, 1f)) }
                        }
                    }
                    if (cancelled) {
                        target.delete()
                        return false
                    }
                }
            }
        } finally {
            onCallCreated(null)
        }
        return true
    }

    /** v1.8.7 and older use the retired certificate and must be reinstalled manually. */
    internal fun requiresManualMigration(current: String, latest: String): Boolean =
        !isNewer(current, "1.8.7") && !isNewer("1.8.9", latest)

    /**
     * Hands a downloaded APK to the system installer.
     *
     * Sideloaded apps need the user's blessing per app ("install unknown apps");
     * without it the installer intent silently does nothing on most ROMs, so
     * the user is sent to that one switch first and taps Install again after.
     * Some TV builds have no such settings screen; there the installer is
     * launched anyway and shows its own "not allowed" prompt with a way in.
     */
    fun installApk(context: Context, file: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            val opened = runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData("package:${context.packageName}".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.isSuccess
            if (opened) return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_INSTALL_PACKAGE)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                .putExtra(Intent.EXTRA_RETURN_RESULT, true)
                .addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK,
                ),
        )
    }

    /** A version split into its numeric dotted parts and whether it carries a "-suffix" (e.g. "-beta2"). */
    private data class ParsedVersion(val parts: List<Int>, val isPreRelease: Boolean)

    private fun parseVersion(raw: String): ParsedVersion {
        val dash = raw.indexOf('-')
        val base = if (dash >= 0) raw.substring(0, dash) else raw
        return ParsedVersion(base.split(".").map { it.toIntOrNull() ?: 0 }, dash >= 0)
    }

    /**
     * Numeric, dot-separated comparison — "1.10" outranks "1.9" — with one
     * extra rule: a "-betaN" build (see the debug build type's
     * `versionNameSuffix` in app/build.gradle.kts) is treated as older than a
     * plain release of the same numbers, since the beta by definition predates
     * the tag it was testing toward. Without this, a beta and the release it
     * matches compare equal and testers never get nudged onto the real build.
     */
    internal fun isNewer(latest: String, current: String): Boolean {
        val l = parseVersion(latest)
        val c = parseVersion(current)
        for (i in 0 until maxOf(l.parts.size, c.parts.size)) {
            val a = l.parts.getOrElse(i) { 0 }
            val b = c.parts.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return c.isPreRelease && !l.isPreRelease
    }
}
