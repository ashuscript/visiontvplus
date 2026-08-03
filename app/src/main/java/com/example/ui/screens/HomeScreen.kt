package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.WatchHistoryEntity
import com.example.data.repository.MediaItem
import com.example.ui.components.HeroCarouselBanner
import com.example.ui.components.CarouselRow
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.GlassButton
import com.example.ui.theme.AppleTvAccent
import com.example.ui.theme.AppleTvBackground
import com.example.ui.theme.AppleTvTextSecondary
import com.example.data.repository.AddonCatalogRow
import com.example.ui.viewmodel.HomeUiState

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    watchHistory: List<WatchHistoryEntity>,
    addonCatalogs: List<AddonCatalogRow> = emptyList(),
    onMediaClick: (mediaType: String, id: Int) -> Unit,
    onPlayClick: (mediaType: String, id: Int, season: Int, episode: Int) -> Unit,
    onPlayHistoryClick: (WatchHistoryEntity) -> Unit,
    onRemoveWatchHistory: (mediaId: String) -> Unit,
    onSeeAllClick: (categoryType: String, title: String) -> Unit = { _, _ -> },
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleTvBackground)
            .testTag("home_screen")
    ) {
        when (uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppleTvAccent)
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Unable to load Vision TV+ Catalog",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            color = AppleTvTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassButton(
                            text = "Retry",
                            onClick = onRetry,
                            isPrimary = true
                        )
                    }
                }
            }

            is HomeUiState.Success -> {
                val heroCarouselItems = remember(uiState.heroItem, uiState.trending) {
                    val list = mutableListOf<MediaItem>()
                    uiState.heroItem?.let { list.add(it) }
                    uiState.trending.forEach { item ->
                        if (list.none { existing -> existing.id == item.id && existing.mediaType == item.mediaType }) {
                            list.add(item)
                        }
                    }
                    list.take(6)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Top App Bar with Vision TV+ Logo
                    item(key = "top_app_bar", contentType = "header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_vision_tv_logo),
                                    contentDescription = "Vision TV+ Logo",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Vision TV+",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "4K",
                                    color = AppleTvAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Hero Carousel Banner
                    if (heroCarouselItems.isNotEmpty()) {
                        item(key = "hero_carousel_banner", contentType = "carousel_banner") {
                            HeroCarouselBanner(
                                items = heroCarouselItems,
                                onPlayClick = { media ->
                                    onPlayClick(media.mediaType, media.id, 1, 1)
                                },
                                onDetailsClick = { media ->
                                    onMediaClick(media.mediaType, media.id)
                                }
                            )
                        }
                    }

                    // Continue Watching
                    if (watchHistory.isNotEmpty()) {
                        item(key = "continue_watching_section", contentType = "continue_watching") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Continue Watching",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = watchHistory,
                                        key = { it.mediaId },
                                        contentType = { "continue_watching_card" }
                                    ) { history ->
                                        ContinueWatchingCard(
                                            history = history,
                                            onClick = { onPlayHistoryClick(history) },
                                            onDelete = { onRemoveWatchHistory(history.mediaId) },
                                            onDetailsClick = { onMediaClick(history.mediaType, history.tmdbId) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Trending Now
                    item(key = "trending_row", contentType = "media_row") {
                        CarouselRow(
                            title = "Trending Now",
                            items = uiState.trending,
                            onItemClick = { item -> onMediaClick(item.mediaType, item.id) },
                            onSeeAllClick = { onSeeAllClick("trending", "Trending Now") },
                            testTag = "row_trending"
                        )
                    }

                    // Popular Movies
                    item(key = "popular_movies_row", contentType = "media_row") {
                        CarouselRow(
                            title = "Popular Movies",
                            items = uiState.popularMovies,
                            onItemClick = { item -> onMediaClick("movie", item.id) },
                            onSeeAllClick = { onSeeAllClick("popular_movies", "Popular Movies") },
                            testTag = "row_popular_movies"
                        )
                    }

                    // Popular TV Shows
                    item(key = "popular_tv_row", contentType = "media_row") {
                        CarouselRow(
                            title = "Popular TV Series",
                            items = uiState.popularTv,
                            onItemClick = { item -> onMediaClick("tv", item.id) },
                            onSeeAllClick = { onSeeAllClick("popular_tv", "Popular TV Series") },
                            testTag = "row_popular_tv"
                        )
                    }

                    // Top Rated Movies
                    item(key = "top_rated_movies_row", contentType = "media_row") {
                        CarouselRow(
                            title = "Top Rated Movies",
                            items = uiState.topRatedMovies,
                            onItemClick = { item -> onMediaClick("movie", item.id) },
                            onSeeAllClick = { onSeeAllClick("top_rated_movies", "Top Rated Movies") },
                            testTag = "row_top_movies"
                        )
                    }

                    // Top Rated TV
                    item(key = "top_rated_tv_row", contentType = "media_row") {
                        CarouselRow(
                            title = "Top Rated Series",
                            items = uiState.topRatedTv,
                            onItemClick = { item -> onMediaClick("tv", item.id) },
                            onSeeAllClick = { onSeeAllClick("top_rated_tv", "Top Rated Series") },
                            testTag = "row_top_tv"
                        )
                    }

                    // Dynamic Installed Addon & Repo Catalogs
                    itemsIndexed(
                        items = addonCatalogs,
                        key = { index, row -> "${row.providerId}_${row.catalogTitle}_$index" }
                    ) { _, row ->
                        CarouselRow(
                            title = "${row.providerName} - ${row.catalogTitle}",
                            items = row.items,
                            onItemClick = { item -> onMediaClick(item.mediaType, item.id) },
                            onSeeAllClick = { onSeeAllClick("addon_${row.providerId}_${row.catalogTitle}", "${row.providerName} - ${row.catalogTitle}") },
                            testTag = "row_addon_${row.providerId}"
                        )
                    }
                }
            }
        }
    }
}
