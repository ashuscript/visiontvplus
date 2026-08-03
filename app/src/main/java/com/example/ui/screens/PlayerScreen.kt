package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.theme.AppleTvAccent
import com.example.ui.theme.AppleTvTextSecondary
import kotlinx.coroutines.delay

import android.media.AudioManager
import android.content.Context
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import com.example.data.repository.NuvioStream
import com.example.data.repository.PlaybackMode
import com.example.data.repository.ScraperExecutionState
import com.example.ui.components.NuvioStreamSelectorDialog

enum class StreamingSource(val label: String, val isDefault: Boolean) {
    VIDSRC("⚡ VidSrc (Default)", isDefault = true),
    VIXSRC("🌐 VixSrc Cinema", isDefault = false)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    tmdbId: Int = 0,
    mediaType: String = "movie",
    season: Int = 1,
    episode: Int = 1,
    vidSrcUrl: String,
    vixSrcUrl: String,
    title: String,
    subtitle: String? = null,
    availableStreams: List<NuvioStream> = emptyList(),
    isScrapingNuvio: Boolean = false,
    scraperStates: Map<String, ScraperExecutionState> = emptyMap(),
    onStartNuvioScraping: (tmdbId: Int, mediaType: String, season: Int, episode: Int, forceRefresh: Boolean) -> Unit = { _, _, _, _, _ -> },
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val haptic = LocalHapticFeedback.current

    var showStreamSelector by remember { mutableStateOf(true) }
    var selectedDirectStream by remember { mutableStateOf<NuvioStream?>(null) }
    var playbackMode by remember { mutableStateOf<PlaybackMode?>(PlaybackMode.DIRECT) }

    // Auto-start stream scraping when player screen opens
    LaunchedEffect(tmdbId, mediaType, season, episode) {
        onStartNuvioScraping(tmdbId, mediaType, season, episode, false)
    }

    // Nuvio Stream Selector Dialog
    if (showStreamSelector) {
        NuvioStreamSelectorDialog(
            mediaTitle = title,
            mediaSubtitle = subtitle,
            isScraping = isScrapingNuvio,
            scraperStates = scraperStates,
            streams = availableStreams,
            onStreamSelected = { stream ->
                selectedDirectStream = stream
                playbackMode = PlaybackMode.DIRECT
                showStreamSelector = false
            },
            onRetryScraping = {
                onStartNuvioScraping(tmdbId, mediaType, season, episode, true)
            },
            onDismiss = {
                showStreamSelector = false
                if (selectedDirectStream == null) {
                    onBackClick()
                }
            }
        )
    }

    // If Direct Mode is active and a stream is selected, show Direct Native Player
    if (playbackMode == PlaybackMode.DIRECT && selectedDirectStream != null) {
        DirectPlayerScreen(
            stream = selectedDirectStream!!,
            title = title,
            subtitle = subtitle,
            onBackClick = {
                selectedDirectStream = null
                showStreamSelector = true
            },
            onOpenStreamSelector = {
                showStreamSelector = true
            },
            onSwitchToEmbed = {
                selectedDirectStream = null
                playbackMode = PlaybackMode.EMBED
            },
            modifier = modifier
        )
        return
    }

