package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AppDatabase
import com.example.data.repository.MediaItem
import com.example.data.repository.MediaRepository
import com.example.ui.components.MediaPosterCard
import com.example.ui.theme.AppleTvAccent
import com.example.ui.theme.AppleTvBackground
import com.example.ui.theme.AppleTvSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowMoreScreen(
    categoryType: String,
    title: String,
    onMediaClick: (mediaType: String, id: Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) { MediaRepository(AppDatabase.getDatabase(context).appDao()) }

    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var currentPage by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var initialLoading by remember { mutableStateOf(true) }
    var canLoadMore by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val gridState = rememberLazyGridState()

    val addonRepo = remember { com.example.data.repository.AddonCatalogRepository() }
    val providerRepo = remember(context) { com.example.data.repository.ProviderRepository(context) }

    // Load page data
    LaunchedEffect(categoryType, currentPage) {
        if (!canLoadMore && currentPage > 1) return@LaunchedEffect
        isLoading = true
        errorMessage = null

        if (categoryType.startsWith("addon_")) {
            val addonResult = withContext(Dispatchers.IO) {
                try {
                    val activeProviders = providerRepo.providers.value
                    val rows = addonRepo.fetchAddonCatalogs(activeProviders)
                    val matchedRow = rows.find { row ->
                        "addon_${row.providerId}_${row.catalogTitle}" == categoryType || title.contains(row.catalogTitle, ignoreCase = true)
                    }
                    Result.success(matchedRow?.items ?: rows.flatMap { it.items }.distinctBy { "${it.mediaType}_${it.id}" })
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            addonResult.onSuccess { newItems ->
                items = newItems
                canLoadMore = false
                initialLoading = false
                isLoading = false
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Failed to load addon catalog"
                initialLoading = false
                isLoading = false
            }
            return@LaunchedEffect
        }

        val result = withContext(Dispatchers.IO) {
            when (categoryType) {
                "trending" -> repository.getTrendingAll(currentPage)
                "popular_movies" -> repository.getPopularMovies(currentPage)
                "popular_tv" -> repository.getPopularTvShows(currentPage)
                "top_rated_movies" -> repository.getTopRatedMovies(currentPage)
                "top_rated_tv" -> repository.getTopRatedTvShows(currentPage)
                else -> repository.getTrendingAll(currentPage)
            }
        }

        result.onSuccess { newItems ->
            if (newItems.isEmpty()) {
                canLoadMore = false
            } else {
                val existingKeys = items.map { "${it.mediaType}_${it.id}" }.toSet()
                val fresh = newItems.filter { "${it.mediaType}_${it.id}" !in existingKeys }
                items = items + fresh
                if (newItems.size < 10) {
                    canLoadMore = false
                }
            }
            initialLoading = false
            isLoading = false
        }.onFailure { err ->
            if (currentPage == 1) {
                errorMessage = err.localizedMessage ?: "Failed to load media items"
            } else {
                Toast.makeText(context, "Could not load page $currentPage", Toast.LENGTH_SHORT).show()
            }
            initialLoading = false
            isLoading = false
        }
    }

    // Infinite scroll trigger
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisibleItem != null && lastVisibleItem.index >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading && canLoadMore && !initialLoading && errorMessage == null) {
            currentPage += 1
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleTvBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (items.isNotEmpty()) {
                            Text(
                                text = "${items.size} titles loaded • Page $currentPage",
                                color = Color(0x99FFFFFF),
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0F18)
                )
            )

            if (initialLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppleTvAccent)
                }
            } else if (errorMessage != null && items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Error loading content",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                initialLoading = true
                                currentPage = 1
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppleTvAccent)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 105.dp),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = items,
                        key = { "${it.mediaType}_${it.id}" }
                    ) { media ->
                        MediaPosterCard(
                            item = media,
                            onClick = { onMediaClick(media.mediaType, media.id) }
                        )
                    }

                    if (isLoading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = AppleTvAccent,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Loading more titles...",
                                        color = Color(0xAAFFFFFF),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else if (!canLoadMore && items.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "End of content • All titles loaded",
                                    color = Color(0x66FFFFFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
