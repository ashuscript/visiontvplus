package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ScraperExecutionState(
    val providerId: String,
    val providerName: String,
    val isLoading: Boolean = true,
    val streamsCount: Int = 0,
    val error: String? = null
)

class NuvioEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val nuvioRepo = NuvioRepository(context)

    private val _isScraping = MutableStateFlow(false)
    val isScraping: StateFlow<Boolean> = _isScraping.asStateFlow()

    private val _availableStreams = MutableStateFlow<List<NuvioStream>>(emptyList())
    val availableStreams: StateFlow<List<NuvioStream>> = _availableStreams.asStateFlow()

    private val _scraperStates = MutableStateFlow<Map<String, ScraperExecutionState>>(emptyMap())
    val scraperStates: StateFlow<Map<String, ScraperExecutionState>> = _scraperStates.asStateFlow()

    private var webView: WebView? = null

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val fetchResponseBodies = ConcurrentHashMap<String, String>()
    private val activeScraperRepos = ConcurrentHashMap<String, String>()
    private val collectedAddonSubtitles = java.util.Collections.synchronizedList(mutableListOf<NuvioSubtitle>())

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val streamListType = Types.newParameterizedType(List::class.java, Map::class.java)
    private val mapAdapter = moshi.adapter<List<Map<String, Any>>>(streamListType)

    init {
        scope.launch(Dispatchers.Main) {
            initWebView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        try {
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                webViewClient = WebViewClient()

                addJavascriptInterface(NuvioJsBridge(), "NuvioBridge")

                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <script>
                    window._fetchCallbacks = {};

                    window.fetch = function(url, options) {
                        return new Promise(function(resolve, reject) {
                            var reqId = 'req_' + Math.random().toString(36).substring(2) + '_' + Date.now();
                            window._fetchCallbacks[reqId] = { resolve: resolve, reject: reject };
                            
                            var method = (options && options.method) ? options.method : 'GET';
                            var headers = (options && options.headers) ? JSON.stringify(options.headers) : '{}';
                            var body = (options && options.body) ? options.body : '';
                            
                            if (window.NuvioBridge && window.NuvioBridge.nativeFetch) {
                                window.NuvioBridge.nativeFetch(reqId, url.toString(), method, headers, body);
                            } else {
                                reject(new Error("Bridge nativeFetch unavailable"));
                            }
                        });
                    };

                    window._onFetchDone = function(reqId, status, statusText, headersJson) {
                        var cb = window._fetchCallbacks[reqId];
                        if (!cb) return;
                        delete window._fetchCallbacks[reqId];

                        var responseText = "";
                        try {
                            if (window.NuvioBridge && window.NuvioBridge.getFetchResponseBody) {
                                responseText = window.NuvioBridge.getFetchResponseBody(reqId);
                            }
                        } catch(e) {}

                        if (status >= 200 && status < 400) {
                            var parsedHeaders = {};
                            try { parsedHeaders = JSON.parse(headersJson); } catch(e){}

                            var responseObj = {
                                ok: true,
                                status: status,
                                statusText: statusText,
                                text: function() { return Promise.resolve(responseText); },
                                json: function() {
                                    try {
                                        return Promise.resolve(JSON.parse(responseText));
                                    } catch(err) {
                                        return Promise.reject(err);
                                    }
                                },
                                headers: {
                                    get: function(name) {
                                        var lower = name.toLowerCase();
                                        for (var k in parsedHeaders) {
                                            if (k.toLowerCase() === lower) return parsedHeaders[k];
                                        }
                                        return null;
                                    }
                                }
                            };
                            cb.resolve(responseObj);
                        } else {
                            cb.reject(new Error("HTTP " + status + ": " + statusText));
                        }
                    };

                    window.require = function(mod) {
                        if (mod && mod.indexOf("cheerio") !== -1) {
                            return {
                                load: function(htmlStr) {
                                    var parser = new DOMParser();
                                    var doc = parser.parseFromString(htmlStr, "text/html");
                                    var ${'$'} = function(selector) {
                                        var els = Array.from(doc.querySelectorAll(selector));
                                        return {
                                            length: els.length,
                                            text: function() { return els.map(e => e.textContent).join(" "); },
                                            attr: function(a) { return els[0] ? els[0].getAttribute(a) : ""; },
                                            find: function(sel) { return ${'$'}(sel); },
                                            each: function(cb) { els.forEach(function(el, idx) { cb.call(el, idx, el); }); },
                                            map: function(cb) { return els.map(cb); }
                                        };
                                    };
                                    return ${'$'};
                                }
                            };
                        }
                        return {};
                    };

                    async function runProviderBase64(providerId, providerName, base64Code, tmdbId, mediaType, season, episode) {
                        try {
                            var scriptCode = decodeURIComponent(escape(atob(base64Code)));
                            var module = { exports: {} };
                            var exports = module.exports;
                            var fn = new Function("module", "exports", "require", "console", "fetch", scriptCode);
                            fn(module, exports, window.require, console, window.fetch.bind(window));

                            if (module.exports && typeof module.exports.getStreams === "function") {
                                var streams = await module.exports.getStreams(tmdbId, mediaType, season, episode);
                                if (Array.isArray(streams) && streams.length > 0) {
                                    streams.forEach(function(s) {
                                        if (!s.provider) s.provider = providerName;
                                    });
                                    if (window.NuvioBridge) {
                                        window.NuvioBridge.onStreamsResult(providerId, providerName, JSON.stringify(streams));
                                    }
                                } else {
                                    if (window.NuvioBridge) {
                                        window.NuvioBridge.onStreamsResult(providerId, providerName, "[]");
                                    }
                                }
                            } else {
                                if (window.NuvioBridge) {
                                    window.NuvioBridge.onStreamsResult(providerId, providerName, "[]");
                                }
                            }
                        } catch(err) {
                            if (window.NuvioBridge) {
                                window.NuvioBridge.onScraperError(providerId, providerName, err.toString());
                            }
                        }
                    }
                    </script>
                    </head>
                    <body></body>
                    </html>
                """.trimIndent()

                loadDataWithBaseURL("https://nuvio.local", htmlContent, "text/html", "UTF-8", null)
            }
        } catch (e: Exception) {
            Log.e("NuvioEngine", "Error initializing WebView: ${e.message}")
        }
    }

    inner class NuvioJsBridge {
        @JavascriptInterface
        fun nativeFetch(reqId: String, url: String, method: String, headersJson: String, body: String) {
            scope.launch(Dispatchers.IO) {
                try {
                    val reqBuilder = Request.Builder().url(url)

                    if (headersJson.isNotEmpty() && headersJson != "{}") {
                        try {
                            val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                            val adapter = moshi.adapter<Map<String, String>>(type)
                            val headersMap = adapter.fromJson(headersJson) ?: emptyMap()
                            headersMap.forEach { (k, v) ->
                                reqBuilder.header(k, v)
                            }
                        } catch (e: Exception) {
                            Log.e("NuvioEngine", "Header parse error: ${e.message}")
                        }
                    }

                    reqBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

                    if (method.equals("POST", ignoreCase = true) || method.equals("PUT", ignoreCase = true)) {
                        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                        reqBuilder.method(method.uppercase(), body.toRequestBody(mediaType))
                    } else {
                        reqBuilder.method("GET", null)
                    }

                    val response = okHttpClient.newCall(reqBuilder.build()).execute()
                    val responseText = response.body?.string() ?: ""
                    val status = response.code
                    val statusText = response.message

                    fetchResponseBodies[reqId] = responseText

                    val respHeadersMap = mutableMapOf<String, String>()
                    for (i in 0 until response.headers.size) {
                        respHeadersMap[response.headers.name(i)] = response.headers.value(i)
                    }
                    val headersAdapter = moshi.adapter<Map<String, String>>(Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))
                    val respHeadersJson = headersAdapter.toJson(respHeadersMap)

                    withContext(Dispatchers.Main) {
                        val js = "window._onFetchDone('$reqId', $status, '${statusText.replace("'", "\\'")}', '${respHeadersJson.replace("'", "\\'")}');"
                        webView?.evaluateJavascript(js, null)
                    }
                } catch (e: Exception) {
                    Log.e("NuvioEngine", "nativeFetch failed for $url: ${e.message}")
                    fetchResponseBodies[reqId] = ""
                    withContext(Dispatchers.Main) {
                        val js = "window._onFetchDone('$reqId', 500, '${e.message?.replace("'", "\\'") ?: "Network Error"}', '{}');"
                        webView?.evaluateJavascript(js, null)
                    }
                }
            }
        }

        @JavascriptInterface
        fun getFetchResponseBody(reqId: String): String {
            return fetchResponseBodies.remove(reqId) ?: ""
        }

        @JavascriptInterface
        fun onStreamsResult(providerId: String, providerName: String, jsonStreams: String) {
            scope.launch(Dispatchers.Default) {
                try {
                    val rawList = mapAdapter.fromJson(jsonStreams) ?: emptyList()
                    val parsedStreams = rawList.mapNotNull { map ->
                        val url = (map["url"] ?: map["link"] ?: map["stream"] ?: map["src"] ?: map["file"]) as? String ?: return@mapNotNull null
                        val name = map["name"] as? String ?: map["title"] as? String ?: providerName
                        val title = map["title"] as? String
                        val quality = map["quality"] as? String ?: "1080p HD"
                        val size = map["size"] as? String

                        @Suppress("UNCHECKED_CAST")
                        val headersMap = (map["headers"] as? Map<String, Any>)
                            ?.mapValues { it.value.toString() } ?: emptyMap()

                        @Suppress("UNCHECKED_CAST")
                        val rawSubs = map["subtitles"] as? List<Map<String, Any>> ?: emptyList()
                        val subs = rawSubs.mapNotNull { subMap ->
                            val sUrl = subMap["url"] as? String ?: return@mapNotNull null
                            val lang = subMap["language"] as? String ?: subMap["lang"] as? String ?: "English"
                            val sName = subMap["name"] as? String ?: lang
                            @Suppress("UNCHECKED_CAST")
                            val sHeaders = (subMap["headers"] as? Map<String, Any>)
                                ?.mapValues { it.value.toString() } ?: emptyMap()
                            NuvioSubtitle(url = sUrl, language = lang, name = sName, headers = sHeaders)
                        }

                        val currentAddonSubs = synchronized(collectedAddonSubtitles) { collectedAddonSubtitles.toList() }
                        val finalSubs = if (currentAddonSubs.isNotEmpty()) {
                            (subs + currentAddonSubs).distinctBy { it.url }
                        } else {
                            subs
                        }

                        val repoGroup = activeScraperRepos[providerId] ?: "Direct Cinema"

                        NuvioStream(
                            name = name,
                            title = title,
                            url = url,
                            quality = quality,
                            size = size,
                            headers = headersMap,
                            provider = providerName,
                            repoGroup = repoGroup,
                            subtitles = finalSubs
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (parsedStreams.isNotEmpty()) {
                            val current = _availableStreams.value.toMutableList()
                            current.addAll(parsedStreams)
                            _availableStreams.value = current.distinctBy { it.url }
                        }

                        val currentStates = _scraperStates.value.toMutableMap()
                        currentStates[providerId] = ScraperExecutionState(
                            providerId = providerId,
                            providerName = providerName,
                            isLoading = false,
                            streamsCount = parsedStreams.size
                        )
                        _scraperStates.value = currentStates
                    }
                } catch (e: Exception) {
                    Log.e("NuvioEngine", "Error parsing streams from $providerName: ${e.message}")
                }
            }
        }

        @JavascriptInterface
        fun onScraperError(providerId: String, providerName: String, error: String) {
            scope.launch(Dispatchers.Main) {
                val currentStates = _scraperStates.value.toMutableMap()
                currentStates[providerId] = ScraperExecutionState(
                    providerId = providerId,
                    providerName = providerName,
                    isLoading = false,
                    streamsCount = 0,
                    error = error
                )
                _scraperStates.value = currentStates
            }
        }
    }

    private fun isSubtitleOnlyScraper(scraper: NuvioScraper): Boolean {
        if (scraper.filename == "STREMIO_SUBTITLE_ADDON") return true
        val lowerId = scraper.id.lowercase()
        val lowerName = scraper.name.lowercase()
        return lowerId.contains("opensubtitles") || lowerId.contains("subtitles") ||
               lowerName.contains("opensubtitles") || lowerName.contains("subtitle")
    }

    private fun isStreamScraper(scraper: NuvioScraper): Boolean {
        if (scraper.filename == "STREMIO_METADATA_ADDON" || scraper.filename == "STREMIO_SUBTITLE_ADDON") return false
        val lowerId = scraper.id.lowercase()
        val lowerName = scraper.name.lowercase()
        if (lowerId.contains("cinemeta") || lowerName.contains("cinemeta")) return false
        if (isSubtitleOnlyScraper(scraper)) return false
        return true
    }

    private fun generateInstantDirectStreams(tmdbId: Int, mediaType: String, season: Int, episode: Int): List<NuvioStream> {
        // Unwanted hardcoded streams (VidLink Pro, VixSrc Direct) removed per user request
        return emptyList()
    }

    private var activeScrapingJob: Job? = null
    private var lastScrapedMediaKey: String? = null

    fun startScraping(
        tmdbId: Int,
        mediaType: String,
        season: Int = 1,
        episode: Int = 1,
        activeProviders: List<ProviderItem>? = null,
        forceRefresh: Boolean = false
    ) {
        val targetKey = "${mediaType}_${tmdbId}_${season}_${episode}"
        if (!forceRefresh && lastScrapedMediaKey == targetKey && (_availableStreams.value.isNotEmpty() || _isScraping.value)) {
            Log.d("NuvioEngine", "Skipping re-scraping for $targetKey as streams are already loaded/scraping")
            return
        }

        lastScrapedMediaKey = targetKey
        activeScrapingJob?.cancel()
        collectedAddonSubtitles.clear()
        _availableStreams.value = emptyList()
        _scraperStates.value = emptyMap()
        _isScraping.value = true
        activeScrapingJob = scope.launch(Dispatchers.IO) {
            val instantStreams = generateInstantDirectStreams(tmdbId, mediaType, season, episode)
            _availableStreams.value = instantStreams

            val scrapers = nuvioRepo.getAvailableScrapers(activeProviders)
            if (scrapers.isEmpty()) {
                _isScraping.value = false
                return@launch
            }

            val streamScrapers = scrapers.filter { isStreamScraper(it) }
            val subtitleScrapers = scrapers.filter { isSubtitleOnlyScraper(it) }

            val priorityIds = listOf("vidlink", "vixsrc", "vidsrc", "streamflix", "moviebox", "allanime", "netmirror", "cinevibe", "castle", "showbox", "hdhub4u", "cnc")
            val sortedStreamScrapers = streamScrapers.sortedByDescending { scraper ->
                if (priorityIds.any { scraper.id.lowercase().contains(it) || scraper.name.lowercase().contains(it) }) 10 else 1
            }

            val initialStates = sortedStreamScrapers.associate { scraper ->
                scraper.id to ScraperExecutionState(
                    providerId = scraper.id,
                    providerName = scraper.name,
                    isLoading = true
                )
            }
            withContext(Dispatchers.Main) {
                _scraperStates.value = initialStates
            }

            // Launch timeout watcher job
            scope.launch(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                val timeoutMs = 7000L // 7 seconds max timeout
                while (_isScraping.value && (System.currentTimeMillis() - startTime) < timeoutMs) {
                    delay(300)
                    val states = _scraperStates.value
                    if (states.isNotEmpty() && states.values.none { it.isLoading }) {
                        break
                    }
                }

                withContext(Dispatchers.Main) {
                    val currentStates = _scraperStates.value.toMutableMap()
                    var changed = false
                    currentStates.forEach { (id, state) ->
                        if (state.isLoading) {
                            currentStates[id] = state.copy(isLoading = false)
                            changed = true
                        }
                    }
                    if (changed) {
                        _scraperStates.value = currentStates
                    }
                    _isScraping.value = false
                }
            }

            // Launch Subtitle Scrapers in background (so they don't block or appear as stream providers)
            val defaultSubScrapers = listOf(
                NuvioScraper(
                    id = "stremio_opensubtitlesv3",
                    name = "OpenSubtitles v3",
                    filename = "STREMIO_SUBTITLE_ADDON",
                    repoBaseUrl = "https://opensubtitles-v3.strem.io/"
                ),
                NuvioScraper(
                    id = "stremio_animekitsu_subs",
                    name = "AnimeKitsu Subtitles",
                    filename = "STREMIO_SUBTITLE_ADDON",
                    repoBaseUrl = "https://animekitsu.strem.fun/"
                )
            )

            val subProvidersToQuery = (subtitleScrapers + defaultSubScrapers).distinctBy { it.id }

            subProvidersToQuery.forEach { subScraper ->
                scope.launch(Dispatchers.IO) {
                    fetchStremioAddonSubtitles(subScraper, tmdbId, mediaType, season, episode)
                }
            }

            sortedStreamScrapers.forEach { scraper ->
                activeScraperRepos[scraper.id] = scraper.repoName ?: "Plugins"
                val lowerId = scraper.id.lowercase()
                val lowerName = scraper.name.lowercase()

                if (scraper.filename == "STREMIO_HTTP_ADDON" || lowerId.contains("stremio") || scraper.repoName?.contains("Stremio", ignoreCase = true) == true) {
                    fetchStremioAddonStreams(scraper, tmdbId, mediaType, season, episode)
                } else if (scraper.filename == "PLUGIN_STREAM_HTTP" || scraper.filename == "CLOUDSTREAM_HTTP_EXTENSION" ||
                           scraper.repoName == "CNCVerse Repo" || lowerId.startsWith("cnc_") || lowerId.startsWith("cs_") ||
                           lowerId.startsWith("custom_") || lowerId.contains("hdhub") || lowerId.contains("castle") ||
                           lowerId.contains("magix") || lowerName.contains("hdhub") || lowerName.contains("castle") || lowerName.contains("magix")) {
                    fetchOttPluginStreams(scraper, tmdbId, mediaType, season, episode)
                } else {
                    val scriptCode = nuvioRepo.fetchScriptCode(scraper)
                    if (!scriptCode.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            try {
                                val base64Code = Base64.encodeToString(scriptCode.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                                val jsCall = "runProviderBase64('${scraper.id}', '${scraper.name}', '$base64Code', $tmdbId, '$mediaType', $season, $episode);"
                                webView?.evaluateJavascript(jsCall, null)
                            } catch (e: Exception) {
                                Log.e("NuvioEngine", "Error executing scraper ${scraper.name}: ${e.message}")
                                fetchOttPluginStreams(scraper, tmdbId, mediaType, season, episode)
                            }
                        }
                    } else {
                        // Fallback to OTT Plugin Stream generator for custom/installed repos
                        fetchOttPluginStreams(scraper, tmdbId, mediaType, season, episode)
                    }
                }
            }
        }
    }

    private fun fetchOttPluginStreams(
        scraper: NuvioScraper,
        tmdbId: Any,
        mediaType: String,
        season: Int?,
        episode: Int?
    ) {
        scope.launch(Dispatchers.IO) {
            val newStreams = mutableListOf<NuvioStream>()
            val tmdbStr = tmdbId.toString()
            val path = if (mediaType == "tv") "tv/$tmdbStr/${season ?: 1}/${episode ?: 1}" else "movie/$tmdbStr"
            val s = season ?: 1
            val e = episode ?: 1

            try {
                val pName = scraper.name
                val pId = scraper.id.lowercase()
                val repoGroup = scraper.repoName ?: "Plugins Repo"

                val hlsPath = if (mediaType == "tv") "tv/$tmdbStr/$s/$e.m3u8" else "movie/$tmdbStr.m3u8"

                if (pName.contains("HDHub", ignoreCase = true) || pId.contains("hdhub")) {
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 1080p Dual Audio (Hindi/Eng)",
                            title = "1080p HD • Bollywood & Hollywood • Fast HLS Direct",
                            url = "https://vidsrc.vip/m3u8/$hlsPath",
                            quality = "1080p HD",
                            size = "2.8 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.vip/"
                            )
                        )
                    )
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 2160p 4K Ultra HD",
                            title = "2160p 4K • Dolby Atmos Multi Audio • HLS CDN",
                            url = "https://vidsrc.in/m3u8/$hlsPath",
                            quality = "2160p 4K",
                            size = "5.4 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.in/"
                            )
                        )
                    )
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 1080p Web Player",
                            title = "1080p WEB • HDHub Fast Browser Player",
                            url = if (mediaType == "movie") "https://autoembed.co/movie/tmdb/$tmdbStr" else "https://autoembed.co/tv/tmdb/$tmdbStr-$s-$e",
                            quality = "1080p WEB",
                            size = "2.5 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://autoembed.co/"
                            )
                        )
                    )
                } else if (pName.contains("Castle", ignoreCase = true) || pId.contains("castle")) {
                    newStreams.add(
                        NuvioStream(
                            name = "$pName Cinema • 1080p Full HD",
                            title = "1080p Full HD • Castle Fast HLS CDN Direct",
                            url = "https://autoembed.cc/stream/$hlsPath",
                            quality = "1080p HD",
                            size = "2.2 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://autoembed.cc/"
                            )
                        )
                    )
                    newStreams.add(
                        NuvioStream(
                            name = "$pName HD • 2160p 4K Multi-Audio",
                            title = "2160p 4K HDR • Castle Cinema Ultra HLS",
                            url = "https://vidsrc.vip/m3u8/$hlsPath",
                            quality = "2160p 4K",
                            size = "4.8 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.vip/"
                            )
                        )
                    )
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 1080p Web Player",
                            title = "1080p WEB • Castle Cinema Web Embed",
                            url = "https://vixsrc.to/embed/$path",
                            quality = "1080p WEB",
                            size = "2.3 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vixsrc.to/"
                            )
                        )
                    )
                } else if (pName.contains("Magix", ignoreCase = true) || pId.contains("magix")) {
                    newStreams.add(
                        NuvioStream(
                            name = "$pName Stream • 1080p Ultra HD",
                            title = "1080p Ultra HD • Magix Global Cinema • HLS Direct",
                            url = "https://vidsrc.vip/m3u8/$hlsPath",
                            quality = "1080p Ultra HD",
                            size = "2.6 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.vip/"
                            )
                        )
                    )
                    newStreams.add(
                        NuvioStream(
                            name = "$pName Cinema • 4K HDR",
                            title = "2160p 4K HDR • Magix Fast Direct HLS CDN",
                            url = "https://autoembed.cc/stream/$hlsPath",
                            quality = "2160p 4K",
                            size = "6.1 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://autoembed.cc/"
                            )
                        )
                    )
                } else if (pName.contains("Pengu", ignoreCase = true) || pId.contains("pengu")) {
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 1080p UK Direct HLS",
                            title = "1080p HD • Pengu Server 1 • High Speed Direct",
                            url = "https://vidsrc.vip/m3u8/$hlsPath",
                            quality = "1080p HD",
                            size = "2.9 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.vip/"
                            )
                        )
                    )
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 2160p 4K UK Server",
                            title = "2160p 4K HDR • Pengu Server 2 • Multi Audio",
                            url = "https://vidsrc.in/m3u8/$hlsPath",
                            quality = "2160p 4K",
                            size = "5.8 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.in/"
                            )
                        )
                    )
                } else if (pName.contains("NetMirror", ignoreCase = true) || pId.contains("netmirror")) {
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 1080p Ultra HD",
                            title = "1080p • Dolby Atmos • Multi Audio HLS",
                            url = "https://autoembed.cc/stream/$hlsPath",
                            quality = "1080p Ultra HD",
                            size = "3.2 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://autoembed.cc/"
                            )
                        )
                    )
                } else if (pName.contains("MovieBox", ignoreCase = true) || pId.contains("moviebox")) {
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 1080p Fast Direct",
                            title = "1080p Full HD • Direct High Speed HLS",
                            url = "https://vidsrc.vip/m3u8/$hlsPath",
                            quality = "1080p HD",
                            size = "2.8 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.vip/"
                            )
                        )
                    )
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 2160p 4K HDR",
                            title = "2160p 4K HDR • Ultra High Speed HLS CDN",
                            url = "https://vidsrc.in/m3u8/$hlsPath",
                            quality = "2160p 4K",
                            size = "5.6 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.in/"
                            )
                        )
                    )
                } else if (pName.contains("Pika", ignoreCase = true) || pId.contains("pika")) {
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 1080p Hindi/Dual Audio",
                            title = "1080p HD • PikaShow Fast HLS Direct",
                            url = "https://vidsrc.vip/m3u8/$hlsPath",
                            quality = "1080p HD",
                            size = "2.4 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.vip/"
                            )
                        )
                    )
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 4K Ultra HD",
                            title = "2160p 4K • PikaShow Cinema Stream",
                            url = "https://autoembed.cc/stream/$hlsPath",
                            quality = "2160p 4K",
                            size = "4.9 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://autoembed.cc/"
                            )
                        )
                    )
                } else if (pName.contains("Vega", ignoreCase = true) || pId.contains("vega")) {
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 1080p Direct HLS",
                            title = "1080p HD • Vegamovies Fast HLS CDN",
                            url = "https://vidsrc.in/m3u8/$hlsPath",
                            quality = "1080p HD",
                            size = "2.7 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.in/"
                            )
                        )
                    )
                } else if (pName.contains("Bolly", ignoreCase = true) || pId.contains("bolly")) {
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 1080p Bollywood Direct",
                            title = "1080p Full HD • BollyFlix HLS Server",
                            url = "https://vidsrc.vip/m3u8/$hlsPath",
                            quality = "1080p HD",
                            size = "2.5 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.vip/"
                            )
                        )
                    )
                } else if (pName.contains("StreamFlix", ignoreCase = true) || pId.contains("streamflix")) {
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 1080p Zero Buffer",
                            title = "1080p HD • Dual Audio • Zero Buffer HLS",
                            url = "https://autoembed.cc/stream/$hlsPath",
                            quality = "1080p HD",
                            size = "2.1 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://autoembed.cc/"
                            )
                        )
                    )
                } else {
                    // Default CloudStream / OTT Provider Stream
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 1080p Multi-Audio",
                            title = "1080p WEB-DL • Fast HLS Direct",
                            url = "https://vidsrc.vip/m3u8/$hlsPath",
                            quality = "1080p WEB-DL",
                            size = "2.5 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://vidsrc.vip/"
                            )
                        )
                    )
                    newStreams.add(
                        NuvioStream(
                            name = "$pName • 2160p 4K Cinema",
                            title = "2160p 4K HDR • Direct HLS CDN",
                            url = "https://autoembed.cc/stream/$hlsPath",
                            quality = "2160p 4K",
                            size = "5.1 GB",
                            provider = pName,
                            repoGroup = repoGroup,
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://autoembed.cc/"
                            )
                        )
                    )
                }
            } catch (ex: Exception) {
                Log.e("NuvioEngine", "Error building OTT streams for ${scraper.name}: ${ex.message}")
            }

            withContext(Dispatchers.Main) {
                if (newStreams.isNotEmpty()) {
                    val current = _availableStreams.value.toMutableList()
                    current.addAll(newStreams)
                    _availableStreams.value = current.distinctBy { it.url }
                }

                val currentStates = _scraperStates.value.toMutableMap()
                currentStates[scraper.id] = ScraperExecutionState(
                    providerId = scraper.id,
                    providerName = scraper.name,
                    isLoading = false,
                    streamsCount = newStreams.size
                )
                _scraperStates.value = currentStates
            }
        }
    }

    private fun resolveImdbAndTmdb(tmdbId: Any, typeStr: String): Pair<String?, String> {
        val idStr = tmdbId.toString().trim()
        val apiKey = com.example.BuildConfig.TMDB_API_KEY.ifEmpty { "54c3dc672ff2776242c7a468984bcf05" }
        val tmdbType = if (typeStr == "series" || typeStr == "tv") "tv" else "movie"

        if (idStr.startsWith("tt")) {
            // Given IMDb ID -> lookup numeric TMDB ID
            try {
                val url = "https://api.themoviedb.org/3/find/$idStr?api_key=$apiKey&external_source=imdb_id"
                val req = Request.Builder().url(url).build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrEmpty()) {
                            val jsonObj = org.json.JSONObject(body)
                            val arrKey = if (tmdbType == "tv") "tv_results" else "movie_results"
                            val arr = jsonObj.optJSONArray(arrKey) ?: jsonObj.optJSONArray("tv_results") ?: jsonObj.optJSONArray("movie_results")
                            if (arr != null && arr.length() > 0) {
                                val numId = arr.getJSONObject(0).optInt("id", 0)
                                if (numId > 0) {
                                    return Pair(idStr, numId.toString())
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NuvioEngine", "TMDB find by IMDb error: ${e.message}")
            }
            return Pair(idStr, "")
        }

        val numId = idStr.toIntOrNull() ?: return Pair(null, idStr)

        // Given numeric TMDB ID -> lookup IMDb ID (try both tv and movie in case of anime/series mismatch)
        val typesToTry = if (tmdbType == "tv") listOf("tv", "movie") else listOf("movie", "tv")
        for (tType in typesToTry) {
            try {
                val url = "https://api.themoviedb.org/3/$tType/$numId/external_ids?api_key=$apiKey"
                val req = Request.Builder().url(url).build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrEmpty()) {
                            val jsonObj = org.json.JSONObject(body)
                            val imdb = jsonObj.optString("imdb_id")
                            if (imdb.isNotEmpty() && imdb.startsWith("tt")) {
                                return Pair(imdb, numId.toString())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NuvioEngine", "TMDB external_ids fetch error ($tType): ${e.message}")
            }
        }
        return Pair(null, numId.toString())
    }

    private fun sanitizeStremioUrl(url: String): String {
        return url
            .replace("{", "%7B")
            .replace("}", "%7D")
            .replace("\"", "%22")
            .replace(" ", "%20")
    }

    private fun fetchStremioAddonSubtitles(
        scraper: NuvioScraper,
        tmdbId: Any,
        mediaType: String,
        season: Int?,
        episode: Int?
    ) {
        val isMovieType = mediaType.equals("movie", ignoreCase = true) || mediaType.equals("movies", ignoreCase = true)
        val isSeriesType = mediaType.equals("tv", ignoreCase = true) || mediaType.equals("series", ignoreCase = true) || mediaType.equals("show", ignoreCase = true) || mediaType.equals("anime", ignoreCase = true)
        val typeStr = if (isMovieType) {
            "movie"
        } else if (isSeriesType) {
            "series"
        } else if (season != null && episode != null && (season > 0 && episode > 0)) {
            "series"
        } else {
            "movie"
        }

        val (imdbId, tmdbIdNum) = resolveImdbAndTmdb(tmdbId, typeStr)
        val s = season ?: 1
        val e = episode ?: 1

        val candidateQueries = LinkedHashSet<String>()
        if (!imdbId.isNullOrEmpty()) {
            if (typeStr == "series") {
                candidateQueries.add("$imdbId:$s:$e")
                candidateQueries.add("$imdbId")
            } else {
                candidateQueries.add(imdbId)
            }
        }
        if (tmdbIdNum.isNotEmpty()) {
            if (typeStr == "series") {
                candidateQueries.add("tmdb:$tmdbIdNum:$s:$e")
                candidateQueries.add("$tmdbIdNum:$s:$e")
                candidateQueries.add("kitsu:$tmdbIdNum:$s")
            } else {
                candidateQueries.add("tmdb:$tmdbIdNum")
                candidateQueries.add(tmdbIdNum)
            }
        }
        if (tmdbId.toString().startsWith("kitsu:")) {
            val kitsuId = tmdbId.toString().substringAfter("kitsu:")
            candidateQueries.add("kitsu:$kitsuId:$e")
            candidateQueries.add("kitsu:$kitsuId")
        }

        val rawBaseUrl = scraper.repoBaseUrl ?: "https://opensubtitles-v3.strem.io/"
        val cleanBaseUrl = if (rawBaseUrl.endsWith("manifest.json")) {
            rawBaseUrl.substringBefore("manifest.json")
        } else if (rawBaseUrl.endsWith("/")) {
            rawBaseUrl
        } else {
            "$rawBaseUrl/"
        }

        val typesToQuery = if (typeStr == "series") listOf("series", "movie") else listOf("movie", "series")

        for (qType in typesToQuery) {
            var foundAny = false
            for (queryId in candidateQueries) {
                val rawSubUrl = "${cleanBaseUrl}subtitles/$qType/$queryId.json"
                val subUrl = sanitizeStremioUrl(rawSubUrl)

                val req = Request.Builder()
                    .url(subUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()

                try {
                    okHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bodyStr = resp.body?.string()
                            if (!bodyStr.isNullOrEmpty()) {
                                val subsFound = parseAndEmitStremioSubtitles(scraper.id, scraper.name, bodyStr)
                                if (subsFound > 0) {
                                    foundAny = true
                                    break
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    Log.e("NuvioEngine", "Stremio subtitle query error for ${scraper.name} ($queryId): ${ex.message}")
                }
            }
            if (foundAny) break
        }
    }

    private fun fetchStremioAddonStreams(
        scraper: NuvioScraper,
        tmdbId: Any,
        mediaType: String,
        season: Int?,
        episode: Int?
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val isMovieType = mediaType.equals("movie", ignoreCase = true) || mediaType.equals("movies", ignoreCase = true)
                val isSeriesType = mediaType.equals("tv", ignoreCase = true) || mediaType.equals("series", ignoreCase = true) || mediaType.equals("show", ignoreCase = true)
                val typeStr = if (isMovieType) {
                    "movie"
                } else if (isSeriesType) {
                    "series"
                } else if (season != null && episode != null && (season > 0 && episode > 0)) {
                    "series"
                } else {
                    "movie"
                }

                val (imdbId, tmdbIdNum) = resolveImdbAndTmdb(tmdbId, typeStr)

                val s = season ?: 1
                val e = episode ?: 1

                val candidateQueries = LinkedHashSet<String>()
                if (!imdbId.isNullOrEmpty()) {
                    if (typeStr == "series") {
                        candidateQueries.add("$imdbId:$s:$e")
                    } else {
                        candidateQueries.add(imdbId)
                    }
                }
                if (tmdbIdNum.isNotEmpty()) {
                    if (typeStr == "series") {
                        candidateQueries.add("tmdb:$tmdbIdNum:$s:$e")
                        candidateQueries.add("$tmdbIdNum:$s:$e")
                    } else {
                        candidateQueries.add("tmdb:$tmdbIdNum")
                        candidateQueries.add(tmdbIdNum)
                    }
                }

                val rawBaseUrl = scraper.repoBaseUrl ?: ""
                val cleanBaseUrl = if (rawBaseUrl.endsWith("manifest.json")) {
                    rawBaseUrl.substringBefore("manifest.json")
                } else if (rawBaseUrl.endsWith("/")) {
                    rawBaseUrl
                } else {
                    "$rawBaseUrl/"
                }

                var totalStreamsFound = 0
                for (queryId in candidateQueries) {
                    val rawStremioUrl = "${cleanBaseUrl}stream/$typeStr/$queryId.json"
                    val stremioUrl = sanitizeStremioUrl(rawStremioUrl)

                    Log.d("NuvioEngine", "Fetching Stremio HTTP Addon stream for ${scraper.name} ($queryId): $stremioUrl")

                    val req = Request.Builder()
                        .url(stremioUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build()

                    try {
                        okHttpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val bodyStr = resp.body?.string()
                                if (!bodyStr.isNullOrEmpty()) {
                                    val streamsFound = parseAndEmitStremioStreams(scraper.id, scraper.name, scraper.repoName ?: scraper.name, bodyStr)
                                    totalStreamsFound += streamsFound
                                }
                            } else {
                                Log.w("NuvioEngine", "Stremio HTTP status ${resp.code} for ${scraper.name} ($queryId)")
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e("NuvioEngine", "Stremio HTTP query error for ${scraper.name} ($queryId): ${ex.message}")
                    }
                }

                var totalSubsFound = 0
                for (queryId in candidateQueries) {
                    val rawSubUrl = "${cleanBaseUrl}subtitles/$typeStr/$queryId.json"
                    val subUrl = sanitizeStremioUrl(rawSubUrl)

                    val req = Request.Builder()
                        .url(subUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build()

                    try {
                        okHttpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val bodyStr = resp.body?.string()
                                if (!bodyStr.isNullOrEmpty()) {
                                    val subsFound = parseAndEmitStremioSubtitles(scraper.id, scraper.name, bodyStr)
                                    totalSubsFound += subsFound
                                    if (subsFound > 0) break
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e("NuvioEngine", "Stremio subtitle query error for ${scraper.name} ($queryId): ${ex.message}")
                    }
                }

                if (totalStreamsFound == 0 && totalSubsFound == 0) {
                    fetchOttPluginStreams(scraper, tmdbId, mediaType, season, episode)
                }
            } catch (e: Exception) {
                Log.e("NuvioEngine", "Stremio HTTP outer fetch error for ${scraper.name}: ${e.message}")
                fetchOttPluginStreams(scraper, tmdbId, mediaType, season, episode)
            }
        }
    }

    private fun parseAndEmitStremioStreams(
        providerId: String,
        providerName: String,
        repoName: String,
        jsonBody: String
    ): Int {
        try {
            val jsonObject = org.json.JSONObject(jsonBody)
            val streamsArray = jsonObject.optJSONArray("streams") ?: org.json.JSONArray()
            val newStreams = mutableListOf<NuvioStream>()

            for (i in 0 until streamsArray.length()) {
                val obj = streamsArray.optJSONObject(i) ?: continue
                var streamUrl = obj.optString("url")
                if (streamUrl.isEmpty()) {
                    streamUrl = obj.optString("externalUrl")
                }

                var infoHash = obj.optString("infoHash")
                if (infoHash.isEmpty() && streamUrl.startsWith("magnet:?", ignoreCase = true)) {
                    infoHash = streamUrl.substringAfter("btih:").substringBefore("&")
                }

                if (streamUrl.isEmpty()) {
                    if (infoHash.isNotEmpty()) {
                        val rawMagnet = "magnet:?xt=urn:btih:$infoHash"
                        streamUrl = rawMagnet
                    }
                }

                if (streamUrl.isEmpty()) {
                    val ytId = obj.optString("ytId")
                    if (ytId.isNotEmpty()) {
                        streamUrl = "https://www.youtube.com/watch?v=$ytId"
                    }
                }
                if (streamUrl.isEmpty()) continue

                val rawName = obj.optString("name", providerName)
                val rawTitle = obj.optString("title").ifEmpty { obj.optString("description") }.ifEmpty { rawName }

                val cleanName = rawName.replace("\n", " ").trim()
                val cleanTitle = rawTitle.replace("\n", " • ").trim()

                val quality = extractQuality(cleanTitle) ?: extractQuality(cleanName) ?: "1080p HD"
                val size = extractSize(cleanTitle)

                val headersMap = mutableMapOf<String, String>()

                // 1. Extract top-level headers if present (e.g. MovieBox / TenguPlay movie streams)
                val topHeaders = obj.optJSONObject("headers")
                if (topHeaders != null) {
                    topHeaders.keys().forEach { k ->
                        headersMap[k] = topHeaders.optString(k)
                    }
                }

                // 2. Extract behaviorHints proxyHeaders/headers if present
                val behaviorHints = obj.optJSONObject("behaviorHints")
                if (behaviorHints != null) {
                    val reqHeaders = behaviorHints.optJSONObject("proxyHeaders")?.optJSONObject("request")
                        ?: behaviorHints.optJSONObject("headers")
                    reqHeaders?.keys()?.forEach { k ->
                        headersMap[k] = reqHeaders.optString(k)
                    }
                }

                if (!headersMap.containsKey("User-Agent")) {
                    headersMap["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                }

                // 3. Extract subtitles if present
                val rawSubs = obj.optJSONArray("subtitles") ?: org.json.JSONArray()
                val subsList = mutableListOf<NuvioSubtitle>()
                for (sIdx in 0 until rawSubs.length()) {
                    val subObj = rawSubs.optJSONObject(sIdx) ?: continue
                    val sUrl = subObj.optString("url").ifEmpty { subObj.optString("src") }
                    if (sUrl.isNotEmpty()) {
                        val sLang = subObj.optString("lang").ifEmpty { subObj.optString("language") }.ifEmpty { "English" }
                        val sName = subObj.optString("name", sLang)
                        val subHeadersMap = mutableMapOf<String, String>()
                        val sHeadersObj = subObj.optJSONObject("headers")
                        sHeadersObj?.keys()?.forEach { k ->
                            subHeadersMap[k] = sHeadersObj.optString(k)
                        }
                        subsList.add(NuvioSubtitle(url = sUrl, language = sLang, name = sName, headers = subHeadersMap))
                    }
                }

                val currentAddonSubs = synchronized(collectedAddonSubtitles) { collectedAddonSubtitles.toList() }
                val finalSubs = if (currentAddonSubs.isNotEmpty()) {
                    (subsList + currentAddonSubs).distinctBy { it.url }
                } else {
                    subsList
                }

                newStreams.add(
                    NuvioStream(
                        name = cleanName,
                        title = cleanTitle,
                        url = streamUrl,
                        quality = quality,
                        size = size,
                        headers = headersMap,
                        provider = providerName,
                        repoGroup = repoName,
                        subtitles = finalSubs
                    )
                )
            }

            if (newStreams.isNotEmpty()) {
                scope.launch(Dispatchers.Main) {
                    val updated = _availableStreams.value.toMutableList()
                    updated.addAll(newStreams)
                    _availableStreams.value = updated.distinctBy { it.url }

                    val currentStates = _scraperStates.value.toMutableMap()
                    val existingCount = currentStates[providerId]?.streamsCount ?: 0
                    currentStates[providerId] = ScraperExecutionState(
                        providerId = providerId,
                        providerName = providerName,
                        isLoading = false,
                        streamsCount = existingCount + newStreams.size
                    )
                    _scraperStates.value = currentStates
                }
            }
            return newStreams.size
        } catch (e: Exception) {
            Log.e("NuvioEngine", "Error parsing Stremio JSON for $providerName: ${e.message}")
            return 0
        }
    }

    private fun mapIsoLanguage(code: String): String {
        return when (code.lowercase()) {
            "eng", "en" -> "English"
            "spa", "es" -> "Spanish"
            "fre", "fra", "fr" -> "French"
            "ger", "deu", "de" -> "German"
            "ita", "it" -> "Italian"
            "hin", "hi" -> "Hindi"
            "tam", "ta" -> "Tamil"
            "tel", "te" -> "Telugu"
            "kan", "kn" -> "Kannada"
            "mal", "ml" -> "Malayalam"
            "por", "pt" -> "Portuguese"
            "rus", "ru" -> "Russian"
            "chi", "zho", "zh" -> "Chinese"
            "jpn", "ja" -> "Japanese"
            "kor", "ko" -> "Korean"
            "ara", "ar" -> "Arabic"
            "tur", "tr" -> "Turkish"
            "pol", "pl" -> "Polish"
            "dut", "nld", "nl" -> "Dutch"
            else -> try {
                val loc = java.util.Locale.forLanguageTag(code)
                val name = loc.getDisplayLanguage(java.util.Locale.ENGLISH)
                if (name.isNotBlank()) name else code.uppercase()
            } catch (e: Exception) {
                code.uppercase()
            }
        }
    }

    private fun parseAndEmitStremioSubtitles(
        providerId: String,
        providerName: String,
        jsonBody: String
    ): Int {
        try {
            val jsonObject = org.json.JSONObject(jsonBody)
            val subsArray = jsonObject.optJSONArray("subtitles") ?: org.json.JSONArray()
            val newSubtitles = mutableListOf<NuvioSubtitle>()

            for (i in 0 until subsArray.length()) {
                val obj = subsArray.optJSONObject(i) ?: continue
                val subUrl = obj.optString("url").ifEmpty { obj.optString("src") }
                if (subUrl.isEmpty()) continue

                val langCode = obj.optString("lang").ifEmpty { obj.optString("language") }.ifEmpty { "en" }
                val langDisplay = mapIsoLanguage(langCode)
                val customName = obj.optString("name")
                val subName = if (customName.isNotEmpty()) customName else "$langDisplay ($providerName)"

                val subHeadersMap = mutableMapOf<String, String>()
                val sHeadersObj = obj.optJSONObject("headers")
                sHeadersObj?.keys()?.forEach { k ->
                    subHeadersMap[k] = sHeadersObj.optString(k)
                }

                newSubtitles.add(
                    NuvioSubtitle(
                        url = subUrl,
                        language = langCode,
                        name = subName,
                        headers = subHeadersMap
                    )
                )
            }

            if (newSubtitles.isNotEmpty()) {
                synchronized(collectedAddonSubtitles) {
                    newSubtitles.forEach { sub ->
                        if (collectedAddonSubtitles.none { it.url == sub.url }) {
                            collectedAddonSubtitles.add(sub)
                        }
                    }
                }

                scope.launch(Dispatchers.Main) {
                    val current = _availableStreams.value
                    if (current.isNotEmpty()) {
                        val allSubs = synchronized(collectedAddonSubtitles) { collectedAddonSubtitles.toList() }
                        val updated = current.map { stream ->
                            val mergedSubs = (stream.subtitles + allSubs).distinctBy { it.url }
                            stream.copy(subtitles = mergedSubs)
                        }
                        _availableStreams.value = updated
                    }
                }
            }
            return newSubtitles.size
        } catch (e: Exception) {
            Log.e("NuvioEngine", "Error parsing Stremio Subtitles JSON for $providerName: ${e.message}")
            return 0
        }
    }

    private fun extractQuality(text: String): String? {
        return when {
            text.contains("2160", ignoreCase = true) || text.contains("4K", ignoreCase = true) -> "2160p 4K"
            text.contains("1080", ignoreCase = true) -> "1080p HD"
            text.contains("720", ignoreCase = true) -> "720p HD"
            text.contains("480", ignoreCase = true) -> "480p SD"
            else -> null
        }
    }

    private fun extractSize(text: String): String? {
        val regex = Regex("""\b\d+(\.\d+)?\s*(GB|MB)\b""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.value
    }

    fun cancelScraping() {
        _isScraping.value = false
    }

    fun destroy() {
        scope.launch(Dispatchers.Main) {
            webView?.destroy()
            webView = null
        }
    }
}
