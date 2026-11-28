// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFeedFilterTest {
    @Test
    fun removesGenericExploreCategoriesAndDiscoveryShelves() {
        val shelves = listOf(
            HomeShelf("Recents", listOf(track("t1"))),
            HomeShelf("Coffee shop blends", listOf(card("c1"))),
            HomeShelf("Featured playlists", listOf(card("c2"))),
            HomeShelf("Laidback + acoustic", listOf(card("c3"))),
            HomeShelf("Unwind + explore", listOf(card("c4"))),
            HomeShelf("Mindfulness & meditation", listOf(card("c5"))),
            HomeShelf("Time to relax", listOf(card("c6"))),
            HomeShelf("Chilled", listOf(card("c7"))),
            HomeShelf("Peaceful bedtime", listOf(card("c8"))),
            HomeShelf("Yoga", listOf(card("c9"))),
            HomeShelf("Korean wave drive", listOf(card("c10"))),
            HomeShelf("Dance club beats", listOf(card("c11"))),
            HomeShelf("New music discovery", listOf(card("c12"))),
            HomeShelf("Hits today", listOf(card("c13"))),
            HomeShelf("Charts", listOf(card("c14")), subtitle = "YouTube Charts"),
            HomeShelf("Relaxation", listOf(card("c15")), discoveryCategory = "Relax"),
        )

        val filtered = filterPersonalizedHomeShelves(shelves)
        assertEquals(listOf("Recents"), filtered.map { it.title })
    }

    @Test
    fun preservesPersonalizedAndListeningBasedShelvesEvenWhenTitleContainsMoodWords() {
        val shelves = listOf(
            HomeShelf("Recently played", listOf(track("t1"))),
            HomeShelf("Listen again", listOf(track("t2"))),
            HomeShelf("Quick picks", listOf(track("t3"))),
            HomeShelf("Mixed for you", listOf(card("c1"))),
            HomeShelf("Similar to Chillhop Essentials", listOf(card("c2"))),
            HomeShelf("Similar to Dance Gavin Dance", listOf(card("c3"))),
            HomeShelf("Forgotten favorites", listOf(track("t4"))),
            HomeShelf("From your library", listOf(card("c4"))),
            HomeShelf("Recommended artists", listOf(card("c5"))),
        )

        val filtered = filterPersonalizedHomeShelves(shelves)
        assertEquals(shelves.map { it.title }, filtered.map { it.title })
    }

    @Test
    fun categoryTaggedShelvesAreAlwaysExcludedFromHome() {
        val tagged = HomeShelf("For you", listOf(card("c1")), discoveryCategory = "Pop")
        assertTrue(isGenericExploreHomeShelf(tagged))
        assertFalse(isGenericExploreHomeShelf(HomeShelf("For you", listOf(card("c1")))))
    }

    private fun track(id: String) = ShelfItem(id, "Artist", null, videoId = id, browseId = null)
    private fun card(id: String) = ShelfItem(id, "Playlist", null, videoId = null, browseId = id)
}
