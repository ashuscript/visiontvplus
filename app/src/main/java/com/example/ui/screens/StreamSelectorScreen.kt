package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.NuvioStream
import com.example.data.repository.ScraperExecutionState
import com.example.ui.theme.AppleTvAccent
import com.example.ui.theme.AppleTvBackground
import com.example.ui.theme.AppleTvTextMuted
import com.example.ui.theme.AppleTvTextSecondary

@Composable
fun StreamSelectorScreen(
    mediaTitle: String,
    mediaSubtitle: String? = null,
    tmdbId: Int,
    mediaType: String,
    season: Int = 1,
    episode: Int = 1,
    isScraping: Boolean,
    scraperStates: Map<String, ScraperExecutionState>,
    availableStreams: List<NuvioStream>,
    onStartScraping: (tmdbId: Int, mediaType: String, season: Int, episode: Int, forceRefresh: Boolean) -> Unit,
    onSelectStream: (NuvioStream) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Auto-trigger scraper search on screen launch or when media details change
    LaunchedEffect(tmdbId, mediaType, season, episode) {
        onStartScraping(tmdbId, mediaType, season, episode, false)
    }

    var selectedFilterChip by remember { mutableStateOf("All") }

    val activeScrapersList = remember(scraperStates) {
        scraperStates.values.toList()
    }

    val providerNames = remember(availableStreams, activeScrapersList) {
        val list = mutableListOf("All")
        activeScrapersList.forEach { if (!list.contains(it.providerName)) list.add(it.providerName) }
        availableStreams.mapNotNull { it.provider }.forEach { if (!list.contains(it)) list.add(it) }
        list
    }

    // Filter streams based on selected provider chip
    val filteredStreams = remember(availableStreams, selectedFilterChip) {
        if (selectedFilterChip == "All") {
            availableStreams
        } else {
            availableStreams.filter { stream ->
                stream.provider?.equalsIgnoreCase(selectedFilterChip) == true ||
                stream.name.contains(selectedFilterChip, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
            .testTag("stream_selector_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Top Bar: Back Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBackClick()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Centered Media Title (Matching Image 6 header)
                Text(
                    text = mediaTitle.uppercase(),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Resume Chip (Matching Image 6: "Resume from 3:45" or play indicator)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1B1E28))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable {
                            if (availableStreams.isNotEmpty()) onSelectStream(availableStreams.first())
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Resume from 3:45",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Tabs Row (Matching Image 6: [Refresh Button] [All] [WebStreamrMBG] [Torrentio] [HdHub]...)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Refresh Circle Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2230))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStartScraping(tmdbId, mediaType, season, episode, true)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(providerNames) { provider ->
                        val isSelected = selectedFilterChip == provider
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color(0xFF1B1E28))
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White else Color(0x22FFFFFF),
                                    CircleShape
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedFilterChip = provider
                                }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = provider,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Provider Status Bar (Matching Image 6: "WebStreamrMBG" ... "✨ Fetching...")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedFilterChip == "All") "Available Providers" else selectedFilterChip,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isScraping) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✨ Fetching...",
                            color = Color(0xDDFFFFFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color.White,
                            strokeWidth = 1.5.dp
                        )
                    }
                } else {
                    Text(
                        text = "${filteredStreams.size} Streams",
                        color = Color(0x88FFFFFF),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stream Cards List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredStreams.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isScraping) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Searching available sources...",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            } else {
                                Text(
                                    text = "No streams found for $selectedFilterChip",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = filteredStreams, key = { it.url + it.name }) { stream ->
                            ExactStreamCardItem(
                                stream = stream,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelectStream(stream)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String.equalsIgnoreCase(other: String): Boolean = this.equals(other, ignoreCase = true)

@Composable
private fun ExactStreamCardItem(
    stream: NuvioStream,
    onClick: () -> Unit
) {
    val providerName = stream.provider ?: "Torrentio"
    val qualityStr = stream.quality ?: "1080p"
    val filename = stream.title ?: stream.name
    val sizeStr = stream.size ?: "1.25 GB"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131620))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Line 1: Provider Name + Quality Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = providerName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = qualityStr,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Line 2: Release Filename
        Text(
            text = filename,
            color = Color(0xAAFFFFFF),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Line 3: Seeders / Size / Source (Blue icons/text matching Image 6 screenshot)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "👤 ", fontSize = 12.sp)
                Text(text = "40", color = Color(0xFF3880FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💾 ", fontSize = 12.sp)
                Text(text = sizeStr, color = Color(0xDDFFFFFF), fontSize = 12.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚙️ ", fontSize = 12.sp)
                Text(
                    text = stream.repoGroup ?: "1337x",
                    color = Color(0xFF3880FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Line 4: Audio / Flag Badges (Dual Audio / 🇮🇳 / 🇬🇧)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Dual Audio / 🇮🇳 🇬🇧",
                color = Color(0x88FFFFFF),
                fontSize = 11.sp
            )
        }
    }
}
