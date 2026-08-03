package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Error
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.math.roundToInt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.AppDatabase
import com.example.data.db.WatchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.CaptionStyleCompat
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.ui.graphics.toArgb
import com.example.data.repository.NuvioStream
import com.example.ui.theme.AppleTvAccent
import com.example.ui.theme.AppleTvTextSecondary
import kotlinx.coroutines.delay

enum class SubtitleBgStyle(val label: String) {
    TRANSPARENT("None (Clean)"),
    SEMI_TRANSPARENT("Semi-Transparent"),
    SOLID_BLACK("Black Box")
}

data class ExtractedTrackItem(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

data class ContinueWatchingItem(
    val title: String,
    val subtitle: String,
    val duration: String,
    val posterUrl: String
)

private fun getDisplayLanguageName(code: String?): String {
    if (code.isNullOrEmpty()) return "Original Language"
    return when (code.lowercase()) {
        "en", "eng" -> "Original: English"
        "hi", "hin" -> "Hindi"
        "fr", "fre", "fra" -> "French"
        "de", "ger", "deu" -> "German"
        "es", "spa" -> "Spanish"
        "it", "ita" -> "Italian"
        "ja", "jpn" -> "Japanese"
        "ta", "tam" -> "Tamil"
        "te", "tel" -> "Telugu"
        "kn", "kan" -> "Kannada"
        "ml", "mal" -> "Malayalam"
        "ko", "kor" -> "Korean"
        "zh", "zho", "chi" -> "Chinese"
        "ru", "rus" -> "Russian"
        "pt", "por" -> "Portuguese"
        else -> try {
            val loc = java.util.Locale.forLanguageTag(code)
            val name = loc.getDisplayLanguage(java.util.Locale.ENGLISH)
            if (name.isNotBlank()) name else code.uppercase()
        } catch (e: Exception) {
            code.uppercase()
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun DirectPlayerScreen(
    streamUrl: String,
    mediaTitle: String,
    mediaType: String = "movie",
    tmdbId: Int = 0,
    season: Int? = null,
    episode: Int? = null,
    posterPath: String? = null,
    backdropPath: String? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitleText = if (mediaType == "tv" && season != null && episode != null) {
        "Season $season • Episode $episode"
    } else {
        "Vision TV+ Cinema"
    }
    val stream = remember(streamUrl, mediaTitle) {
        NuvioStream(
            name = mediaTitle,
            title = mediaTitle,
            url = streamUrl,
            quality = "Direct Stream"
        )
    }
    DirectPlayerScreen(
        stream = stream,
        title = mediaTitle,
        subtitle = subtitleText,
        mediaType = mediaType,
        tmdbId = tmdbId,
        season = season,
        episode = episode,
        posterPath = posterPath,
        backdropPath = backdropPath,
        onBackClick = onBackClick,
        onOpenStreamSelector = onBackClick,
        onSwitchToEmbed = onBackClick,
        modifier = modifier
    )
}

@OptIn(UnstableApi::class)
@Composable
fun DirectPlayerScreen(
    stream: NuvioStream,
    title: String,
    subtitle: String? = null,
    mediaType: String = "movie",
    tmdbId: Int = 0,
    season: Int? = null,
    episode: Int? = null,
    posterPath: String? = null,
    backdropPath: String? = null,
    onBackClick: () -> Unit,
    onOpenStreamSelector: () -> Unit,
    onSwitchToEmbed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Force landscape mode and full screen immersive
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        if (activity != null) {
            val window = activity.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (activity != null) {
                val window = activity.window
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Controls state
    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // Advanced Player Feature States
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentTracks by remember { mutableStateOf<Tracks?>(null) }

    // Subtitle Style Customization States (Default: Transparent / No Black Box)
    var subBgStyle by remember { mutableStateOf(SubtitleBgStyle.TRANSPARENT) }
    var subTextSizeSp by remember { mutableFloatStateOf(20f) }
    var subTextColor by remember { mutableStateOf(Color.White) }
    var subEdgeType by remember { mutableIntStateOf(CaptionStyleCompat.EDGE_TYPE_OUTLINE) }
    var subBottomPaddingFraction by remember { mutableFloatStateOf(0.08f) }
    var selectedSubTab by remember { mutableIntStateOf(0) }

    // Direct Web Fallback Error State
    var directWebError by remember { mutableStateOf(false) }
    var directReloadToken by remember { mutableIntStateOf(0) }

    // Feature Dialog States
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Active Bottom Overlay Tab State (null, "INFO", "CONTINUE_WATCHING")
    var activeOverlayTab by remember { mutableStateOf<String?>(null) }

    // Dynamic stream and title state for smooth in-player stream switching
    var currentStreamUrl by remember(stream.url) { mutableStateOf(stream.url) }
    var currentMediaTitle by remember(title) { mutableStateOf(title) }

    // App Database Integration for Real Continue Watching & Media Info
    val appDao = remember(context) { AppDatabase.getDatabase(context).appDao() }
    val realWatchHistory by appDao.getWatchHistory().collectAsStateWithLifecycle(initialValue = emptyList())
    val realWatchlist by appDao.getWatchlist().collectAsStateWithLifecycle(initialValue = emptyList())

    var realOverview by remember { mutableStateOf<String?>(null) }
    var realPosterPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentMediaTitle, realWatchHistory, realWatchlist) {
        val matchedWatchlist = realWatchlist.find { it.title.equals(currentMediaTitle, ignoreCase = true) }
        val matchedHistory = realWatchHistory.find { it.title.equals(currentMediaTitle, ignoreCase = true) || (it.streamUrl != null && it.streamUrl == currentStreamUrl) }
        if (matchedWatchlist != null && matchedWatchlist.overview.isNotBlank()) {
            realOverview = matchedWatchlist.overview
            realPosterPath = matchedWatchlist.backdropPath ?: matchedWatchlist.posterPath
        } else if (matchedHistory != null) {
            realPosterPath = matchedHistory.backdropPath ?: matchedHistory.posterPath
        }
    }

    // Automatically record playback in watch history
    LaunchedEffect(isPlaying, currentPosition, duration, currentStreamUrl, currentMediaTitle) {
        if (currentPosition > 2000L && duration > 0L) {
            val mediaIdKey = if (mediaType == "tv" && season != null && episode != null) {
                "${mediaType}_${tmdbId}_${season}_${episode}"
            } else if (tmdbId != 0) {
                "${mediaType}_${tmdbId}"
            } else {
                "direct_${currentMediaTitle.hashCode()}"
            }

            val rawP = posterPath ?: realPosterPath
            val formattedPoster = when {
                rawP.isNullOrEmpty() -> null
                rawP.startsWith("/") -> "https://image.tmdb.org/t/p/w500$rawP"
                else -> rawP
            }

            withContext(Dispatchers.IO) {
                appDao.saveWatchHistory(
                    WatchHistoryEntity(
                        mediaId = mediaIdKey,
                        tmdbId = if (tmdbId != 0) tmdbId else currentMediaTitle.hashCode(),
                        mediaType = mediaType,
                        title = currentMediaTitle,
                        posterPath = formattedPoster,
                        backdropPath = formattedPoster,
                        season = season,
                        episode = episode,
                        episodeTitle = subtitle,
                        currentTimeSec = currentPosition / 1000L,
                        durationSec = duration / 1000L,
                        streamUrl = currentStreamUrl,
                        streamName = stream.name,
                        lastWatchedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Gesture Control HUD State (Volume & Brightness Swipe)
    var gestureHUDText by remember { mutableStateOf<String?>(null) }
    var gestureHUDIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }

    LaunchedEffect(gestureHUDText) {
        if (gestureHUDText != null) {
            delay(1200)
            gestureHUDText = null
        }
    }

    // Double Tap Seek Indicator State
    var doubleTapSeekText by remember { mutableStateOf<String?>(null) }
    var doubleTapSeekIsLeft by remember { mutableStateOf(true) }

    LaunchedEffect(doubleTapSeekText) {
        if (doubleTapSeekText != null) {
            delay(800)
            doubleTapSeekText = null
        }
    }

    // Quick controls tooltip (Brightness & Volume)
    var quickTooltipVisible by remember { mutableStateOf(false) }
    var currentBrightness by remember {
        val lp = activity?.window?.attributes
        mutableFloatStateOf(if (lp != null && lp.screenBrightness >= 0) lp.screenBrightness else 0.5f)
    }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var currentVolume by remember {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2))
    }
    var currentVolumeFloat by remember {
        mutableFloatStateOf(currentVolume.toFloat())
    }

    // Resolve magnet links into direct native stream candidate URLs for ExoPlayer
    val p2pGatewayCandidates = remember(stream.url) {
        val raw = stream.url
        val lower = raw.lowercase()
        if (raw.startsWith("magnet:?", ignoreCase = true) || lower.contains("btih:")) {
            val hash = raw.substringAfter("btih:").substringBefore("&").substringBefore("/").trim().lowercase()
            val encoded = try { java.net.URLEncoder.encode(raw, "UTF-8") } catch (e: Exception) { raw }
            val list = mutableListOf<String>()
            
            // Candidate 1: Torrentio / Stremio node if hash exists
            if (hash.isNotEmpty() && hash.length >= 10) {
                list.add("https://torrentio.strem.fun/stream/movie/$hash.m3u8")
            }
            // Candidate 2: Webtor direct streaming API
            list.add("https://webtor.io/api/v1/stream?magnet=$encoded")
            // Candidate 3: Webtor show gateway
            list.add("https://webtor.io/show?magnet=$encoded")
            // Candidate 4: Instant.io P2P node
            list.add("https://instant.io/api/stream?magnet=$encoded")
            list.distinct()
        } else {
            listOf(raw)
        }
    }

    var currentGatewayIndex by remember(currentStreamUrl) { mutableIntStateOf(0) }
    val playableUrl = remember(p2pGatewayCandidates, currentGatewayIndex, currentStreamUrl) {
        p2pGatewayCandidates.getOrElse(currentGatewayIndex) { currentStreamUrl }
    }

    var useWebViewFallback by remember(currentStreamUrl) {
        val lower = currentStreamUrl.lowercase()
        val isExplicitWebEmbed = lower.contains("vixsrc.to/embed") || lower.contains("autoembed.co") || lower.contains("vidsrc.me/embed") || lower.contains("autoembed.cc/embed")
        mutableStateOf(isExplicitWebEmbed)
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(4000)
            controlsVisible = false
            activeOverlayTab = null
        }
    }

    var playerErrorMsg by remember(playableUrl) { mutableStateOf<String?>(null) }

    // Initialize ExoPlayer (only if direct video)
    val exoPlayer = remember(playableUrl, useWebViewFallback) {
        if (!useWebViewFallback) {
            val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    20000, // minBufferMs
                    60000, // maxBufferMs
                    2500,  // bufferForPlaybackMs
                    5000   // bufferForPlaybackAfterRebufferMs
                )
                .build()

            ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build().apply {
                playWhenReady = true

                val initTrackParams = trackSelectionParameters.buildUpon()
                    .setPreferredTextLanguage("en")
                    .setSelectUndeterminedTextLanguage(true)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .build()
                trackSelectionParameters = initTrackParams

                val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                    val userAgent = stream.headers["User-Agent"] ?: stream.headers["user-agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36"
                    setUserAgent(userAgent)
                    setConnectTimeoutMs(30000)
                    setReadTimeoutMs(30000)
                    setAllowCrossProtocolRedirects(true)
                    val customHeaders = mutableMapOf<String, String>()
                    stream.headers.forEach { (k, v) ->
                        if (!k.equals("User-Agent", ignoreCase = true)) {
                            customHeaders[k] = v
                        }
                    }
                    setDefaultRequestProperties(customHeaders)
                }

                val detectedMimeType = when {
                    playableUrl.contains(".m3u8", ignoreCase = true) || playableUrl.contains("m3u8", ignoreCase = true) || playableUrl.contains("/hls/", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
                    playableUrl.contains(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
                    playableUrl.contains(".mp4", ignoreCase = true) -> MimeTypes.VIDEO_MP4
                    playableUrl.contains(".mkv", ignoreCase = true) -> MimeTypes.VIDEO_MATROSKA
                    playableUrl.contains(".webm", ignoreCase = true) -> MimeTypes.VIDEO_WEBM
                    playableUrl.contains(".ts", ignoreCase = true) -> MimeTypes.VIDEO_MP2T
                    else -> null
                }

                val subtitleConfigs = mutableListOf<MediaItem.SubtitleConfiguration>()

                val sideLoadedSources = stream.subtitles.mapNotNull { sub ->
                    try {
                        if (sub.url.isBlank()) return@mapNotNull null
                        val subMimeType = when {
                            sub.url.contains(".vtt", ignoreCase = true) || sub.name?.contains("vtt", ignoreCase = true) == true -> MimeTypes.TEXT_VTT
                            sub.url.contains(".ass", ignoreCase = true) || sub.url.contains(".ssa", ignoreCase = true) || sub.name?.contains("ass", ignoreCase = true) == true -> MimeTypes.TEXT_SSA
                            sub.url.contains(".ttml", ignoreCase = true) || sub.url.contains(".xml", ignoreCase = true) -> MimeTypes.APPLICATION_TTML
                            else -> MimeTypes.APPLICATION_SUBRIP
                        }
                        val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                            .setMimeType(subMimeType)
                            .setLanguage(sub.language ?: "en")
                            .setLabel(sub.name ?: sub.language ?: "Subtitle")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_AUTOSELECT)
                            .build()

                        subtitleConfigs.add(subConfig)

                        val subDataSourceFactory = DefaultHttpDataSource.Factory().apply {
                            val userAgent = sub.headers?.get("User-Agent") ?: sub.headers?.get("user-agent") ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36"
                            setUserAgent(userAgent)
                            setConnectTimeoutMs(15000)
                            setReadTimeoutMs(15000)
                            setAllowCrossProtocolRedirects(true)
                            if (!sub.headers.isNullOrEmpty()) {
                                val customHeaders = mutableMapOf<String, String>()
                                sub.headers.forEach { (k, v) ->
                                    if (!k.equals("User-Agent", ignoreCase = true)) {
                                        customHeaders[k] = v
                                    }
                                }
                                setDefaultRequestProperties(customHeaders)
                            }
                        }

                        SingleSampleMediaSource.Factory(subDataSourceFactory)
                            .createMediaSource(subConfig, C.TIME_UNSET)
                    } catch (e: Exception) {
                        null
                    }
                }

                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(Uri.parse(playableUrl))
                    .setSubtitleConfigurations(subtitleConfigs)

                if (detectedMimeType != null) {
                    mediaItemBuilder.setMimeType(detectedMimeType)
                }

                val mediaItem = mediaItemBuilder.build()
                val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                val mainSource = mediaSourceFactory.createMediaSource(mediaItem)

                val finalSource = if (sideLoadedSources.isNotEmpty()) {
                    MergingMediaSource(mainSource, *sideLoadedSources.toTypedArray())
                } else {
                    mainSource
                }

                setMediaSource(finalSource)
                prepare()
            }
        } else null
    }

    // Apply speed changes to ExoPlayer
    LaunchedEffect(playbackSpeed, exoPlayer) {
        exoPlayer?.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    // Track position & events
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isLoading = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    playerErrorMsg = null
                    duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
                val hasSelectedText = tracks.groups.any { group ->
                    group.type == C.TRACK_TYPE_TEXT && group.isSelected
                }
                if (!hasSelectedText) {
                    val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                    if (textGroups.isNotEmpty()) {
                        var targetGroup: Tracks.Group? = null
                        var targetTrackIndex = 0

                        for (group in textGroups) {
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                val lang = format.language?.lowercase() ?: ""
                                val label = format.label?.lowercase() ?: ""
                                if (lang.contains("en") || lang.contains("eng") || label.contains("en") || label.contains("english")) {
                                    targetGroup = group
                                    targetTrackIndex = i
                                    break
                                }
                            }
                            if (targetGroup != null) break
                        }

                        if (targetGroup == null) {
                            targetGroup = textGroups.first()
                            targetTrackIndex = 0
                        }

                        try {
                            exoPlayer?.let { player ->
                                val builder = player.trackSelectionParameters.buildUpon()
                                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                builder.addOverride(androidx.media3.common.TrackSelectionOverride(targetGroup.mediaTrackGroup, targetTrackIndex))
                                player.trackSelectionParameters = builder.build()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("DirectPlayerScreen", "Auto-select subtitle error: ${e.message}")
                        }
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("DirectPlayerScreen", "ExoPlayer error on candidate #$currentGatewayIndex ($playableUrl): ${error.message}")
                if (currentGatewayIndex < p2pGatewayCandidates.size - 1) {
                    // Try next P2P stream gateway URL
                    currentGatewayIndex += 1
                } else {
                    val urlLower = stream.url.lowercase()
                    if (urlLower.contains("embed") || urlLower.contains("vixsrc") || urlLower.contains("vidlink") || urlLower.contains("autoembed.co") || urlLower.contains("vidsrc.me")) {
                        useWebViewFallback = true
                    } else {
                        playerErrorMsg = error.localizedMessage ?: "Stream Connection Timed Out / Failed"
                    }
                }
            }
        }
        exoPlayer?.addListener(listener)

        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
            activity?.window?.let { window ->
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    // Periodic position update
    LaunchedEffect(isPlaying, exoPlayer) {
        while (isPlaying && exoPlayer != null) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // Helper functions to parse audio & text tracks from ExoPlayer
    val extractedAudioTracks = remember(currentTracks) {
        val list = mutableListOf<ExtractedTrackItem>()
        currentTracks?.groups?.forEachIndexed { groupIndex, group ->
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val langDisplay = getDisplayLanguageName(format.language)
                    val channels = when (format.channelCount) {
                        6 -> "5.1 Surround"
                        8 -> "7.1 Surround"
                        2 -> "Stereo"
                        1 -> "Mono"
                        else -> null
                    }
                    val label = when {
                        !format.label.isNullOrEmpty() -> format.label!!
                        else -> listOfNotNull(langDisplay, channels).joinToString(" • ")
                    }
                    list.add(
                        ExtractedTrackItem(
                            groupIndex = groupIndex,
                            trackIndex = trackIndex,
                            label = if (label.isNotBlank()) label else "Audio Track #${list.size + 1}",
                            language = format.language,
                            isSelected = group.isTrackSelected(trackIndex)
                        )
                    )
                }
            }
        }
        list
    }

    val extractedSubtitleTracks = remember(currentTracks, stream.subtitles) {
        val list = mutableListOf<ExtractedTrackItem>()
        currentTracks?.groups?.forEachIndexed { groupIndex, group ->
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val langDisplay = getDisplayLanguageName(format.language)
                    val label = when {
                        !format.label.isNullOrEmpty() -> format.label!!
                        format.language != null -> langDisplay
                        else -> "Subtitle Track #${list.size + 1}"
                    }
                    list.add(
                        ExtractedTrackItem(
                            groupIndex = groupIndex,
                            trackIndex = trackIndex,
                            label = label,
                            language = format.language,
                            isSelected = group.isTrackSelected(trackIndex)
                        )
                    )
                }
            }
        }
        list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val screenWidth = size.width
                        val isLeftHalf = change.position.x < screenWidth * 0.5f

                        if (isLeftHalf) {
                            // Left side swipe -> Screen Brightness
                            val delta = -dragAmount / 700f
                            val newBrightness = (currentBrightness + delta).coerceIn(0.05f, 1.0f)
                            currentBrightness = newBrightness
                            val lp = activity?.window?.attributes
                            if (lp != null) {
                                lp.screenBrightness = newBrightness
                                activity.window.attributes = lp
                            }
                            gestureHUDText = "Brightness ${(newBrightness * 100).toInt()}%"
                            gestureHUDIcon = Icons.Default.Brightness6
                        } else {
                            // Right side swipe -> Media Volume
                            val delta = -dragAmount / 700f * maxVolume.toFloat()
                            currentVolumeFloat = (currentVolumeFloat + delta).coerceIn(0f, maxVolume.toFloat())
                            val targetVolInt = currentVolumeFloat.roundToInt().coerceIn(0, maxVolume)
                            if (targetVolInt != currentVolume) {
                                currentVolume = targetVolInt
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolInt, 0)
                            }
                            val pct = if (maxVolume > 0) ((currentVolumeFloat / maxVolume.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
                            gestureHUDText = "Volume $pct%"
                            gestureHUDIcon = Icons.Default.VolumeUp
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        exoPlayer?.let { player ->
                            if (offset.x < screenWidth * 0.45f) {
                                val target = (player.currentPosition - 10000).coerceAtLeast(0)
                                player.seekTo(target)
                                currentPosition = target
                                doubleTapSeekText = "-10s Rewind"
                                doubleTapSeekIsLeft = true
                            } else if (offset.x > screenWidth * 0.55f) {
                                val target = (player.currentPosition + 10000).coerceAtMost(duration)
                                player.seekTo(target)
                                currentPosition = target
                                doubleTapSeekText = "+10s Forward"
                                doubleTapSeekIsLeft = false
                            }
                        }
                    },
                    onTap = {
                        if (activeOverlayTab != null) {
                            activeOverlayTab = null
                        } else {
                            controlsVisible = !controlsVisible
                        }
                    }
                )
            }
    ) {
        // Video View: ExoPlayer or Web Fallback
        if (useWebViewFallback || exoPlayer == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                CinemaWebViewPlayer(
                    embedUrl = playableUrl,
                    reloadToken = directReloadToken,
                    onErrorOccurred = {
                        directWebError = true
                    }
                )

                if (directWebError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xDD000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFA0F111A))
                                .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(20.dp))
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Stream Connection Failed / Timed Out",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "The stream server is unresponsive or offline. Switch server below.",
                                color = AppleTvTextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        directWebError = false
                                        onOpenStreamSelector()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AppleTvAccent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "⚡ Switch Server",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        directWebError = false
                                        directReloadToken += 1
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "🔄 Retry",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        this.resizeMode = resizeMode
                        subtitleView?.visibility = android.view.View.VISIBLE
                        val bgColor = when (subBgStyle) {
                            SubtitleBgStyle.TRANSPARENT -> android.graphics.Color.TRANSPARENT
                            SubtitleBgStyle.SEMI_TRANSPARENT -> android.graphics.Color.argb(140, 0, 0, 0)
                            SubtitleBgStyle.SOLID_BLACK -> android.graphics.Color.BLACK
                        }
                        val style = CaptionStyleCompat(
                            subTextColor.toArgb(),
                            bgColor,
                            android.graphics.Color.TRANSPARENT,
                            subEdgeType,
                            android.graphics.Color.BLACK,
                            Typeface.DEFAULT_BOLD
                        )
                        subtitleView?.setStyle(style)
                        subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subTextSizeSp)
                        subtitleView?.setBottomPaddingFraction(subBottomPaddingFraction)
                    }
                },
                update = { view ->
                    view.resizeMode = resizeMode
                    val bgColor = when (subBgStyle) {
                        SubtitleBgStyle.TRANSPARENT -> android.graphics.Color.TRANSPARENT
                        SubtitleBgStyle.SEMI_TRANSPARENT -> android.graphics.Color.argb(140, 0, 0, 0)
                        SubtitleBgStyle.SOLID_BLACK -> android.graphics.Color.BLACK
                    }
                    val style = CaptionStyleCompat(
                        subTextColor.toArgb(),
                        bgColor,
                        android.graphics.Color.TRANSPARENT,
                        subEdgeType,
                        android.graphics.Color.BLACK,
                        Typeface.DEFAULT_BOLD
                    )
                    view.subtitleView?.setStyle(style)
                    view.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subTextSizeSp)
                    view.subtitleView?.setBottomPaddingFraction(subBottomPaddingFraction)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading Indicator Overlay with status text
        if (isLoading && !useWebViewFallback && playerErrorMsg == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xCC0B0D14))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = AppleTvAccent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Connecting & Buffering Stream...",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Establishing high-speed CDN connection",
                            color = AppleTvTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Direct Player Connection Error Overlay
        if (playerErrorMsg != null && !useWebViewFallback) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFA0F111A))
                        .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Stream Connection Failed / Timed Out",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = playerErrorMsg ?: "Server took too long to respond. You can retry or switch server.",
                        color = AppleTvTextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                playerErrorMsg = null
                                exoPlayer?.prepare()
                                exoPlayer?.play()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppleTvAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🔄 Retry",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                playerErrorMsg = null
                                onOpenStreamSelector()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⚡ Switch Server",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                playerErrorMsg = null
                                useWebViewFallback = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x2238BDF8)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🌐 Web Player",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Volume / Brightness Swipe Gesture HUD Toast Overlay
        if (gestureHUDText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xDD0F111A))
                    .border(1.dp, AppleTvAccent, RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    gestureHUDIcon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AppleTvAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = gestureHUDText!!,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Animated Double Tap Seek Toast Overlay
        if (doubleTapSeekText != null) {
            Box(
                modifier = Modifier
                    .align(if (doubleTapSeekIsLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 60.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC0F111A))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (doubleTapSeekIsLeft) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = null,
                            tint = AppleTvAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = doubleTapSeekText!!,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!doubleTapSeekIsLeft) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = null,
                            tint = AppleTvAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Custom Apple TV+ Style Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xDD000000),
                                Color(0x33000000),
                                Color(0xF2000000)
                            )
                        )
                    )
            ) {
                // Top Control Bar (Close, Aspect Ratio, Rating Badge on Left, Cast & Volume on Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top Left Buttons: Close (X), PiP/Aspect & Rating Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x662D313E))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                resizeMode = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x662D313E))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Rating Divider & Rating Details Badge
                        Box(
                            modifier = Modifier
                                .height(22.dp)
                                .width(1.dp)
                                .background(Color(0x33FFFFFF))
                        )

                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "U/A 13+",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text(
                                text = "Fantasy Violence",
                                color = Color(0xAAFFFFFF),
                                fontSize = 11.sp,
                                lineHeight = 13.sp
                            )
                            Text(
                                text = "Language",
                                color = Color(0xAAFFFFFF),
                                fontSize = 11.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    // Top Right Buttons: Cast & Mute/Volume
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Scanning for Cast devices...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x662D313E))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = "Cast",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        var isMuted by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                exoPlayer?.volume = if (isMuted) 0f else 1f
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x662D313E))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Volume",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Center Play / Pause Controls
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { exoPlayer?.let { it.seekTo((it.currentPosition - 10000).coerceAtLeast(0)) } },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0x662D313E))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                exoPlayer?.let {
                                    if (it.isPlaying) it.pause() else it.play()
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0x882D313E))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(
                            onClick = { exoPlayer?.let { it.seekTo((it.currentPosition + 10000).coerceAtMost(duration)) } },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0x662D313E))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Bottom Overlay Area (Media Title, Audio/Subtitle Controls, Timeline Slider, Info Pills)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Top Pills Row: Info & Continue Watching
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Info Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (activeOverlayTab == "INFO") Color.White else Color(0x662D313E))
                                .clickable {
                                    activeOverlayTab = if (activeOverlayTab == "INFO") null else "INFO"
                                }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Info",
                                color = if (activeOverlayTab == "INFO") Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Continue Watching Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (activeOverlayTab == "CONTINUE_WATCHING") Color.White else Color(0x662D313E))
                                .clickable {
                                    activeOverlayTab = if (activeOverlayTab == "CONTINUE_WATCHING") null else "CONTINUE_WATCHING"
                                }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Continue Watching",
                                color = if (activeOverlayTab == "CONTINUE_WATCHING") Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Content Section based on active overlay tab
                    when (activeOverlayTab) {
                        "INFO" -> {
                            // INFO TAB PANEL VIEW (Loads Real Media Overview & Info)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Left Column: Title, Overview, Badges
                                Column(modifier = Modifier.weight(1f).padding(end = 24.dp)) {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val displayOverview = realOverview?.ifBlank { null } ?: "Streaming $title ${subtitle?.let { "• $it" } ?: ""} via ${stream.provider ?: stream.repoGroup ?: "Direct Cinema"}. Quality: ${stream.quality ?: "1080p HD"}."
                                    Text(
                                        text = displayOverview,
                                        color = Color(0xDDFFFFFF),
                                        fontSize = 13.sp,
                                        maxLines = 3,
                                        lineHeight = 17.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val runtimeStr = if (duration > 0) "${duration / (1000 * 60)} min" else "HD"
                                        Text(
                                            text = "${subtitle ?: "Cinema"} • $runtimeStr",
                                            color = Color(0xAAFFFFFF),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val dynamicBadges = listOfNotNull("U/A 13+", stream.quality ?: "4K", "Dolby VISION", "Dolby ATMOS", if (extractedSubtitleTracks.isNotEmpty() || stream.subtitles.isNotEmpty()) "CC" else null, "AD")
                                        dynamicBadges.forEach { badge ->
                                            Box(
                                                modifier = Modifier
                                                    .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = badge,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Right Column: From Beginning & More Info Buttons
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0x882D313E))
                                            .clickable {
                                                exoPlayer?.seekTo(0)
                                                exoPlayer?.play()
                                                activeOverlayTab = null
                                            }
                                            .padding(horizontal = 20.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "From Beginning",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0x882D313E))
                                            .clickable {
                                                Toast.makeText(context, "$title - Vision TV+ Cinema", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 20.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "More Info",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "CONTINUE_WATCHING" -> {
                            // CONTINUE WATCHING TAB PANEL VIEW (Loads Actual Watched Media)
                            if (realWatchHistory.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x441E2028))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No watch history recorded yet. Start watching movies or shows to continue where you left off!",
                                        color = Color(0xBBFFFFFF),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(realWatchHistory, key = { it.mediaId }) { history ->
                                        val epSub = if (history.season != null && history.episode != null) "S${history.season}, E${history.episode}" else "Movie"
                                        val durationStr = if (history.durationSec > 0) "${history.durationSec / 60}m" else "HD"
                                        val progress = if (history.durationSec > 0) (history.currentTimeSec.toFloat() / history.durationSec.toFloat()).coerceIn(0f, 1f) else 0.5f

                                        val rawImg = history.backdropPath ?: history.posterPath
                                        val imageUrl = when {
                                            rawImg.isNullOrEmpty() -> null
                                            rawImg.startsWith("/") -> "https://image.tmdb.org/t/p/w500$rawImg"
                                            else -> rawImg
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(220.dp)
                                                .height(125.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF1E2028))
                                                .clickable {
                                                    if (!history.streamUrl.isNullOrEmpty() && (history.streamUrl.startsWith("http://") || history.streamUrl.startsWith("https://"))) {
                                                        currentStreamUrl = history.streamUrl
                                                        currentMediaTitle = history.title
                                                        activeOverlayTab = null
                                                        Toast.makeText(context, "Resuming ${history.title} ($epSub)", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        onOpenStreamSelector()
                                                    }
                                                }
                                        ) {
                                            if (imageUrl != null) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(imageUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = history.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Brush.linearGradient(listOf(Color(0xFF2C303E), Color(0xFF141722))))
                                                        .padding(12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = history.title,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }

                                            // Top Right tv+ logo
                                            Text(
                                                text = "tv+",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                            )

                                            // Title text on poster center
                                            Text(
                                                text = history.title.uppercase(),
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                maxLines = 2,
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .padding(horizontal = 10.dp)
                                            )

                                            // Bottom Gradient Overlay & Play Duration
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color.Transparent, Color(0xF2000000))
                                                        )
                                                    )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "$epSub • $durationStr",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    Icon(
                                                        imageVector = Icons.Default.MoreVert,
                                                        contentDescription = "Options",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                // Progress bar line
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(progress)
                                                        .height(3.dp)
                                                        .background(AppleTvAccent)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // STANDARD TIMELINE & TRACK CONTROLS VIEW
                            // Title & Audio/Subtitle Track Quick Toggle Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    if (!subtitle.isNullOrEmpty()) {
                                        Text(
                                            text = subtitle,
                                            color = Color(0xBBFFFFFF),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Audio Track Modal Launcher
                                    IconButton(
                                        onClick = { showAudioDialog = true },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x662D313E))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = "Audio Track",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Subtitle Track Modal Launcher
                                    IconButton(
                                        onClick = { showSubtitleDialog = true },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x662D313E))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Subtitles,
                                            contentDescription = "Subtitles",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Timeline Duration Labels
                            val remainingMs = (duration - currentPosition).coerceAtLeast(0L)
                            val remainingStr = if (remainingMs > 0) "-${formatTime(remainingMs)}" else "00:00"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatTime(currentPosition),
                                    color = Color(0xDDFFFFFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = remainingStr,
                                    color = Color(0xDDFFFFFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Sleek White Progress Bar
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                                onValueChange = { frac ->
                                    currentPosition = (frac * duration).toLong()
                                    exoPlayer?.seekTo(currentPosition)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color(0x44FFFFFF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }


    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        Dialog(
            onDismissRequest = { showSpeedDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp)),
                color = Color(0xFA0F111A)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Playback Speed",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        val isSelected = playbackSpeed == speed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0x3311998E) else Color.Transparent)
                                .clickable {
                                    playbackSpeed = speed
                                    showSpeedDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${speed}x Normal Speed",
                                color = if (isSelected) AppleTvAccent else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AppleTvAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Subtitle Selection & Advanced Customization Dialog
    if (showSubtitleDialog) {
        Dialog(
            onDismissRequest = { showSubtitleDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .width(360.dp)
                    .heightIn(max = 520.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)),
                color = Color(0xFF1E2029)
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Subtitle Settings",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Selector: Tracks vs Appearance
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x11FFFFFF))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedSubTab == 0) AppleTvAccent else Color.Transparent)
                                .clickable { selectedSubTab = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tracks",
                                color = if (selectedSubTab == 0) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedSubTab == 1) AppleTvAccent else Color.Transparent)
                                .clickable { selectedSubTab = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Appearance",
                                color = if (selectedSubTab == 1) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (selectedSubTab == 0) {
                            // TAB 0: SUBTITLE TRACKS
                            val isAnySelected = extractedSubtitleTracks.any { it.isSelected }

                            // Option: Off
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!isAnySelected) Color(0x22FFFFFF) else Color.Transparent)
                                    .clickable {
                                        exoPlayer?.let { player ->
                                            val builder = player.trackSelectionParameters.buildUpon()
                                            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                            builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                            player.trackSelectionParameters = builder.build()
                                        }
                                        showSubtitleDialog = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isAnySelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                } else {
                                    Spacer(modifier = Modifier.width(28.dp))
                                }
                                Text(
                                    text = "Off",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (!isAnySelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0x1AFFFFFF))
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (extractedSubtitleTracks.isEmpty() && stream.subtitles.isEmpty()) {
                                Text(
                                    text = "No subtitle tracks found in stream",
                                    color = Color(0x88FFFFFF),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                extractedSubtitleTracks.forEach { trackItem ->
                                    val isSelected = trackItem.isSelected
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0x22FFFFFF) else Color.Transparent)
                                            .clickable {
                                                exoPlayer?.let { player ->
                                                    val builder = player.trackSelectionParameters.buildUpon()
                                                    builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                    builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                                    val group = currentTracks?.groups?.getOrNull(trackItem.groupIndex)
                                                    if (group != null) {
                                                        builder.addOverride(TrackSelectionOverride(group.mediaTrackGroup, trackItem.trackIndex))
                                                    }
                                                    player.trackSelectionParameters = builder.build()
                                                }
                                                showSubtitleDialog = false
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                        } else {
                                            Spacer(modifier = Modifier.width(28.dp))
                                        }
                                        Text(
                                            text = trackItem.label,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                val listedLabels = extractedSubtitleTracks.map { it.label.lowercase() }
                                val remainingSubs = stream.subtitles.filter { sub ->
                                    val l = (sub.name ?: sub.language ?: "").lowercase()
                                    l.isNotEmpty() && !listedLabels.contains(l)
                                }

                                if (remainingSubs.isNotEmpty()) {
                                    remainingSubs.forEachIndexed { subIdx, sub ->
                                        val label = sub.name ?: sub.language ?: "Subtitle #${subIdx + 1}"
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    exoPlayer?.let { player ->
                                                        val builder = player.trackSelectionParameters.buildUpon()
                                                        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                        builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                                        val textGroups = currentTracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT }
                                                        if (!textGroups.isNullOrEmpty()) {
                                                            val targetGroup = textGroups.getOrNull(subIdx) ?: textGroups.first()
                                                            builder.addOverride(TrackSelectionOverride(targetGroup.mediaTrackGroup, 0))
                                                        }
                                                        player.trackSelectionParameters = builder.build()
                                                    }
                                                    showSubtitleDialog = false
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Spacer(modifier = Modifier.width(28.dp))
                                            Text(
                                                text = label,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // TAB 1: ADVANCED APPEARANCE CUSTOMIZATION

                            // Live Subtitle Preview Container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F1117))
                                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when (subBgStyle) {
                                                SubtitleBgStyle.TRANSPARENT -> Color.Transparent
                                                SubtitleBgStyle.SEMI_TRANSPARENT -> Color(0x99000000)
                                                SubtitleBgStyle.SOLID_BLACK -> Color.Black
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Sample Subtitle Text",
                                        color = subTextColor,
                                        fontSize = (subTextSizeSp - 4).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 1. Text Color
                            Text(
                                text = "TEXT COLOR",
                                color = Color(0xAAFFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val colorsList = listOf(
                                Color(0xFFFFFFFF) to "White",
                                Color(0xFFFFEB3B) to "Yellow",
                                Color(0xFF00E5FF) to "Cyan",
                                Color(0xFF00FF66) to "Green",
                                Color(0xFFFF4081) to "Pink"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                colorsList.forEach { (colorVal, _) ->
                                    val isSelected = subTextColor == colorVal
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(colorVal)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) AppleTvAccent else Color(0x44FFFFFF),
                                                shape = CircleShape
                                            )
                                            .clickable { subTextColor = colorVal }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. Font Size
                            Text(
                                text = "FONT SIZE",
                                color = Color(0xAAFFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val fontSizes = listOf(
                                16f to "Small",
                                20f to "Medium",
                                24f to "Large",
                                28f to "XL"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                fontSizes.forEach { (sizeSp, label) ->
                                    val isSelected = subTextSizeSp == sizeSp
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0x3338BDF8) else Color(0x11FFFFFF))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF38BDF8) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { subTextSizeSp = sizeSp }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3. Background Style
                            Text(
                                text = "BACKGROUND STYLE",
                                color = Color(0xAAFFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            SubtitleBgStyle.entries.forEach { bgOption ->
                                val isSelected = subBgStyle == bgOption
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0x22FFFFFF) else Color.Transparent)
                                        .clickable { subBgStyle = bgOption }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { subBgStyle = bgOption },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = bgOption.label,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 4. Edge Effect / Stroke
                            Text(
                                text = "TEXT EDGE / OUTLINE",
                                color = Color(0xAAFFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val edgeOptions = listOf(
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE to "Outline",
                                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW to "Shadow",
                                CaptionStyleCompat.EDGE_TYPE_RAISED to "Raised",
                                CaptionStyleCompat.EDGE_TYPE_NONE to "None"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                edgeOptions.forEach { (edgeType, label) ->
                                    val isSelected = subEdgeType == edgeType
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0x3338BDF8) else Color(0x11FFFFFF))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF38BDF8) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { subEdgeType = edgeType }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 5. Vertical Position Offset
                            Text(
                                text = "BOTTOM MARGIN POSITION",
                                color = Color(0xAAFFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val positions = listOf(
                                0.04f to "Low",
                                0.08f to "Standard",
                                0.14f to "High"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                positions.forEach { (frac, label) ->
                                    val isSelected = subBottomPaddingFraction == frac
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0x3338BDF8) else Color(0x11FFFFFF))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF38BDF8) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { subBottomPaddingFraction = frac }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Audio Track Selection Dialog
    if (showAudioDialog) {
        Dialog(
            onDismissRequest = { showAudioDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .width(340.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(22.dp)),
                color = Color(0xFF22242D)
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 18.dp, horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Audio",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (extractedAudioTracks.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Default Audio Track",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        extractedAudioTracks.forEach { trackItem ->
                            val isSelected = trackItem.isSelected
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0x22FFFFFF) else Color.Transparent)
                                    .clickable {
                                        exoPlayer?.let { player ->
                                            val builder = player.trackSelectionParameters.buildUpon()
                                            builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                            val group = currentTracks?.groups?.getOrNull(trackItem.groupIndex)
                                            if (group != null) {
                                                builder.addOverride(TrackSelectionOverride(group.mediaTrackGroup, trackItem.trackIndex))
                                            }
                                            player.trackSelectionParameters = builder.build()
                                        }
                                        showAudioDialog = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                } else {
                                    Spacer(modifier = Modifier.width(28.dp))
                                }
                                Text(
                                    text = trackItem.label,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
