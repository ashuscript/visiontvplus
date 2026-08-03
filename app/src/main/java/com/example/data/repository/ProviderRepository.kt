package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ProviderCategory {
    STREMIO_ADDON,
    NUVIO_PLUGIN
}

data class ProviderItem(
    val id: String,
    val name: String,
    val category: ProviderCategory,
    val url: String,
    val repoBaseUrl: String = "",
    val description: String = "",
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val status: String = "Active", // "Active", "Working", "Failed"
    val responseTimeMs: Long? = null,
    val scrapersCount: Int = 1
)

data class ProviderTestResult(
    val isSuccess: Boolean,
    val responseTimeMs: Long,
    val detectedName: String? = null,
    val scrapersCount: Int = 0,
    val detectedCategory: ProviderCategory? = null,
    val errorMessage: String? = null
)

class ProviderRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("nuvio_providers_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val defaultProviders = listOf(
        ProviderItem(
            id = "stremio_tenguplay",
            name = "TenguPlay Addon",
            category = ProviderCategory.STREMIO_ADDON,
            url = "https://tenguplay.vercel.app/manifest.json",
            repoBaseUrl = "https://tenguplay.vercel.app/",
            description = "Built-in Stremio Addon: Multi-Source MP4/DASH/HLS streams, MovieBox dubs & live subtitles",
            isCustom = false
        ),
        ProviderItem(
            id = "stremio_opensubtitlesv3",
            name = "OpenSubtitles v3",
            category = ProviderCategory.STREMIO_ADDON,
            url = "https://opensubtitles-v3.strem.io/manifest.json",
            repoBaseUrl = "https://opensubtitles-v3.strem.io/",
            description = "Official OpenSubtitles Addon: Global subtitle tracks in 60+ languages for Movies & TV",
            isCustom = false
        ),
        ProviderItem(
            id = "stremio_animekitsu_subs",
            name = "AnimeKitsu Subtitles",
            category = ProviderCategory.STREMIO_ADDON,
            url = "https://animekitsu.strem.fun/manifest.json",
            repoBaseUrl = "https://animekitsu.strem.fun/",
            description = "AnimeKitsu Subtitles Addon: Dedicated subtitles and metadata for Anime shows & movies",
            isCustom = false
        )
    )

    private val _providers = MutableStateFlow<List<ProviderItem>>(emptyList())
    val providers: StateFlow<List<ProviderItem>> = _providers.asStateFlow()

    init {
        loadProviders()
    }

    fun loadProviders() {
        val customJson = prefs.getString("custom_providers_json", null)
        val customList = mutableListOf<ProviderItem>()

        if (!customJson.isNullOrEmpty()) {
            try {
                val array = JSONArray(customJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val catName = obj.getString("category")
                    val catEnum = if (catName == "CLOUDSTREAM_REPO") ProviderCategory.NUVIO_PLUGIN else try { ProviderCategory.valueOf(catName) } catch (e: Exception) { ProviderCategory.STREMIO_ADDON }
                    val item = ProviderItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        category = catEnum,
                        url = obj.getString("url"),
                        repoBaseUrl = obj.optString("repoBaseUrl", obj.getString("url")),
                        description = obj.optString("description", "User Installed Custom Provider"),
                        isCustom = true,
                        isEnabled = obj.optBoolean("isEnabled", true),
                        status = obj.optString("status", "Active"),
                        responseTimeMs = if (obj.has("responseTimeMs")) obj.getLong("responseTimeMs") else null,
                        scrapersCount = obj.optInt("scrapersCount", 1)
                    )
                    customList.add(item)
                }
            } catch (e: Exception) {
                Log.e("ProviderRepository", "Error parsing custom providers: ${e.message}")
            }
        }

        val removedIds = prefs.getStringSet("removed_provider_ids", emptySet()) ?: emptySet()
        val disabledIds = prefs.getStringSet("disabled_provider_ids", emptySet()) ?: emptySet()

        val activeDefaults = defaultProviders
            .filter { !removedIds.contains(it.id) }
            .map { p -> p.copy(isEnabled = !disabledIds.contains(p.id)) }

        _providers.value = activeDefaults + customList
    }

    private fun saveCustomProviders(customItems: List<ProviderItem>) {
        try {
            val array = JSONArray()
            customItems.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("category", item.category.name)
                obj.put("url", item.url)
                obj.put("repoBaseUrl", item.repoBaseUrl)
                obj.put("description", item.description)
                obj.put("isEnabled", item.isEnabled)
                obj.put("status", item.status)
                item.responseTimeMs?.let { obj.put("responseTimeMs", it) }
                obj.put("scrapersCount", item.scrapersCount)
                array.put(obj)
            }
            prefs.edit().putString("custom_providers_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("ProviderRepository", "Error saving custom providers: ${e.message}")
        }
    }

    fun cleanUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.startsWith("stremio://")) {
            url = "https://" + url.removePrefix("stremio://")
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        if (url.endsWith("/configure") || url.endsWith("/configure/")) {
            url = url.substringBefore("/configure") + "/manifest.json"
        }
        return url
    }

    suspend fun testProvider(url: String, category: ProviderCategory? = null): ProviderTestResult = withContext(Dispatchers.IO) {
        val targetUrl = cleanUrl(url)
        val startTime = System.currentTimeMillis()
        
        val urlsToTry = mutableListOf(targetUrl)
        if (!targetUrl.endsWith(".json") && !targetUrl.contains("manifest")) {
            urlsToTry.add(if (targetUrl.endsWith("/")) "${targetUrl}manifest.json" else "$targetUrl/manifest.json")
        }

        var lastError: String? = null
        for (tryUrl in urlsToTry) {
            try {
                val req = Request.Builder()
                    .url(tryUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()

                client.newCall(req).execute().use { resp ->
                    val elapsed = System.currentTimeMillis() - startTime
                    if (!resp.isSuccessful) {
                        lastError = "HTTP Error ${resp.code}: ${resp.message}"
                        return@use
                    }

                    val bodyStr = resp.body?.string()
                    if (bodyStr.isNullOrEmpty()) {
                        lastError = "Empty response received from provider URL"
                        return@use
                    }

                    var detectedName: String? = null
                    var count = 0
                    var detectedCat = category ?: ProviderCategory.STREMIO_ADDON

                    val trimmed = bodyStr.trim()
                    if (trimmed.startsWith("{")) {
                        val jsonObj = JSONObject(trimmed)
                        if (jsonObj.has("id") && jsonObj.has("name") && (jsonObj.has("resources") || jsonObj.has("catalogs") || jsonObj.has("types"))) {
                            // Stremio Addon Manifest
                            detectedCat = ProviderCategory.STREMIO_ADDON
                            detectedName = jsonObj.optString("name", "Stremio Addon")
                            val catCount = jsonObj.optJSONArray("catalogs")?.length() ?: 0
                            val resCount = jsonObj.optJSONArray("resources")?.length() ?: 1
                            count = maxOf(catCount, resCount)
                        } else if (jsonObj.has("scrapers") || jsonObj.has("name")) {
                            // Nuvio Scraper Manifest
                            detectedCat = ProviderCategory.NUVIO_PLUGIN
                            detectedName = jsonObj.optString("name", "Nuvio Plugin")
                            count = jsonObj.optJSONArray("scrapers")?.length() ?: 1
                        } else {
                            detectedName = jsonObj.optString("name", "Custom Stream Provider")
                            count = 1
                        }
                    } else if (trimmed.startsWith("[")) {
                        detectedCat = ProviderCategory.NUVIO_PLUGIN
                        val jsonArr = JSONArray(trimmed)
                        count = jsonArr.length()
                        detectedName = "Nuvio Scraper Repo ($count plugins)"
                    } else {
                        lastError = "Invalid Manifest format. Response is not valid JSON."
                        return@use
                    }

                    return@withContext ProviderTestResult(
                        isSuccess = true,
                        responseTimeMs = elapsed,
                        detectedName = detectedName,
                        scrapersCount = count,
                        detectedCategory = detectedCat
                    )
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Connection failed"
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        return@withContext ProviderTestResult(
            isSuccess = false,
            responseTimeMs = elapsed,
            errorMessage = lastError ?: "Connection failed"
        )
    }

    suspend fun addProvider(
        url: String,
        category: ProviderCategory,
        customName: String? = null
    ): ProviderTestResult {
        val cleaned = cleanUrl(url)
        val testRes = testProvider(cleaned, category)
        if (testRes.isSuccess) {
            val repoBaseUrl = if (cleaned.contains("/")) cleaned.substringBeforeLast("/") + "/" else cleaned
            val providerName = customName?.takeIf { it.isNotBlank() } ?: testRes.detectedName ?: "Custom Provider"
            val finalCat = testRes.detectedCategory ?: category
            val newId = "custom_${finalCat.name.lowercase()}_${System.currentTimeMillis()}"

            val newItem = ProviderItem(
                id = newId,
                name = providerName,
                category = finalCat,
                url = cleaned,
                repoBaseUrl = repoBaseUrl,
                description = "Custom installed ${finalCat.name.replace("_", " ")} source",
                isCustom = true,
                isEnabled = true,
                status = "Working",
                responseTimeMs = testRes.responseTimeMs,
                scrapersCount = testRes.scrapersCount
            )

            val currentCustoms = _providers.value.filter { it.isCustom }.toMutableList()
            currentCustoms.add(newItem)
            saveCustomProviders(currentCustoms)
            loadProviders()
        }
        return testRes
    }

    fun removeProvider(id: String) {
        val item = _providers.value.find { it.id == id }
        if (item != null) {
            if (item.isCustom) {
                val currentCustoms = _providers.value.filter { it.isCustom && it.id != id }
                saveCustomProviders(currentCustoms)
            } else {
                val removedSet = (prefs.getStringSet("removed_provider_ids", emptySet()) ?: emptySet()).toMutableSet()
                removedSet.add(id)
                prefs.edit().putStringSet("removed_provider_ids", removedSet).apply()
            }
            loadProviders()
        }
    }

    fun toggleProvider(id: String, enabled: Boolean) {
        val item = _providers.value.find { it.id == id } ?: return
        if (item.isCustom) {
            val customs = _providers.value.filter { it.isCustom }.map {
                if (it.id == id) it.copy(isEnabled = enabled) else it
            }
            saveCustomProviders(customs)
        } else {
            val disabledSet = (prefs.getStringSet("disabled_provider_ids", emptySet()) ?: emptySet()).toMutableSet()
            if (enabled) {
                disabledSet.remove(id)
            } else {
                disabledSet.add(id)
            }
            prefs.edit().putStringSet("disabled_provider_ids", disabledSet).apply()
        }
        loadProviders()
    }

    suspend fun updateProviderStatus(id: String, status: String, responseTimeMs: Long?) {
        withContext(Dispatchers.Main) {
            _providers.value = _providers.value.map {
                if (it.id == id) it.copy(status = status, responseTimeMs = responseTimeMs) else it
            }
        }
    }
}

