package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.NuvioStream
import com.example.data.repository.ScraperExecutionState
import com.example.ui.theme.AppleTvAccent
import com.example.ui.theme.AppleTvTextSecondary

@Composable
fun NuvioStreamSelectorDialog(
    mediaTitle: String,
    mediaSubtitle: String? = null,
    isScraping: Boolean,
    scraperStates: Map<String, ScraperExecutionState>,
    streams: List<NuvioStream>,
    onStreamSelected: (NuvioStream) -> Unit,
    onRetryScraping: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("All") }
    var selectedFilter by remember { mutableStateOf("All") }

    val repoTabs = listOf(
        "All",
        "Direct Cinema",
        "Abinathan Repo",
        "Phisher Providers",
        "Mooncrown TR",
        "Pengu UK",
        "Yoruix Providers",
        "All-in-One Nuvio"
    )

    val visibleRepoTabs = remember(streams, isScraping) {
        repoTabs.filter { tab ->
            if (tab == "All") true
            else {
                val tabCount = streams.count { s ->
                    if (tab == "Direct Cinema") s.repoGroup.isNullOrEmpty() || s.repoGroup.equals("Direct Cinema", ignoreCase = true)
                    else s.repoGroup?.equals(tab, ignoreCase = true) == true
                }
                tabCount > 0 || isScraping
            }
        }
    }

    val visibleScrapers = remember(scraperStates, streams) {
        scraperStates.values.filter { state ->
            val providerStreamCount = streams.count { s ->
                s.provider?.contains(state.providerName, ignoreCase = true) == true ||
                s.name.contains(state.providerName, ignoreCase = true) ||
                state.providerName.contains(s.provider ?: "___", ignoreCase = true) ||
                s.provider?.contains(state.providerId, ignoreCase = true) == true
            }
            providerStreamCount > 0 || state.isLoading
        }.toList()
    }

    val filteredStreams = remember(streams, selectedTab, selectedFilter) {
        streams.filter { s ->
            val matchesTab = when (selectedTab) {
                "All" -> true
                "Direct Cinema" -> s.repoGroup.isNullOrEmpty() || s.repoGroup.equals("Direct Cinema", ignoreCase = true)
                else -> s.repoGroup?.equals(selectedTab, ignoreCase = true) == true
            }

            val matchesFilter = if (selectedFilter == "All") {
                true
            } else {
                s.provider?.contains(selectedFilter, ignoreCase = true) == true ||
                s.name.contains(selectedFilter, ignoreCase = true) ||
                selectedFilter.contains(s.provider ?: "___", ignoreCase = true)
            }

            matchesTab && matchesFilter
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFA090D16),
                            Color(0xEE0B111D)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x33151E30))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x3BFFFFFF), Color(0x0CFFFFFF))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AppleTvAccent, Color(0xFF38EF7D)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Nuvio Streams",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Nuvio Direct Streams",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x3311998E))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${streams.size} Ready",
                                        color = AppleTvAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (!mediaSubtitle.isNullOrEmpty()) {
                                Text(
                                    text = mediaSubtitle,
                                    color = AppleTvTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onRetryScraping) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescrape",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Plugin Repository Tabs (Abinathan, Phisher, Mooncrown, Pengu, Yoruix, etc.)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(visibleRepoTabs) { tab ->
                        val isSelected = selectedTab == tab
                        val tabCount = if (tab == "All") streams.size else streams.count { s ->
                            if (tab == "Direct Cinema") s.repoGroup.isNullOrEmpty() || s.repoGroup.equals("Direct Cinema", ignoreCase = true)
                            else s.repoGroup?.equals(tab, ignoreCase = true) == true
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AppleTvAccent else Color(0x22FFFFFF))
                                .border(1.dp, if (isSelected) AppleTvAccent else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedTab = tab
                                    selectedFilter = "All"
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$tab ($tabCount)",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Provider Status Bar & Filter Chips
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Streams (${visibleScrapers.size} plugins)",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (selectedFilter != "All") {
                            Text(
                                text = "Clear Filter (Showing $selectedFilter)",
                                color = AppleTvAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { selectedFilter = "All" }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // "All" filter chip
                        item {
                            val isAllSelected = selectedFilter == "All"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isAllSelected) AppleTvAccent else Color(0x22FFFFFF))
                                    .border(1.dp, if (isAllSelected) AppleTvAccent else Color(0x44FFFFFF), RoundedCornerShape(10.dp))
                                    .clickable { selectedFilter = "All" }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "All (${streams.size})",
                                    color = if (isAllSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Plugin provider chips (only those returning streams or currently loading)
                        items(visibleScrapers, key = { it.providerId }) { state ->
                            val isSelected = selectedFilter.equals(state.providerName, ignoreCase = true) ||
                                            selectedFilter.equals(state.providerId, ignoreCase = true)
                            val providerStreamCount = streams.count { s ->
                                s.provider?.contains(state.providerName, ignoreCase = true) == true ||
                                s.name.contains(state.providerName, ignoreCase = true) ||
                                state.providerName.contains(s.provider ?: "___", ignoreCase = true) ||
                                s.provider?.contains(state.providerId, ignoreCase = true) == true
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> AppleTvAccent
                                            state.isLoading -> Color(0x22FFFFFF)
                                            providerStreamCount > 0 -> Color(0x3311998E)
                                            else -> Color(0x22FF5252)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isSelected -> AppleTvAccent
                                            state.isLoading -> Color(0x44FFFFFF)
                                            providerStreamCount > 0 -> AppleTvAccent
                                            else -> Color(0x44FF5252)
                                        },
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedFilter = if (isSelected) "All" else state.providerName
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(10.dp),
                                            color = if (isSelected) Color.Black else Color.White,
                                            strokeWidth = 1.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    } else if (providerStreamCount > 0) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.Black else AppleTvAccent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "${state.providerName}${if (providerStreamCount > 0) " ($providerStreamCount)" else ""}",
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Streams Scrollable Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (filteredStreams.isEmpty() && selectedFilter != "All") {
                        val activeState = scraperStates.values.firstOrNull {
                            it.providerName.equals(selectedFilter, ignoreCase = true) ||
                            it.providerId.equals(selectedFilter, ignoreCase = true)
                        }

                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (activeState?.isLoading == true) {
                                CircularProgressIndicator(
                                    color = AppleTvAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "$selectedFilter is currently searching for streams...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "No direct streams returned by $selectedFilter for this title.",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AppleTvAccent)
                                            .clickable { selectedFilter = "All" }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Show All Streams (${streams.size})",
                                            color = Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else if (streams.isEmpty() && isScraping) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = AppleTvAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Scraping direct streams from Nuvio plugins...",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    } else if (streams.isEmpty() && !isScraping) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No streams found from current active plugins.",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppleTvAccent)
                                    .clickable { onRetryScraping() }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Rescrape Direct Streams",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items = filteredStreams, key = { it.url }) { stream ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onStreamSelected(stream) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.horizontalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF)))
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x3311998E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = AppleTvAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = stream.name,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0x4411998E))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = stream.quality ?: "HD",
                                                        color = AppleTvAccent,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            if (!stream.title.isNullOrEmpty() && stream.title != stream.name) {
                                                Text(
                                                    text = stream.title,
                                                    color = AppleTvTextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Provider: ${stream.provider ?: "Nuvio Plugin"}",
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp
                                                )
                                                if (stream.subtitles.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Subtitles,
                                                        contentDescription = null,
                                                        tint = AppleTvAccent,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "${stream.subtitles.size} Subtitles",
                                                        color = AppleTvAccent,
                                                        fontSize = 10.sp
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
            }
        }
    }
}
