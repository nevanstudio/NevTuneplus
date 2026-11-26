package com.nevanstudio.nevtuneplus.ui

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedRefreshTest {
    @Test
    fun staleRefreshCannotPublishOverANewerHomeLoad() {
        assertEquals(true, shouldPublishHomeRefresh(true, 4L, 4L))
        assertEquals(false, shouldPublishHomeRefresh(true, 4L, 5L))
        assertEquals(false, shouldPublishHomeRefresh(false, 4L, 4L))
    }

    @Test
    fun failedRefreshAlwaysClearsItsOwnRefreshingFlag() = runBlocking {
        val state = MutableStateFlow(setOf("HOME", "LIBRARY"))

        runCatching {
            runFeedRefresh(state, "HOME") {
                throw IllegalStateException("simulated repository failure")
            }
        }

        assertEquals(setOf("LIBRARY"), state.value)
    }

    @Test
    fun successfulRefreshClearsItsOwnRefreshingFlag() = runBlocking {
        val state = MutableStateFlow(setOf("HOME"))

        runFeedRefresh(state, "HOME") { }

        assertEquals(emptySet<String>(), state.value)
    }

    @Test
    fun homeAutoRefreshIsBlockedWithin24HoursAndAllowedAfter24Hours() {
        val t0 = 1_700_000_000_000L
        assertEquals(false, shouldAutoRefreshHome(nowMs = t0, lastRefreshedAtMs = 0L))
        assertEquals(false, shouldAutoRefreshHome(nowMs = t0 + 1_000L, lastRefreshedAtMs = t0))
        assertEquals(
            false,
            shouldAutoRefreshHome(
                nowMs = t0 + HOME_AUTO_REFRESH_INTERVAL_MS - 1L,
                lastRefreshedAtMs = t0,
            ),
        )
        assertEquals(
            true,
            shouldAutoRefreshHome(
                nowMs = t0 + HOME_AUTO_REFRESH_INTERVAL_MS,
                lastRefreshedAtMs = t0,
            ),
        )
        assertEquals(
            true,
            shouldAutoRefreshHome(
                nowMs = t0 + HOME_AUTO_REFRESH_INTERVAL_MS + 60_000L,
                lastRefreshedAtMs = t0,
            ),
        )
    }
}
