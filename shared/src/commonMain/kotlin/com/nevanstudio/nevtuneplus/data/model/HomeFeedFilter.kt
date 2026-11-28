// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.data.model

/**
 * Keeps only personalized, listening-habit, and user-library shelves on the Home screen.
 *
 * Generic Explore mood/genre categories, charts, trending feeds, and broad discovery
 * collections belong on the Explore page rather than Home.
 */
fun filterPersonalizedHomeShelves(shelves: List<HomeShelf>): List<HomeShelf> =
    shelves.filterNot(::isGenericExploreHomeShelf)

fun isGenericExploreHomeShelf(shelf: HomeShelf): Boolean {
    if (shelf.discoveryCategory != null) return true

    val title = shelf.title.trim().lowercase()
    val subtitle = shelf.subtitle.trim().lowercase()

    if (title.isEmpty()) return false
    if (isExplicitlyPersonalizedTitle(title)) return false

    if (subtitle in GENERIC_EXPLORE_SUBTITLES) return true
    if (title in GENERIC_EXPLORE_TITLES) return true
    if (GENERIC_EXPLORE_PHRASES.any { phrase -> title.contains(phrase) }) return true

    return false
}

private fun isExplicitlyPersonalizedTitle(title: String): Boolean {
    if (title in PERSONALIZED_TITLES) return true
    return PERSONALIZED_PREFIXES.any { prefix -> title.startsWith(prefix) }
}

private val PERSONALIZED_TITLES = setOf(
    "recents",
    "recently played",
    "gần đây",
    "最近",
    "listen again",
    "keep listening",
    "nghe lại",
    "もう一度聴く",
    "quick picks",
    "lựa chọn nhanh",
    "chọn nhanh đài phát",
    "クイック ミックス",
    "mixed for you",
    "made for you",
    "dành riêng cho bạn",
    "ミックス",
    "forgotten favorites",
    "old favorites",
    "giai điệu quen thuộc",
    "よく聴いたお気に入りの曲",
    "from your library",
    "từ thư viện của bạn",
    "ライブラリから",
    "from your spotify",
    "made for you on spotify",
    "liked songs",
    "discover weekly",
    "release radar",
    "on repeat",
    "repeat rewind",
    "recommended artists",
    "artists you might like",
    "your favorite artists",
    "recommended albums",
    "recommended playlists",
    "your mixes",
    "your top mixes",
)

private val PERSONALIZED_PREFIXES = listOf(
    "similar to ",
    "tương tự như ",
    "because you listened",
    "more like ",
    "fans also like",
    "daily mix",
    "your ",
)

private val GENERIC_EXPLORE_SUBTITLES = setOf(
    "top tunes right now",
    "from the community",
    "youtube charts",
    "giai điệu hàng đầu hiện nay",
    "từ cộng đồng",
    "bảng xếp hạng youtube",
    "注目の曲",
    "コミュニティより",
    "youtube チャート",
)

private val GENERIC_EXPLORE_TITLES = setOf(
    "charts",
    "top charts",
    "trending",
    "trending songs",
    "trending community playlists",
    "hits",
    "hits today",
    "today's hits",
    "top hits",
    "global hits",
    "viral hits",
    "featured playlists",
    "featured playlists for you",
    "new releases",
    "fresh finds",
    "new music discovery",
    "discover",
    "explore",
    "moods & genres",
    "moods and genres",
    "genres",
    "top music videos",
    "recommended music videos",
    "video charts",
    "concerts",
    "shorts",
    // Broad mood / activity / genre collections that belong exclusively on Explore
    "relax",
    "relaxation",
    "time to relax",
    "chill",
    "chilled",
    "chill out",
    "laidback",
    "laidback + acoustic",
    "unwind",
    "unwind + explore",
    "coffee shop blends",
    "coffee shop",
    "mindfulness",
    "mindfulness & meditation",
    "meditation",
    "peaceful bedtime",
    "bedtime",
    "sleep",
    "yoga",
    "dance club",
    "dance club beats",
    "club",
    "party",
    "workout",
    "fitness",
    "commute",
    "focus",
    "study",
    "energize",
    "energy",
    "feel good",
    "feeling blue",
    "heartbreak",
    "romance",
    "korean wave drive",
    "ambient",
    "sound healing",
    // Localized generic Explore headings supported by HomeScreen
    "bảng xếp hạng",
    "thịnh hành",
    "bản phát hành mới",
    "bản hit hôm nay",
    "video âm nhạc hàng đầu",
    "video âm nhạc đề xuất",
    "buổi hòa nhạc",
    "shorts nổi bật",
    "danh sách phát cộng đồng thịnh hành",
    "danh sách phát thịnh hành trong cộng đồng người dùng",
    "danh sách phát đề xuất cho bạn",
    "danh sách phát nổi bật dành cho bạn",
    "チャート",
    "急上昇",
    "最新リリース",
    "今日のヒット曲",
    "人気のミュージック ビデオ",
    "おすすめのミュージック ビデオ",
    "コンサート",
    "ショート",
    "急上昇のコミュニティ再生リスト",
    "おすすめの再生リスト",
)

private val GENERIC_EXPLORE_PHRASES = listOf(
    "relaxation",
    "time to relax",
    "dance club",
    "mindfulness",
    "peaceful bedtime",
    "coffee shop",
    "music discovery",
    "community playlists",
    "youtube charts",
    "hits today",
    "today's hits",
    "top hits",
    "trending",
    "korean wave",
    "unwind +",
    "laidback +",
)
