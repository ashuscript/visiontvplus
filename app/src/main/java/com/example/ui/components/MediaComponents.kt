package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.api.CastDto
import com.example.data.api.EpisodeDto
import com.example.data.db.WatchHistoryEntity
import com.example.data.repository.MediaItem
import com.example.data.repository.TvSeasonSummary
import com.example.ui.theme.AppleTvAccent
import com.example.ui.theme.AppleTvAccentSecondary
import com.example.ui.theme.AppleTvBackground
import com.example.ui.theme.AppleTvGlassBorder
import com.example.ui.theme.AppleTvGold
import com.example.ui.theme.AppleTvTextMuted
import com.example.ui.theme.AppleTvTextPrimary
import com.example.ui.theme.AppleTvTextSecondary
import kotlinx.coroutines.delay

@Composable
fun MediaPosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 130
) {
    val context = LocalContext.current
    val imageRequest = remember(item.posterUrl, item.backdropUrl) {
        ImageRequest.Builder(context)
            .data(item.posterUrl ?: item.backdropUrl)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .width(width.dp)
            .testTag("media_card_${item.mediaType}_${item.id}")
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141722))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Glass gradient shadow at bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000)),
                                startY = 100f
                            )
                        )
                )

                // Rating badge
                if (item.voteAverage > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC0D0E15))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AppleTvGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", item.voteAverage),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Media type chip
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (item.mediaType == "movie") AppleTvAccent else AppleTvAccentSecondary)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (item.mediaType == "movie") "MOVIE" else "TV",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    color = AppleTvTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.releaseYear.isNotEmpty()) {
                    Text(
                        text = item.releaseYear,
                        color = AppleTvTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BackdropHeroBanner(
    item: MediaItem,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    isInWatchlist: Boolean = false,
    onWatchlistToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
            .testTag("hero_banner_${item.id}")
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.backdropUrl ?: item.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark Gradient overlay for top & bottom contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x77000000),
                            Color.Transparent,
                            Color(0xCC0D0D0D),
                            Color(0xFF0D0D0D)
                        )
                    )
                )
        )

        // Center Hero Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 28.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = item.title.uppercase(),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-details: Movie • Genre • Year
            Text(
                text = "${if (item.mediaType == "movie") "Movie" else "TV Series"} • ${item.releaseYear.ifEmpty { "2026" }}",
                color = Color(0xDDFFFFFF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // View Details White Pill Button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onDetailsClick() }
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "View Details",
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarouselBanner(
    items: List<MediaItem>,
    onPlayClick: (MediaItem) -> Unit,
    onDetailsClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = 0) { items.size }

    LaunchedEffect(pagerState.isScrollInProgress, items) {
        if (!pagerState.isScrollInProgress && items.size > 1) {
            while (true) {
                delay(5000)
                val nextPage = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 600)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp)
            .testTag("hero_carousel_banner")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            BackdropHeroBanner(
                item = item,
                onPlayClick = { onPlayClick(item) },
                onDetailsClick = { onDetailsClick(item) },
                onWatchlistToggle = { onDetailsClick(item) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Centered Indicator dots • • • — • • at bottom
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    val widthAnimated by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 6.dp,
                        animationSpec = tween(250),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(widthAnimated)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color(0x66FFFFFF))
                    )
                }
            }
        }
    }
}

@Composable
fun CarouselRow(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null,
    testTag: String = "carousel_row"
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onSeeAllClick != null) { onSeeAllClick?.invoke() }
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Right arrow button icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .clickable(enabled = onSeeAllClick != null) { onSeeAllClick?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Show More",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> "${item.mediaType}_${item.id}_$index" },
                contentType = { _, _ -> "media_poster" }
            ) { _, item ->
                MediaPosterCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingCard(
    history: WatchHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDetailsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(230.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141722))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .background(Color(0xFF222536))
            ) {
                val rawImg = history.backdropPath ?: history.posterPath
                val imageUrl = when {
                    rawImg.isNullOrEmpty() -> null
                    rawImg.startsWith("/") -> "https://image.tmdb.org/t/p/w500$rawImg"
                    else -> rawImg
                }

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
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF1E2640), Color(0xFF111422))
                                )
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = history.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC000000))
                            )
                        )
                )

                // More Options button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0x77000000))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1F2232))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Resume Stream", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AppleTvAccent)
                            },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        if (onDetailsClick != null) {
                            DropdownMenuItem(
                                text = { Text("View Details", color = Color.White, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                                },
                                onClick = {
                                    showMenu = false
                                    onDetailsClick()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Remove from Continue Watching", color = Color(0xFFFF453A), fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF453A))
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }

                // Play circle button center overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA000000))
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (history.season != null && history.episode != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "S${history.season} E${history.episode}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Progress bar
            val current = history.currentTimeSec.toFloat()
            val total = if (history.durationSec > 0) history.durationSec.toFloat() else 100f
            val progress = (current / total).coerceIn(0.1f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = AppleTvAccent,
                trackColor = Color(0x33FFFFFF)
            )

            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = history.title,
                    color = AppleTvTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val epTitle = history.episodeTitle
                val subtitleText = when {
                    !epTitle.isNullOrEmpty() -> epTitle
                    history.season != null -> "Season ${history.season} Episode ${history.episode}"
                    else -> "Movie • Resume stream"
                }
                Text(
                    text = subtitleText,
                    color = AppleTvTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun String?.isNull_orEmpty(): Boolean = this == null || this.trim().isEmpty()

@Composable
fun CastAvatarList(
    cast: List<CastDto>,
    modifier: Modifier = Modifier
) {
    if (cast.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Cast & Crew",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(cast, key = { it.id }) { actor ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(72.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(actor.profilePath?.let { "https://image.tmdb.org/t/p/w185$it" })
                            .crossfade(true)
                            .build(),
                        contentDescription = actor.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222636))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = actor.name,
                        color = AppleTvTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val charName = actor.character
                    if (!charName.isNullOrEmpty()) {
                        Text(
                            text = charName,
                            color = AppleTvTextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonEpisodePicker(
    seasons: List<TvSeasonSummary>,
    selectedSeasonNumber: Int,
    onSeasonSelect: (Int) -> Unit,
    episodes: List<EpisodeDto>,
    onEpisodeClick: (EpisodeDto) -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Episodes",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Season selector dropdown
            if (seasons.isNotEmpty()) {
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x442B3045))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Season $selectedSeasonNumber ▾",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF1B1E2B))
                    ) {
                        seasons.forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = s.name,
                                        color = if (s.seasonNumber == selectedSeasonNumber) AppleTvAccent else Color.White
                                    )
                                },
                                onClick = {
                                    onSeasonSelect(s.seasonNumber)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (episodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading episodes...",
                    color = AppleTvTextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                episodes.forEach { ep ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x331C202E))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { onEpisodeClick(ep) }
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Still image
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F1118))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(ep.stillPath?.let { "https://image.tmdb.org/t/p/w300$it" })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = ep.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Play icon
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xAA000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${ep.episodeNumber}. ${ep.name ?: "Episode ${ep.episodeNumber}"}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (ep.runtime != null) {
                                        Text(
                                            text = "${ep.runtime}m",
                                            color = AppleTvTextMuted,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }
                                }

                                val overviewText = ep.overview
                                if (!overviewText.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = overviewText,
                                        color = AppleTvTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 15.sp
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
