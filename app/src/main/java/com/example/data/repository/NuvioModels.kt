package com.example.data.repository

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NuvioManifest(
    val name: String? = null,
    val version: String? = null,
    val scrapers: List<NuvioScraper> = emptyList()
)

@JsonClass(generateAdapter = true)
data class NuvioScraper(
    val id: String,
    val name: String,
    val description: String? = null,
    val version: String? = null,
    val author: String? = null,
    val supportedTypes: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val filename: String,
    val enabled: Boolean = true,
    val formats: List<String> = emptyList(),
    val logo: String? = null,
    val hashVerified: Boolean = false,
    val androidCompatible: Boolean = true,
    var isPinned: Boolean = false,
    var repoBaseUrl: String? = null,
    var repoName: String? = null
)

data class NuvioStream(
    val name: String,
    val title: String? = null,
    val url: String,
    val quality: String? = null,
    val size: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val provider: String? = null,
    val repoGroup: String? = "Direct Cinema",
    val subtitles: List<NuvioSubtitle> = emptyList()
)

data class NuvioSubtitle(
    val url: String,
    val language: String? = null,
    val name: String? = null,
    val headers: Map<String, String> = emptyMap()
)

enum class PlaybackMode {
    DIRECT, // Nuvio Direct ExoPlayer
    EMBED   // Web Embed Player (VidSrc / VixSrc)
}
