package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SearchHistoryEntity
import com.example.data.repository.MediaItem
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassSearchBar
import com.example.ui.components.MediaPosterCard
import com.example.ui.theme.AppleTvAccent
import com.example.ui.theme.AppleTvBackground
import com.example.ui.theme.AppleTvTextMuted
import com.example.ui.theme.AppleTvTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<MediaItem>,
    isSearching: Boolean,
    searchHistory: List<SearchHistoryEntity> = emptyList(),
    onSaveSearchQuery: (String) -> Unit = {},
    onDeleteSearchQuery: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    onMediaClick: (mediaType: String, id: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filterCategories = listOf("All", "Movies", "TV Shows", "Action", "Drama", "Sci-Fi", "Comedy")

    val filteredResults = searchResults.filter { item ->
        when (selectedFilter) {
            "Movies" -> item.mediaType == "movie"
            "TV Shows" -> item.mediaType == "tv"
            else -> true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleTvBackground)
            .padding(top = 16.dp)
            .testTag("explore_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Title
            Text(
                text = "Search & Discover",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                GlassSearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = {
                        if (query.isNotBlank()) {
                            onSaveSearchQuery(query)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterCategories) { cat ->
                    GlassChip(
                        text = cat,
                        isSelected = selectedFilter == cat,
                        onClick = { selectedFilter = cat }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppleTvAccent)
                }
            } else if (query.isNotEmpty() && filteredResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No movies or TV shows found for \"$query\"",
                        color = AppleTvTextMuted,
                        fontSize = 14.sp
                    )
                }
            } else if (query.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    if (searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = AppleTvAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Recent Searches",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(onClick = onClearSearchHistory) {
                                    Text("Clear All", color = Color(0xFFFF453A), fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                searchHistory.forEach { history ->
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E2235))
                                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                            .clickable { onQueryChange(history.query) }
                                            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = history.query,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable { onDeleteSearchQuery(history.query) }
                                                .padding(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = AppleTvTextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Search Vision TV+ Catalog",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Type a movie or TV show title to stream immediately",
                                    color = AppleTvTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredResults, key = { "${it.mediaType}_${it.id}" }) { item ->
                        MediaPosterCard(
                            item = item,
                            onClick = {
                                if (query.isNotBlank()) {
                                    onSaveSearchQuery(query)
                                }
                                onMediaClick(item.mediaType, item.id)
                            },
                            width = 110
                        )
                    }
                }
            }
        }
    }
}
