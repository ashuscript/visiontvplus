package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ManifestSource(
    val name: String,
    val manifestUrl: String,
    val repoBaseUrl: String
)

class NuvioRepository(private val context: Context? = null) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences("nuvio_plugins_prefs", Context.MODE_PRIVATE)

    private val disabledPluginIds: MutableSet<String> = run {
        val saved = prefs?.getStringSet("disabled_plugin_ids", null)
        saved?.toMutableSet() ?: mutableSetOf()
    }

    private val pinnedPluginIds: MutableSet<String> = run {
        val saved = prefs?.getStringSet("pinned_plugin_ids", null)
        saved?.toMutableSet() ?: mutableSetOf()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val manifestAdapter = moshi.adapter(NuvioManifest::class.java)

    private val defaultManifestSources = listOf(
        ManifestSource(
            name = "TenguPlay Addon",
            manifestUrl = "https://tenguplay.vercel.app/manifest.json",
            repoBaseUrl = "https://tenguplay.vercel.app/"
        ),
        ManifestSource(
            name = "OpenSubtitles v3",
            manifestUrl = "https://opensubtitles-v3.strem.io/manifest.json",
            repoBaseUrl = "https://opensubtitles-v3.strem.io/"
        ),
        ManifestSource(
            name = "AnimeKitsu Subtitles",
            manifestUrl = "https://animekitsu.strem.fun/manifest.json",
            repoBaseUrl = "https://animekitsu.strem.fun/"
        )
    )

    fun togglePluginEnabled(pluginId: String, isEnabled: Boolean) {
        if (isEnabled) {
            disabledPluginIds.remove(pluginId)
        } else {
            disabledPluginIds.add(pluginId)
        }
        prefs?.edit()?.putStringSet("disabled_plugin_ids", HashSet(disabledPluginIds))?.apply()
    }

    fun togglePluginPinned(pluginId: String, isPinned: Boolean) {
        if (isPinned) {
            pinnedPluginIds.add(pluginId)
        } else {
            pinnedPluginIds.remove(pluginId)
        }
        prefs?.edit()?.putStringSet("pinned_plugin_ids", HashSet(pinnedPluginIds))?.apply()
    }

    suspend fun getAllPlugins(activeProviders: List<ProviderItem>? = null): List<NuvioScraper> = withContext(Dispatchers.IO) {
        val scrapersList = mutableListOf<NuvioScraper>()
        val listAdapter = moshi.adapter(List::class.java)

        val sourcesToFetch = if (!activeProviders.isNullOrEmpty()) {
            activeProviders.filter { it.isEnabled }.map {
                ManifestSource(
                    name = it.name,
                    manifestUrl = it.url,
                    repoBaseUrl = if (it.repoBaseUrl.isNotEmpty()) it.repoBaseUrl else it.url
                )
            }
        } else {
            defaultManifestSources
        }

        for (source in sourcesToFetch) {
            try {
                val req = Request.Builder().url(source.manifestUrl).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string()
                        if (!bodyStr.isNullOrEmpty()) {
                            var manifestParsed = false

                            // Check NuvioManifest format
                            try {
                                val manifest = manifestAdapter.fromJson(bodyStr)
                                if (manifest != null && manifest.scrapers.isNotEmpty()) {
                                    val disabledKeywords = listOf("movie_blast", "movieblast")
                                    manifest.scrapers
                                        .filter { disabledKeywords.none { kw -> it.id.lowercase().contains(kw) || it.name.lowercase().contains(kw) } }
                                        .forEach { scraper ->
                                            val pId = scraper.id
                                            val isEnabled = !disabledPluginIds.contains(pId)
                                            val isPinned = pinnedPluginIds.contains(pId)
                                            val copyScraper = scraper.copy(
                                                enabled = isEnabled,
                                                isPinned = isPinned,
                                                repoBaseUrl = source.repoBaseUrl,
                                                repoName = source.name
                                            )
                                            if (scrapersList.none { existing -> existing.id == copyScraper.id }) {
                                                scrapersList.add(copyScraper)
                                            }
                                        }
                                    manifestParsed = true
                                }
                            } catch (e: Exception) {
                                // Ignore
                            }

                            // Check CloudStream Repo with pluginLists or plugins array
                            if (!manifestParsed) {
                                try {
                                    val mapAdapter = moshi.adapter(Map::class.java)
                                    val rawMap = mapAdapter.fromJson(bodyStr) as? Map<String, Any?>

                                    val pluginListsUrls = rawMap?.get("pluginLists") as? List<*>
                                    val directPlugins = (rawMap?.get("plugins") as? List<*>)
                                        ?: (rawMap?.get("extensions") as? List<*>)
                                        ?: (rawMap?.get("scrapers") as? List<*>)

                                    val allPluginMaps = mutableListOf<Map<String, Any?>>()

                                    if (!pluginListsUrls.isNullOrEmpty()) {
                                        for (pUrl in pluginListsUrls) {
                                            val listUrl = pUrl as? String ?: continue
                                            try {
                                                val pReq = Request.Builder().url(listUrl).build()
                                                client.newCall(pReq).execute().use { pResp ->
                                                    if (pResp.isSuccessful) {
                                                        val pBody = pResp.body?.string()
                                                        if (!pBody.isNullOrEmpty() && pBody.trim().startsWith("[")) {
                                                            val rawArray = listAdapter.fromJson(pBody) as? List<Map<String, Any?>>
                                                            if (rawArray != null) {
                                                                allPluginMaps.addAll(rawArray)
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e("NuvioRepo", "Error fetching pluginList $listUrl: ${e.message}")
                                            }
                                        }
                                    }

                                    if (directPlugins != null) {
                                        directPlugins.forEach { item ->
                                            (item as? Map<String, Any?>)?.let { allPluginMaps.add(it) }
                                        }
                                    }

                                    if (allPluginMaps.isNotEmpty()) {
                                        allPluginMaps.forEach { pluginMap ->
                                            val pName = pluginMap["name"] as? String
                                                ?: pluginMap["internalName"] as? String
                                                ?: pluginMap["pluginName"] as? String ?: ""
                                            val pUrl = pluginMap["url"] as? String
                                                ?: pluginMap["file"] as? String
                                                ?: pluginMap["downloadUrl"] as? String ?: ""

                                            if (pName.isNotEmpty()) {
                                                val pId = "cs_${source.name.lowercase().replace(" ", "_")}_${pName.lowercase().replace(" ", "_")}"
                                                val isEnabled = !disabledPluginIds.contains(pId)
                                                val isPinned = pinnedPluginIds.contains(pId)

                                                @Suppress("UNCHECKED_CAST")
                                                val authorsList = (pluginMap["authors"] as? List<String>)?.joinToString(", ")
                                                    ?: pluginMap["author"] as? String ?: "CloudStream Dev"

                                                val description = pluginMap["description"] as? String
                                                    ?: pluginMap["details"] as? String ?: "Multi Language Movies and Series Provider"

                                                val version = (pluginMap["version"] ?: pluginMap["ver"])?.toString()?.let { "Current v$it" } ?: "Current v26"

                                                val iconUrl = pluginMap["iconUrl"] as? String ?: pluginMap["logo"] as? String

                                                val rawLangs = pluginMap["language"] ?: pluginMap["languages"] ?: pluginMap["lang"]
                                                val languages = when (rawLangs) {
                                                    is List<*> -> rawLangs.mapNotNull { it?.toString()?.uppercase() }
                                                    is String -> listOf(rawLangs.uppercase())
                                                    else -> listOf("HI", "EN")
                                                }

                                                @Suppress("UNCHECKED_CAST")
                                                val rawTypes = pluginMap["tvTypes"] as? List<String> ?: pluginMap["types"] as? List<String> ?: listOf("Movie", "TvSeries")

                                                val pluginScraper = NuvioScraper(
                                                    id = pId,
                                                    name = pName,
                                                    author = authorsList,
                                                    description = description,
                                                    version = version,
                                                    languages = languages,
                                                    supportedTypes = rawTypes,
                                                    filename = if (pUrl.isNotEmpty()) pUrl else "CLOUDSTREAM_EXTENSION.js",
                                                    enabled = isEnabled,
                                                    isPinned = isPinned,
                                                    logo = iconUrl,
                                                    hashVerified = pluginMap["fileHash"] != null || pluginMap["hashVerified"] == true,
                                                    androidCompatible = true,
                                                    repoBaseUrl = source.repoBaseUrl,
                                                    repoName = source.name
                                                )

                                                if (scrapersList.none { existing -> existing.id == pluginScraper.id }) {
                                                    scrapersList.add(pluginScraper)
                                                }
                                            }
                                        }
                                        manifestParsed = true
                                    }
                                } catch (e: Exception) {
                                    Log.e("NuvioRepo", "Error parsing CS repo manifest: ${e.message}")
                                }
                            }

                            // Array Manifest Fallback
                            if (!manifestParsed && bodyStr.trim().startsWith("[")) {
                                try {
                                    val rawArray = listAdapter.fromJson(bodyStr) as? List<Map<String, Any?>>
                                    rawArray?.forEach { pluginMap ->
                                        val pName = pluginMap["name"] as? String
                                            ?: pluginMap["internalName"] as? String
                                            ?: pluginMap["pluginName"] as? String
                                        val pUrl = pluginMap["url"] as? String ?: pluginMap["file"] as? String ?: ""
                                        if (!pName.isNullOrEmpty()) {
                                            val pId = "cnc_${source.name.lowercase().replace(" ", "_")}_${pName.lowercase().replace(" ", "_")}"
                                            val isEnabled = !disabledPluginIds.contains(pId)
                                            val isPinned = pinnedPluginIds.contains(pId)

                                            val pluginScraper = NuvioScraper(
                                                id = pId,
                                                name = pName,
                                                author = source.name,
                                                description = "CNCVerse OTT Extension Provider",
                                                version = "Current v155",
                                                languages = listOf("HI", "EN"),
                                                supportedTypes = listOf("Movie", "TvSeries"),
                                                filename = if (pUrl.isNotEmpty()) pUrl else "CNC_EXTENSION.js",
                                                enabled = isEnabled,
                                                isPinned = isPinned,
                                                hashVerified = true,
                                                androidCompatible = true,
                                                repoBaseUrl = source.repoBaseUrl,
                                                repoName = source.name
                                            )
                                            if (scrapersList.none { existing -> existing.id == pluginScraper.id }) {
                                                scrapersList.add(pluginScraper)
                                            }
                                        }
                                    }
                                    manifestParsed = true
                                } catch (e: Exception) {
                                    Log.e("NuvioRepo", "Error parsing array manifest: ${e.message}")
                                }
                            } else if (!manifestParsed && scrapersList.none { it.repoName == source.name }) {
                                // Fallback
                                val isStremio = bodyStr.contains("\"resources\"") || bodyStr.contains("\"catalogs\"") || bodyStr.contains("\"types\"") || source.manifestUrl.contains("stremio") || source.manifestUrl.endsWith("manifest.json")
                                val cleanBaseUrl = if (source.repoBaseUrl.endsWith("manifest.json")) {
                                    source.repoBaseUrl.substringBefore("manifest.json")
                                } else if (source.repoBaseUrl.endsWith("/")) {
                                    source.repoBaseUrl
                                } else {
                                    "${source.repoBaseUrl}/"
                                }

                                var hasStreamCap = false
                                var hasSubCap = false
                                var hasCatalogCap = false
                                if (isStremio && bodyStr.trim().startsWith("{")) {
                                    try {
                                        val manifestObj = org.json.JSONObject(bodyStr.trim())
                                        val resArr = manifestObj.optJSONArray("resources")
                                        if (resArr != null) {
                                            for (rIdx in 0 until resArr.length()) {
                                                val rItem = resArr.opt(rIdx)
                                                val rStr = if (rItem is org.json.JSONObject) rItem.optString("name") else rItem.toString()
                                                if (rStr.equals("stream", ignoreCase = true)) hasStreamCap = true
                                                if (rStr.equals("subtitles", ignoreCase = true)) hasSubCap = true
                                                if (rStr.equals("catalog", ignoreCase = true) || rStr.equals("meta", ignoreCase = true)) hasCatalogCap = true
                                            }
                                        } else {
                                            if (manifestObj.has("stream")) hasStreamCap = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e("NuvioRepo", "Error parsing Stremio addon capabilities: ${e.message}")
                                    }
                                }
                                if (source.name.contains("Torrentio", ignoreCase = true) || source.name.contains("Tengu", ignoreCase = true) || source.name.contains("KnightCrawler", ignoreCase = true) || source.name.contains("Streamed", ignoreCase = true)) {
                                    hasStreamCap = true
                                }

                                val addonFilename = when {
                                    !isStremio -> "PLUGIN_STREAM_HTTP"
                                    hasStreamCap -> "STREMIO_HTTP_ADDON"
                                    hasSubCap -> "STREMIO_SUBTITLE_ADDON"
                                    else -> "STREMIO_METADATA_ADDON"
                                }

                                val pId = if (isStremio) "stremio_${source.name.lowercase().replace(" ", "_")}" else "plugin_${source.name.lowercase().replace(" ", "_")}"
                                val isEnabled = !disabledPluginIds.contains(pId)
                                val isPinned = pinnedPluginIds.contains(pId)

                                val addonScraper = NuvioScraper(
                                    id = pId,
                                    name = source.name,
                                    author = "Official Addon",
                                    description = if (isStremio) (if (hasStreamCap) "Stremio Stream Addon" else if (hasSubCap) "Stremio Subtitles Addon" else "Stremio Metadata Addon") else "OTT Stream Plugin",
                                    version = "Current v1",
                                    languages = listOf("EN"),
                                    filename = addonFilename,
                                    enabled = isEnabled,
                                    isPinned = isPinned,
                                    hashVerified = true,
                                    androidCompatible = true,
                                    repoBaseUrl = cleanBaseUrl,
                                    repoName = source.name
                                )
                                scrapersList.add(addonScraper)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NuvioRepo", "Error loading manifest for ${source.name}: ${e.message}")
            }
        }

        Log.d("NuvioRepo", "Loaded total ${scrapersList.size} Nuvio scrapers across ${sourcesToFetch.size} repositories")
        scrapersList
    }

    suspend fun getAvailableScrapers(activeProviders: List<ProviderItem>? = null): List<NuvioScraper> {
        val all = getAllPlugins(activeProviders)
        return all.filter { it.enabled }
    }

    suspend fun fetchScriptCode(scraper: NuvioScraper): String? = withContext(Dispatchers.IO) {
        val baseUrl = scraper.repoBaseUrl ?: "https://raw.githubusercontent.com/yoruix/nuvio-providers/main/"
        val scriptUrl = if (baseUrl.endsWith("/") && scraper.filename.startsWith("/")) {
            baseUrl + scraper.filename.substring(1)
        } else if (!baseUrl.endsWith("/") && !scraper.filename.startsWith("/")) {
            "$baseUrl/${scraper.filename}"
        } else {
            baseUrl + scraper.filename
        }

        try {
            val req = Request.Builder().url(scriptUrl).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    return@withContext resp.body?.string()
                }
            }
        } catch (e: Exception) {
            Log.e("NuvioRepo", "Error fetching script for ${scraper.name}: ${e.message}")
        }
        null
    }
}