    // Audio & Brightness Managers
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var currentVolume by remember {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2))
    }
    var currentBrightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.7f
        )
    }

    var activeSource by remember { mutableStateOf(StreamingSource.VIDSRC) }
    var controlsVisible by remember { mutableStateOf(true) }
    var quickTooltipVisible by remember { mutableStateOf(false) }
    var tooltipInteractionToken by remember { mutableIntStateOf(0) }
    var isLandscape by remember { mutableStateOf(true) }
    var reloadToken by remember { mutableIntStateOf(0) }

    val activeUrl = when (activeSource) {
        StreamingSource.VIDSRC -> vidSrcUrl
        StreamingSource.VIXSRC -> vixSrcUrl
    }

    val isPlayingVideo = !showStreamSelector && (selectedDirectStream != null || playbackMode == PlaybackMode.EMBED)

    // Manage orientation and system bars: force landscape during active video playback, allow flexible orientation during stream/mode selection
    DisposableEffect(isPlayingVideo) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val window = activity?.window

        if (isPlayingVideo) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    var handleVisible by remember { mutableStateOf(false) }
    var webErrorOccurred by remember(activeUrl) { mutableStateOf(false) }

    // Auto-hide app top controls quickly (2.5 seconds) for a clean video view
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(2500)
            controlsVisible = false
        } else {
            handleVisible = true
            delay(2000)
            handleVisible = false
        }
    }

    // Quick Tooltip Auto-Hide timer (2 seconds)
    LaunchedEffect(quickTooltipVisible, tooltipInteractionToken) {
        if (quickTooltipVisible) {
            delay(2000)
            quickTooltipVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen")
    ) {
        // Embed WebView Video Container (Only rendered when EMBED mode is selected and no selection dialogs are active)
        val shouldRenderWebView = playbackMode == PlaybackMode.EMBED && !showStreamSelector && selectedDirectStream == null

        if (shouldRenderWebView) {
            Box(modifier = Modifier.fillMaxSize()) {
                CinemaWebViewPlayer(
                    embedUrl = activeUrl,
                    reloadToken = reloadToken,
                    onErrorOccurred = {
                        webErrorOccurred = true
                    }
                )

                if (webErrorOccurred) {
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
                                text = "Stream Failed to Load (404 / Offline)",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "The current stream or server returned an error. Select another server below.",
                                color = AppleTvTextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        webErrorOccurred = false
                                        showStreamSelector = true
                                        onStartNuvioScraping(tmdbId, mediaType, season, episode, false)
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
                                        webErrorOccurred = false
                                        reloadToken += 1
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
            // Ambient Cinema Background when dialogs are active or switching
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1A1F2C), Color(0xFF090A0F)),
                            center = androidx.compose.ui.geometry.Offset.Unspecified
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!subtitle.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            color = AppleTvTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Invisible screen tap detector when controls are auto-hidden (OLED Burn-in Protection)
        if (!controlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        controlsVisible = true
                    }
            )
        }

        // Floating Apple TV Header Controls Layer
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFA0F111A),
                                    Color(0xEE121422)
                                )
                            )
                        )
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBackClick()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x44222533))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AppleTvAccent)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "VISION TV+",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                color = AppleTvTextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Nuvio Direct Streams Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x3311998E))
                            .border(1.dp, AppleTvAccent, RoundedCornerShape(16.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showStreamSelector = true
                                onStartNuvioScraping(tmdbId, mediaType, season, episode, false)
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⚡ Nuvio Direct",
                            color = AppleTvAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Quick Tooltip (Brightness & Volume Sliders)
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            quickTooltipVisible = !quickTooltipVisible
                            tooltipInteractionToken++
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (quickTooltipVisible) AppleTvAccent else Color(0x44222533))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Quick Controls",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Reload Stream
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            reloadToken++
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x44222533))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Player",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Orientation / Fullscreen Toggle
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isLandscape = !isLandscape
                            activity?.requestedOrientation = if (isLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x44222533))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Orientation",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Collapse Controls Header
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            controlsVisible = false
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x44222533))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Collapse",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Source Switcher Bar (VidSrc default vs VixSrc)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(StreamingSource.entries.toTypedArray()) { source ->
                            val isSelected = activeSource == source
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) AppleTvAccent else Color(0xDD0F111A))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.White else Color(0x33FFFFFF),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        activeSource = source
                                    }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = source.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Quick Controls Tooltip Overlay (Brightness & Volume Sliders)
        AnimatedVisibility(
            visible = quickTooltipVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFA0F111A))
                    .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Quick Controls (Auto-hides)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Brightness Control
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = "Brightness",
                        tint = AppleTvAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = currentBrightness,
                        onValueChange = { newVal ->
                            currentBrightness = newVal
                            val lp = activity?.window?.attributes
                            if (lp != null) {
                                lp.screenBrightness = newVal.coerceIn(0.05f, 1.0f)
                                activity.window.attributes = lp
                            }
                            tooltipInteractionToken++
                        },
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppleTvAccent,
                            activeTrackColor = AppleTvAccent,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(currentBrightness * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Volume Control
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Volume",
                        tint = AppleTvAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = currentVolume.toFloat(),
                        onValueChange = { newVal ->
                            val volIndex = newVal.toInt()
                            currentVolume = volIndex
                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, volIndex, 0)
                            tooltipInteractionToken++
                        },
                        valueRange = 0f..maxVolume.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = AppleTvAccent,
                            activeTrackColor = AppleTvAccent,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val pct = if (maxVolume > 0) ((currentVolume.toFloat() / maxVolume) * 100).toInt() else 0
                    Text(
                        text = "$pct%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun CinemaWebViewPlayer(
    embedUrl: String,
    reloadToken: Int,
    onErrorOccurred: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var lastLoadedUrl by remember { mutableStateOf("") }
    var lastReloadToken by remember { mutableIntStateOf(-1) }
    var isPageLoading by remember(embedUrl, reloadToken) { mutableStateOf(true) }

    // Connection Timeout Watchdog (25 seconds for P2P/WebTorrent buffering)
    LaunchedEffect(embedUrl, reloadToken) {
        isPageLoading = true
        delay(25000)
        if (isPageLoading) {
            onErrorOccurred?.invoke()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.destroy()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    setBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowFileAccess = true
                        allowContentAccess = true
                        loadsImagesAutomatically = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
                    }

                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun onWebError() {
                            activity?.runOnUiThread {
                                isPageLoading = false
                                onErrorOccurred?.invoke()
                            }
                        }
                    }, "AndroidError")

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            if (newProgress >= 80) {
                                isPageLoading = false
                            }
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customView = view
                            customViewCallback = callback
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }

                        override fun onHideCustomView() {
                            customViewCallback?.onCustomViewHidden()
                            customView = null
                            customViewCallback = null
                        }

                        override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                            try {
                                request?.grant(request.resources)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }

                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?
                        ): Boolean {
                            // Block popup window creation from ad triggers
                            return false
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: android.webkit.WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            if (errorResponse?.statusCode in 400..599 && request?.isForMainFrame == true) {
                                isPageLoading = false
                                onErrorOccurred?.invoke()
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                isPageLoading = false
                                onErrorOccurred?.invoke()
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val urlStr = request?.url?.toString() ?: return false

                            if (urlStr.startsWith("magnet:?", ignoreCase = true)) {
                                val encoded = try { java.net.URLEncoder.encode(urlStr, "UTF-8") } catch (e: Exception) { urlStr }
                                view?.loadUrl("https://webtor.io/show?magnet=$encoded")
                                return true
                            }

                            // Block external app intent redirects (intent://, market://, etc.)
                            if (urlStr.startsWith("intent:") ||
                                urlStr.startsWith("market:") ||
                                urlStr.startsWith("vlc:") ||
                                urlStr.startsWith("whatsapp:") ||
                                urlStr.startsWith("tg:")
                            ) {
                                return true // Prevent external app launcher popup
                            }

                            // Keep internal player and server switcher requests inside WebView
                            return false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isPageLoading = false
                            // Clean canvas, fix subtitle alignment to bottom
                            val cleanStyleJs = """
                                (function() {
                                    try {
                                        window.open = function() { return null; };

                                        // Error detection in page text
                                        var bodyText = (document.body ? document.body.innerText : '') + ' ' + document.title;
                                        if ((bodyText.includes('403') && (bodyText.includes('Forbidden') || bodyText.includes('Access Denied') || bodyText.includes('error'))) ||
                                            bodyText.includes('Access Denied') || bodyText.includes('Hotlinking Disabled')) {
                                            if (window.AndroidError) { window.AndroidError.onWebError(); }
                                        }

                                        var style = document.createElement('style');
                                        style.innerHTML = `
                                            html, body {
                                                margin: 0 !important;
                                                padding: 0 !important;
                                                overflow: hidden !important;
                                                background: #000 !important;
                                                width: 100% !important;
                                                height: 100% !important;
                                            }
                                            iframe, video {
                                                width: 100% !important;
                                                height: 100% !important;
                                                object-fit: contain !important;
                                                border: none !important;
                                            }
                                            /* Subtitle & Caption Positioning Fix: Clean transparent background with dark outline */
                                            ::cue, video::cue {
                                                background: transparent !important;
                                                background-color: transparent !important;
                                                text-shadow: 0px 1px 4px rgba(0,0,0,0.95), 0px 0px 2px #000000 !important;
                                                color: #FFFFFF !important;
                                                font-size: 17px !important;
                                                font-weight: bold !important;
                                                line-height: 1.3 !important;
                                            }
                                            .jw-text-track-container, .jw-text-track-display,
                                            .vjs-text-track-display, .plyr__captions,
                                            div[class*="caption"], div[class*="subtitle"], div[class*="subtitles"], div[class*="track-display"] {
                                                top: auto !important;
                                                bottom: 7% !important;
                                                transform: none !important;
                                            }
                                            .jw-text-track-cue, .vjs-text-track-cue {
                                                top: auto !important;
                                                bottom: 5px !important;
                                            }
                                            div[class*="ad-banner"], div[id*="ad-container"], div[class*="popup-ad"], a[target="_blank"] {
                                                display: none !important;
                                                pointer-events: none !important;
                                            }
                                        `;
                                        document.head.appendChild(style);
                                    } catch(e) {}
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(cleanStyleJs, null)
                        }
                    }

                    lastLoadedUrl = embedUrl
                    lastReloadToken = reloadToken
                    if (embedUrl.startsWith("magnet:?", ignoreCase = true) || embedUrl.contains("infoHash", ignoreCase = true)) {
                        loadDataWithBaseURL("https://webtorrent.io/", buildWebTorrentHtml(embedUrl), "text/html", "UTF-8", null)
                    } else {
                        val headers = mapOf("Referer" to embedUrl)
                        loadUrl(embedUrl, headers)
                    }
                }
            },
            update = { webView ->
                if (lastLoadedUrl != embedUrl || lastReloadToken != reloadToken) {
                    lastLoadedUrl = embedUrl
                    lastReloadToken = reloadToken
                    isPageLoading = true
                    if (embedUrl.startsWith("magnet:?", ignoreCase = true) || embedUrl.contains("infoHash", ignoreCase = true)) {
                        webView.loadDataWithBaseURL("https://webtorrent.io/", buildWebTorrentHtml(embedUrl), "text/html", "UTF-8", null)
                    } else {
                        val headers = mapOf("Referer" to embedUrl)
                        webView.loadUrl(embedUrl, headers)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        customView?.let { fullScreenView ->
            AndroidView(
                factory = {
                    FrameLayout(it).apply {
                        addView(
                            fullScreenView,
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
    }
}

private fun buildWebTorrentHtml(magnetUrl: String): String {
    val cleanMagnet = magnetUrl.replace("'", "\\'").replace("\"", "\\\"")
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { box-sizing: border-box; }
                body {
                    margin: 0; padding: 0;
                    background-color: #000; color: #fff;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    display: flex; flex-direction: column; justify-content: center; align-items: center;
                    height: 100vh; overflow: hidden;
                }
                #videoContainer { position: relative; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
                video { width: 100%; height: 100%; object-fit: contain; background: #000; }
                .p2p-card {
                    position: absolute; top: 20px; left: 20px;
                    background: rgba(15, 17, 26, 0.92);
                    border: 1px solid rgba(17, 153, 142, 0.5);
                    border-radius: 14px; padding: 14px 18px;
                    z-index: 20; max-width: 90%; width: 400px;
                    box-shadow: 0 8px 32px rgba(0,0,0,0.8);
                    backdrop-filter: blur(12px);
                    transition: opacity 0.5s ease;
                }
                .p2p-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
                .p2p-badge { background: #11998E; color: #000; font-weight: 800; font-size: 11px; padding: 3px 8px; border-radius: 6px; text-transform: uppercase; }
                .p2p-title { font-size: 13px; font-weight: 700; color: #fff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
                .p2p-stats { font-size: 12px; color: #A0A5B5; line-height: 1.6; margin-top: 4px; }
                .p2p-progress-bg { width: 100%; background: rgba(255,255,255,0.12); height: 6px; border-radius: 3px; margin-top: 10px; overflow: hidden; }
                .p2p-progress-bar { width: 0%; height: 100%; background: linear-gradient(90deg, #11998E, #38EF7D); transition: width 0.3s; }
                .fade-out { opacity: 0; pointer-events: none; }
            </style>
            <script src="https://cdn.jsdelivr.net/npm/webtorrent@latest/webtorrent.min.js"></script>
        </head>
        <body>
            <div id="videoContainer">
                <div id="p2pCard" class="p2p-card">
                    <div class="p2p-header">
                        <span class="p2p-badge">P2P Torrent Engine</span>
                        <span id="p2pState" style="font-size: 11px; color: #38EF7D;">Connecting...</span>
                    </div>
                    <div id="p2pTitle" class="p2p-title">Initializing Torrent Swarm...</div>
                    <div id="p2pStats" class="p2p-stats">Searching Trackers & Web Seeds...</div>
                    <div class="p2p-progress-bg"><div id="p2pBar" class="p2p-progress-bar"></div></div>
                </div>
                <video id="torrentVideo" controls autoplay playsinline></video>
            </div>

            <script>
                const magnetUrl = "$cleanMagnet";
                const p2pCard = document.getElementById('p2pCard');
                const p2pTitle = document.getElementById('p2pTitle');
                const p2pStats = document.getElementById('p2pStats');
                const p2pState = document.getElementById('p2pState');
                const p2pBar = document.getElementById('p2pBar');
                const video = document.getElementById('torrentVideo');

                try {
                    const client = new WebTorrent();
                    client.add(magnetUrl, {
                        announce: [
                            'wss://tracker.openwebtorrent.com',
                            'wss://tracker.btorrent.xyz',
                            'wss://tracker.webtorrent.dev',
                            'wss://tracker.files.fm:7073/announce'
                        ]
                    }, function (torrent) {
                        p2pState.innerText = "Swarm Connected";
                        
                        let file = torrent.files.find(f => f.name.endsWith('.mp4') || f.name.endsWith('.mkv') || f.name.endsWith('.webm') || f.name.endsWith('.m4v'));
                        if (!file) {
                            file = torrent.files.reduce((a, b) => (a.length > b.length ? a : b));
                        }

                        p2pTitle.innerText = file.name;
                        p2pStats.innerHTML = "Peers: " + torrent.numPeers + " | Speed: " + (torrent.downloadSpeed / 1024 / 1024).toFixed(2) + " MB/s";

                        file.renderTo(video, { autoplay: true }, function (err, elem) {
                            if (err) {
                                p2pStats.innerText = "P2P Stream Render Notice: " + err.message;
                            } else {
                                p2pState.innerText = "Streaming Active";
                                setTimeout(() => { p2pCard.classList.add('fade-out'); }, 6000);
                            }
                        });

                        torrent.on('download', function (bytes) {
                            const speed = (torrent.downloadSpeed / 1024 / 1024).toFixed(2);
                            const progress = (torrent.progress * 100).toFixed(1);
                            p2pStats.innerHTML = "Buffer: " + progress + "% | Peers: " + torrent.numPeers + " | Speed: " + speed + " MB/s";
                            p2pBar.style.width = progress + "%";
                        });
                    });

                    client.on('error', function (err) {
                        p2pState.innerText = "P2P Gateway Active";
                        p2pStats.innerText = "Connecting via P2P HTTP Gateway...";
                    });
                } catch (e) {
                    p2pStats.innerText = "P2P Client Initializing...";
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
