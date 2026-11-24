// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.data.webdav

/**
 * In-memory credentials for [com.nevanstudio.nevtuneplus.data.Http]'s interceptor.
 *
 * Held outside SharedPreferences so the network layer never touches prefs:
 * [com.nevanstudio.nevtuneplus.data.settings.AppSettings] publishes here on init and
 * on every WebDAV edit.
 */
object WebDavAuth {
    @Volatile var host: String? = null
    @Volatile var authHeader: String? = null

    /** [host] and [header] as WebDavConfig derives them from the saved server. */
    fun update(host: String?, header: String?) {
        this.host = host
        authHeader = header
    }

    fun shouldAuthorize(requestHost: String): Boolean {
        val expected = host ?: return false
        val header = authHeader ?: return false
        if (header.isBlank()) return false
        return requestHost.equals(expected, ignoreCase = true)
    }
}
