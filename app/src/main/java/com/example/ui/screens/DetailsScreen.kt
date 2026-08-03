package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.components.CarouselRow
import com.example.ui.components.CastAvatarList
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassChip
import com.example.ui.components.SeasonEpisodePicker
import com.example.ui.theme.AppleTvAccent
import com.example.ui.theme.AppleTvBackground
import com.example.ui.theme.AppleTvGold
import com.example.ui.theme.AppleTvTextMuted
import com.example.ui.theme.AppleTvTextPrimary
import com.example.ui.theme.AppleTvTextSecondary
import com.example.ui.viewmodel.DetailUiState

@Composable
fun DetailsScreen(
    mediaType: String,
    id: Int,
    uiState: DetailUiState,
    isInWatchlist: Boolean = false,
    onLoadDetails: (String, Int) -> Unit,
    onSelectSeason: (seriesId: Int, seasonNumber: Int) -> Unit,
    onPlayClick: (mediaType: String, id: Int, season: Int, episode: Int) -> Unit,
    onToggleWatchlist: (com.example.data.repository.MediaDetail) -> Unit,
    onBackClick: () -> Unit,
    onMediaClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(mediaType, id) {
        onLoadDetails(mediaType, id)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleTvBackground)
            .testTag("details_screen")
    ) {
        when (uiState) {
            is DetailUiState.Idle, is DetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppleTvAccent)
                }
            }

            is DetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error loading media details",
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
                            onClick = { onLoadDetails(mediaType, id) }
                        )
                    }
                }
            }

            is DetailUiState.Success -> {
                val detail = uiState.detail

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Hero Backdrop
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(detail.backdropUrl ?: detail.posterUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = detail.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Gradients
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xBB000000),
                                                Color.Transparent,
                                                AppleTvBackground.copy(alpha = 0.9f),
                                                AppleTvBackground
                                            )
                                        )
                                    )
                            )

                            // Top Back Bar
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .padding(start = 16.dp, top = 24.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xAA000000))
                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            // Banner info
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(20.dp)
                            ) {
                                if (detail.tagline.isNotEmpty()) {
                                    Text(
                                        text = detail.tagline,
                                        color = AppleTvAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Text(
                                    text = detail.title,
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Chips row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlassChip(text = if (detail.mediaType == "movie") "Movie" else "TV Series")
                                    if (detail.releaseYear.isNotEmpty()) {
                                        GlassChip(text = detail.releaseYear)
                                    }
                                    GlassChip(text = detail.durationOrSeasons)
                                    GlassChip(text = "4K HDR")
                                    if (detail.voteAverage > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = AppleTvGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = String.format("%.1f", detail.voteAverage),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Action Buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GlassButton(
                                        text = "Play Now",
                                        icon = Icons.Default.PlayArrow,
                                        onClick = {
                                            val seasonNum = if (detail.seasons.isNotEmpty()) uiState.selectedSeason else 1
                                            val epNum = if (uiState.episodes.isNotEmpty()) uiState.episodes.first().episodeNumber else 1
                                            onPlayClick(detail.mediaType, detail.id, seasonNum, epNum)
                                        },
                                        isPrimary = true,
                                        testTag = "detail_play_button"
                                    )

                                    GlassButton(
                                        text = if (isInWatchlist) "In Watchlist" else "Watchlist",
                                        icon = if (isInWatchlist) Icons.Default.Check else Icons.Default.Add,
                                        onClick = { onToggleWatchlist(detail) },
                                        isPrimary = false,
                                        testTag = "detail_watchlist_button"
                                    )
                                }
                            }
                        }
                    }

                    // Genres
                    if (detail.genres.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                detail.genres.forEach { genre ->
                                    GlassChip(text = genre.name)
                                }
                            }
                        }
                    }

                    // Synopsis Overview
                    if (detail.overview.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Synopsis",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = detail.overview,
                                    color = AppleTvTextSecondary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // Season & Episode Selector (TV Series)
                    if (detail.mediaType == "tv" && detail.seasons.isNotEmpty()) {
                        item {
                            SeasonEpisodePicker(
                                seasons = detail.seasons,
                                selectedSeasonNumber = uiState.selectedSeason,
                                onSeasonSelect = { seasonNum ->
                                    onSelectSeason(detail.id, seasonNum)
                                },
                                episodes = uiState.episodes,
                                onEpisodeClick = { ep ->
                                    onPlayClick(
                                        detail.mediaType,
                                        detail.id,
                                        ep.seasonNumber,
                                        ep.episodeNumber
                                    )
                                }
                            )
                        }
                    }

                    // Cast & Crew
                    item {
                        CastAvatarList(cast = detail.cast)
                    }

                    // Related Recommendations
                    if (detail.recommendations.isNotEmpty()) {
                        item {
                            CarouselRow(
                                title = "You Might Also Like",
                                items = detail.recommendations,
                                onItemClick = { item -> onMediaClick(item.mediaType, item.id) },
                                testTag = "detail_recommendations_carousel"
                            )
                        }
                    }
                }
            }
        }
    }
}
