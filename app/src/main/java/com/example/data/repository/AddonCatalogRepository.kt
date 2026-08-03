package com.example.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class AddonCatalogRow(
    val providerId: String,
    val providerName: String,
    val catalogTitle: String,
    val items: List<MediaItem>
)

class AddonCatalogRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchAddonCatalogs(activeProviders: List<ProviderItem>): List<AddonCatalogRow> = withContext(Dispatchers.IO) {
        val resultRows = mutableListOf<AddonCatalogRow>()
        val enabledProviders = activeProviders.filter { it.isEnabled }

        for (provider in enabledProviders) {
            try {
                if (provider.category == ProviderCategory.STREMIO_ADDON) {
                    fetchStremioCatalogs(provider, resultRows)
                } else if (provider.category == ProviderCategory.NUVIO_PLUGIN) {
                    fetchGenericRepoCatalogs(provider, resultRows)
                }
            } catch (e: Exception) {
                Log.e("AddonCatalogRepo", "Error fetching catalog for ${provider.name}: ${e.message}")
            }
        }

        resultRows
    }

    private fun resolveImdbToTmdb(rawId: String, type: String): Int? {
        if (rawId.toIntOrNull() != null) return rawId.toInt()
        if (!rawId.startsWith("tt")) return null
        return try {
            val apiKey = com.example.BuildConfig.TMDB_API_KEY.ifEmpty { "54c3dc672ff2776242c7a468984bcf05" }
            val url = "https://api.themoviedb.org/3/find/$rawId?api_key=$apiKey&external_source=imdb_id"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val jsonObj = JSONObject(body)
                        val movieArr = jsonObj.optJSONArray("movie_results")
                        if (movieArr != null && movieArr.length() > 0) {
                            return movieArr.getJSONObject(0).optInt("id")
                        }
                        val tvArr = jsonObj.optJSONArray("tv_results")
                        if (tvArr != null && tvArr.length() > 0) {
                            return tvArr.getJSONObject(0).optInt("id")
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMetaToMediaItem(meta: JSONObject, defaultType: String): MediaItem? {
        val rawId = meta.optString("id", "")
        val imdbId = meta.optString("imdb_id", if (rawId.startsWith("tt")) rawId else "")
        val moviedbId = meta.optInt("moviedb_id", meta.optInt("tmdb_id", meta.optInt("tmdbId", 0)))

        val name = meta.optString("name", meta.optString("title", "")).trim()
        if (rawId.isEmpty() || name.isEmpty()) return null

        val desc = meta.optString("description", meta.optString("overview", ""))
        val releaseInfo = meta.optString("releaseInfo", meta.optString("year", "2025"))
        val imdbRating = meta.optDouble("imdbRating", 8.0)
        val metaType = meta.optString("type", defaultType)

        val rawPoster = meta.optString("poster", meta.optString("poster_path", ""))
        val rawBackdrop = meta.optString("background", meta.optString("backdrop_path", ""))

        val finalPosterUrl = when {
            rawPoster.startsWith("http://") || rawPoster.startsWith("https://") -> rawPoster
            rawPoster.startsWith("/") -> "https://image.tmdb.org/t/p/w500$rawPoster"
            imdbId.startsWith("tt") -> "https://images.metahub.space/poster/medium/$imdbId/img"
            else -> null
        }

        val finalBackdropUrl = when {
            rawBackdrop.startsWith("http://") || rawBackdrop.startsWith("https://") -> rawBackdrop
            rawBackdrop.startsWith("/") -> "https://image.tmdb.org/t/p/w1280$rawBackdrop"
            imdbId.startsWith("tt") -> "https://images.metahub.space/background/medium/$imdbId/img"
            else -> finalPosterUrl
        }

        val resolvedTmdbId = if (moviedbId > 0) moviedbId else resolveImdbToTmdb(imdbId.ifEmpty { rawId }, metaType)

        val finalId = when {
            resolvedTmdbId != null && resolvedTmdbId > 0 -> resolvedTmdbId
            moviedbId > 0 -> moviedbId
            rawId.toIntOrNull() != null -> rawId.toInt()
            else -> abs(rawId.hashCode())
        }

        return MediaItem(
            id = finalId,
            title = name,
            overview = desc,
            posterUrl = finalPosterUrl,
            backdropUrl = finalBackdropUrl ?: finalPosterUrl,
            mediaType = if (metaType == "series" || metaType == "tv") "tv" else "movie",
            voteAverage = if (imdbRating > 0) imdbRating else 8.0,
            releaseYear = releaseInfo,
            genreIds = emptyList()
        )
    }

    private suspend fun fetchStremioCatalogs(provider: ProviderItem, resultRows: MutableList<AddonCatalogRow>) {
        val manifestUrl = provider.url
        val req = Request.Builder()
            .url(manifestUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return
            val bodyStr = resp.body?.string() ?: return
            val jsonObj = JSONObject(bodyStr)
            val catalogsArr = jsonObj.optJSONArray("catalogs") ?: return

            val cleanRepoBase = if (provider.url.endsWith("manifest.json")) {
                provider.url.substringBefore("manifest.json")
            } else if (provider.url.endsWith("/")) {
                provider.url
            } else {
                "${provider.url}/"
            }

            for (i in 0 until minOf(catalogsArr.length(), 6)) {
                val catObj = catalogsArr.optJSONObject(i) ?: continue
                val type = catObj.optString("type", "movie")
                val catId = catObj.optString("id", "top")
                val catName = catObj.optString("name", "${provider.name} Catalog")

                val catUrl = if (cleanRepoBase.endsWith("/")) {
                    "${cleanRepoBase}catalog/$type/$catId.json"
                } else {
                    "$cleanRepoBase/catalog/$type/$catId.json"
                }

                try {
                    val catReq = Request.Builder().url(catUrl).header("User-Agent", "Mozilla/5.0").build()
                    client.newCall(catReq).execute().use { catResp ->
                        if (catResp.isSuccessful) {
                            val catBody = catResp.body?.string() ?: return@use
                            val metasObj = JSONObject(catBody)
                            val metasArr = metasObj.optJSONArray("metas") ?: JSONArray()
                            val mediaItems = mutableListOf<MediaItem>()

                            for (j in 0 until minOf(metasArr.length(), 20)) {
                                val meta = metasArr.optJSONObject(j) ?: continue
                                val item = parseMetaToMediaItem(meta, type)
                                if (item != null && mediaItems.none { it.id == item.id && it.mediaType == item.mediaType }) {
                                    mediaItems.add(item)
                                }
                            }

                            if (mediaItems.isNotEmpty()) {
                                resultRows.add(
                                    AddonCatalogRow(
                                        providerId = provider.id,
                                        providerName = provider.name,
                                        catalogTitle = catName,
                                        items = mediaItems
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AddonCatalogRepo", "Failed cat item load $catUrl: ${e.message}")
                }
            }
        }
    }

    suspend fun searchStremioAddons(query: String, activeProviders: List<ProviderItem>): List<MediaItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MediaItem>()
        val stremioProviders = activeProviders.filter { it.isEnabled && it.category == ProviderCategory.STREMIO_ADDON }
        val encodedQuery = try { java.net.URLEncoder.encode(query, "UTF-8") } catch (e: Exception) { query }

        for (provider in stremioProviders) {
            val cleanRepoBase = if (provider.url.endsWith("manifest.json")) {
                provider.url.substringBefore("manifest.json")
            } else if (provider.url.endsWith("/")) {
                provider.url
            } else {
                "${provider.url}/"
            }

            val typesToSearch = listOf("movie", "series")
            for (type in typesToSearch) {
                val searchUrl = "${cleanRepoBase}catalog/$type/top/search=$encodedQuery.json"
                try {
                    val req = Request.Builder().url(searchUrl).header("User-Agent", "Mozilla/5.0").build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bodyStr = resp.body?.string()
                            if (!bodyStr.isNullOrEmpty()) {
                                val jsonObj = JSONObject(bodyStr)
                                val metasArr = jsonObj.optJSONArray("metas")
                                if (metasArr != null) {
                                    for (i in 0 until minOf(metasArr.length(), 15)) {
                                        val meta = metasArr.optJSONObject(i) ?: continue
                                        val item = parseMetaToMediaItem(meta, type)
                                        if (item != null && results.none { it.id == item.id && it.mediaType == item.mediaType }) {
                                            results.add(item)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AddonCatalogRepo", "Error searching $searchUrl: ${e.message}")
                }
            }
        }
        results
    }

    private suspend fun fetchGenericRepoCatalogs(provider: ProviderItem, resultRows: MutableList<AddonCatalogRow>) {
        val req = Request.Builder()
            .url(provider.url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return
            val bodyStr = resp.body?.string() ?: return
            val trimmed = bodyStr.trim()
            if (!trimmed.startsWith("[")) return

            val jsonArr = JSONArray(trimmed)
            val mediaItems = mutableListOf<MediaItem>()

            for (i in 0 until minOf(jsonArr.length(), 20)) {
                val itemObj = jsonArr.optJSONObject(i) ?: continue
                val name = itemObj.optString("name", itemObj.optString("title", ""))
                val desc = itemObj.optString("description", "Plugin Provider Catalog Item")
                val poster = itemObj.optString("poster", itemObj.optString("iconUrl", ""))

                if (name.isNotEmpty()) {
                    val intId = abs(name.hashCode())
                    mediaItems.add(
                        MediaItem(
                            id = intId,
                            title = name,
                            overview = desc,
                            posterUrl = poster.takeIf { it.startsWith("http") },
                            backdropUrl = poster.takeIf { it.startsWith("http") },
                            mediaType = "movie",
                            voteAverage = 8.5,
                            releaseYear = "2025",
                            genreIds = emptyList()
                        )
                    )
                }
            }

            if (mediaItems.isNotEmpty()) {
                resultRows.add(
                    AddonCatalogRow(
                        providerId = provider.id,
                        providerName = provider.name,
                        catalogTitle = "Extensions Index",
                        items = mediaItems
                    )
                )
            }
        }
    }
}
