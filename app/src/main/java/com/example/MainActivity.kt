package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.GlassNavigationBar
import com.example.ui.components.NavItem
import com.example.ui.screens.DetailsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.ShowMoreScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.DetailUiState
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                VisionTvApp()
            }
        }
    }
}

@Composable
fun VisionTvApp(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val addonCatalogs by viewModel.addonCatalogs.collectAsStateWithLifecycle()

    var isNavCollapsed by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -25f) {
                    if (!isNavCollapsed) isNavCollapsed = true
                } else if (delta > 25f) {
                    if (isNavCollapsed) isNavCollapsed = false
                }
                return Offset.Zero
            }
        }
    }

    val navItems = listOf(
        NavItem(route = "home", title = "Home", icon = Icons.Default.Home, testTag = "nav_home"),
        NavItem(route = "explore", title = "Search", icon = Icons.Default.Search, testTag = "nav_explore"),
        NavItem(route = "library", title = "Library", icon = Icons.Default.VideoLibrary, testTag = "nav_library"),
        NavItem(route = "settings", title = "Profile", icon = Icons.Default.Person, testTag = "nav_settings")
    )

    val showBottomBar = currentRoute in listOf("home", "explore", "library", "plugins", "settings")

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize()
            ) {
                // Home Screen
                composable("home") {
                    HomeScreen(
                        uiState = homeState,
                        watchHistory = watchHistory,
                        addonCatalogs = addonCatalogs,
                        onMediaClick = { mediaType, id ->
                            navController.navigate("details/$mediaType/$id")
                        },
                        onPlayClick = { mediaType, id, season, episode ->
                            navController.navigate("stream_selector/$mediaType/$id/$season/$episode")
                        },
                        onPlayHistoryClick = { history ->
                            if (!history.streamUrl.isNullOrEmpty() && (history.streamUrl.startsWith("http://") || history.streamUrl.startsWith("https://"))) {
                                val encodedUrl = URLEncoder.encode(history.streamUrl, StandardCharsets.UTF_8.toString())
                                val encodedTitle = URLEncoder.encode(history.title, StandardCharsets.UTF_8.toString())
                                val encodedPoster = URLEncoder.encode(history.posterPath ?: "", StandardCharsets.UTF_8.toString())
                                val encodedBackdrop = URLEncoder.encode(history.backdropPath ?: "", StandardCharsets.UTF_8.toString())
                                val seasonVal = history.season ?: 1
                                val episodeVal = history.episode ?: 1
                                navController.navigate("direct_player/$encodedUrl/$encodedTitle?mediaType=${history.mediaType}&tmdbId=${history.tmdbId}&season=$seasonVal&episode=$episodeVal&posterPath=$encodedPoster&backdropPath=$encodedBackdrop")
                            } else {
                                navController.navigate("stream_selector/${history.mediaType}/${history.tmdbId}/${history.season ?: 1}/${history.episode ?: 1}")
                            }
                        },
                        onRemoveWatchHistory = { mediaId ->
                            viewModel.deleteWatchHistory(mediaId)
                        },
                        onSeeAllClick = { categoryType, title ->
                            val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
                            navController.navigate("show_more/$categoryType/$encodedTitle")
                        },
                        onRetry = { viewModel.loadHomeData() }
                    )
                }

                // Explore Screen
                composable("explore") {
                    ExploreScreen(
                        query = searchQuery,
                        onQueryChange = { q -> viewModel.onSearchQueryChange(q) },
                        searchResults = searchResults,
                        isSearching = isSearching,
                        searchHistory = searchHistory,
                        onSaveSearchQuery = { q -> viewModel.saveSearchQuery(q) },
                        onDeleteSearchQuery = { q -> viewModel.deleteSearchQuery(q) },
                        onClearSearchHistory = { viewModel.clearSearchHistory() },
                        onMediaClick = { mediaType, id ->
                            navController.navigate("details/$mediaType/$id")
                        }
                    )
                }

                // Library Screen
                composable("library") {
                    LibraryScreen(
                        watchHistory = watchHistory,
                        watchlist = watchlist,
                        onMediaClick = { mediaType, id ->
                            navController.navigate("details/$mediaType/$id")
                        },
                        onPlayHistoryClick = { history ->
                            if (!history.streamUrl.isNullOrEmpty() && (history.streamUrl.startsWith("http://") || history.streamUrl.startsWith("https://"))) {
                                val encodedUrl = URLEncoder.encode(history.streamUrl, StandardCharsets.UTF_8.toString())
                                val encodedTitle = URLEncoder.encode(history.title, StandardCharsets.UTF_8.toString())
                                val encodedPoster = URLEncoder.encode(history.posterPath ?: "", StandardCharsets.UTF_8.toString())
                                val encodedBackdrop = URLEncoder.encode(history.backdropPath ?: "", StandardCharsets.UTF_8.toString())
                                val seasonVal = history.season ?: 1
                                val episodeVal = history.episode ?: 1
                                navController.navigate("direct_player/$encodedUrl/$encodedTitle?mediaType=${history.mediaType}&tmdbId=${history.tmdbId}&season=$seasonVal&episode=$episodeVal&posterPath=$encodedPoster&backdropPath=$encodedBackdrop")
                            } else {
                                navController.navigate("stream_selector/${history.mediaType}/${history.tmdbId}/${history.season ?: 1}/${history.episode ?: 1}")
                            }
                        },
                        onRemoveWatchHistory = { mediaId ->
                            viewModel.deleteWatchHistory(mediaId)
                        }
                    )
                }

                // Show More / Category Screen
                composable(
                    route = "show_more/{categoryType}/{title}",
                    arguments = listOf(
                        navArgument("categoryType") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType }
                    )
                ) { backStack ->
                    val categoryType = backStack.arguments?.getString("categoryType") ?: "trending"
                    val rawTitle = backStack.arguments?.getString("title") ?: "Titles"
                    val decodedTitle = try { URLDecoder.decode(rawTitle, StandardCharsets.UTF_8.toString()) } catch (e: Exception) { rawTitle }

                    ShowMoreScreen(
                        categoryType = categoryType,
                        title = decodedTitle,
                        onMediaClick = { mediaType, id ->
                            navController.navigate("details/$mediaType/$id")
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Plugins Screen
                composable("plugins") {
                    val allPlugins by viewModel.allPlugins.collectAsStateWithLifecycle()
                    val isLoadingPlugins by viewModel.isLoadingPlugins.collectAsStateWithLifecycle()

                    com.example.ui.screens.PluginsScreen(
                        providers = providers,
                        plugins = allPlugins,
                        isLoadingPlugins = isLoadingPlugins,
                        onBackClick = null,
                        onAddProvider = { url, category, name ->
                            viewModel.addProvider(url, category, name) {
                                viewModel.loadAllPlugins()
                            }
                        },
                        onToggleProvider = { providerId, isEnabled ->
                            viewModel.toggleProvider(providerId, isEnabled)
                            viewModel.loadAllPlugins()
                        },
                        onRemoveProvider = { providerId ->
                            viewModel.removeProvider(providerId)
                            viewModel.loadAllPlugins()
                        },
                        onTestProvider = { providerId ->
                            viewModel.testProvider(providerId)
                        },
                        onAddRepo = { url, name ->
                            viewModel.addProvider(url, com.example.data.repository.ProviderCategory.NUVIO_PLUGIN, name) {
                                viewModel.loadAllPlugins()
                            }
                        },
                        onDeleteRepo = { repoId ->
                            viewModel.removeProvider(repoId)
                            viewModel.loadAllPlugins()
                        },
                        onRefreshRepo = { _ ->
                            viewModel.loadAllPlugins()
                        },
                        onTogglePlugin = { pluginId, isEnabled ->
                            viewModel.togglePluginEnabled(pluginId, isEnabled)
                        },
                        onTogglePinPlugin = { pluginId, isPinned ->
                            viewModel.togglePluginPinned(pluginId, isPinned)
                        }
                    )
                }

                // Settings Screen
                composable("settings") {
                    SettingsScreen(
                        providers = providers,
                        onTestProvider = { id -> viewModel.testProvider(id) },
                        onAddProvider = { url, category, name, onResult ->
                            viewModel.addProvider(url, category, name, onResult)
                        },
                        onRemoveProvider = { id -> viewModel.removeProvider(id) },
                        onToggleProvider = { id, enabled -> viewModel.toggleProvider(id, enabled) },
                        onOpenPluginsScreen = { navController.navigate("plugins") }
                    )
                }

                // Details Screen
                composable(
                    route = "details/{mediaType}/{id}",
                    arguments = listOf(
                        navArgument("mediaType") { type = NavType.StringType },
                        navArgument("id") { type = NavType.IntType }
                    )
                ) { backStack ->
                    val mediaType = backStack.arguments?.getString("mediaType") ?: "movie"
                    val id = backStack.arguments?.getInt("id") ?: 0
                    val isInWatchlist by viewModel.isInWatchlist("${mediaType}_${id}").collectAsStateWithLifecycle(initialValue = false)

                    DetailsScreen(
                        mediaType = mediaType,
                        id = id,
                        uiState = detailState,
                        isInWatchlist = isInWatchlist,
                        onLoadDetails = { type, mId -> viewModel.loadDetails(type, mId) },
                        onSelectSeason = { seriesId, seasonNum ->
                            viewModel.selectSeason(seriesId, seasonNum)
                        },
                        onPlayClick = { type, mId, season, ep ->
                            navController.navigate("stream_selector/$type/$mId/$season/$ep")
                        },
                        onToggleWatchlist = { detail ->
                            viewModel.toggleWatchlist(detail)
                        },
                        onBackClick = { navController.popBackStack() },
                        onMediaClick = { type, mId ->
                            navController.navigate("details/$type/$mId")
                        }
                    )
                }

                // Dedicated Stream Selector Screen
                composable(
                    route = "stream_selector/{mediaType}/{id}/{season}/{episode}",
                    arguments = listOf(
                        navArgument("mediaType") { type = NavType.StringType },
                        navArgument("id") { type = NavType.IntType },
                        navArgument("season") { type = NavType.IntType; defaultValue = 1 },
                        navArgument("episode") { type = NavType.IntType; defaultValue = 1 }
                    )
                ) { backStack ->
                    val mediaType = backStack.arguments?.getString("mediaType") ?: "movie"
                    val id = backStack.arguments?.getInt("id") ?: 0
                    val season = backStack.arguments?.getInt("season") ?: 1
                    val episode = backStack.arguments?.getInt("episode") ?: 1

                    val availableStreams by viewModel.availableStreams.collectAsStateWithLifecycle()
                    val isScrapingNuvio by viewModel.isScrapingNuvio.collectAsStateWithLifecycle()
                    val scraperStates by viewModel.scraperStates.collectAsStateWithLifecycle()

                    val currentDetail = (detailState as? DetailUiState.Success)?.detail
                    val realTitle = if (currentDetail != null && currentDetail.id == id) currentDetail.title else (if (mediaType == "movie") "Feature Film" else "Series Episode")
                    val subtitle = if (mediaType == "tv") "Season $season • Episode $episode" else "Direct High-Speed CDN"
                    val rawPoster = currentDetail?.posterUrl ?: currentDetail?.backdropUrl
                    val rawBackdrop = currentDetail?.backdropUrl ?: currentDetail?.posterUrl

                    com.example.ui.screens.StreamSelectorScreen(
                        mediaTitle = realTitle,
                        mediaSubtitle = subtitle,
                        tmdbId = id,
                        mediaType = mediaType,
                        season = season,
                        episode = episode,
                        isScraping = isScrapingNuvio,
                        scraperStates = scraperStates,
                        availableStreams = availableStreams,
                        onStartScraping = { mId, mType, s, e, force ->
                            viewModel.startNuvioScraping(mId, mType, s, e, force)
                        },
                        onSelectStream = { stream ->
                            val mediaIdKey = if (mediaType == "tv") "${mediaType}_${id}_${season}_${episode}" else "${mediaType}_${id}"
                            val formattedPoster = when {
                                rawPoster.isNullOrEmpty() -> null
                                rawPoster.startsWith("/") -> "https://image.tmdb.org/t/p/w500$rawPoster"
                                else -> rawPoster
                            }
                            val formattedBackdrop = when {
                                rawBackdrop.isNullOrEmpty() -> null
                                rawBackdrop.startsWith("/") -> "https://image.tmdb.org/t/p/w500$rawBackdrop"
                                else -> rawBackdrop
                            }

                            viewModel.saveProgress(
                                mediaId = mediaIdKey,
                                tmdbId = id,
                                mediaType = mediaType,
                                title = realTitle,
                                posterPath = formattedPoster,
                                backdropPath = formattedBackdrop,
                                season = if (mediaType == "tv") season else null,
                                episode = if (mediaType == "tv") episode else null,
                                episodeTitle = subtitle,
                                currentTimeSec = 0L,
                                durationSec = 0L,
                                streamUrl = stream.url,
                                streamName = stream.name
                            )
                            val encodedUrl = URLEncoder.encode(stream.url, StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(realTitle, StandardCharsets.UTF_8.toString())
                            val encodedPoster = URLEncoder.encode(formattedPoster ?: "", StandardCharsets.UTF_8.toString())
                            val encodedBackdrop = URLEncoder.encode(formattedBackdrop ?: "", StandardCharsets.UTF_8.toString())
                            navController.navigate("direct_player/$encodedUrl/$encodedTitle?mediaType=$mediaType&tmdbId=$id&season=$season&episode=$episode&posterPath=$encodedPoster&backdropPath=$encodedBackdrop")
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Direct ExoPlayer Stream Player Screen
                composable(
                    route = "direct_player/{streamUrl}/{title}?mediaType={mediaType}&tmdbId={tmdbId}&season={season}&episode={episode}&posterPath={posterPath}&backdropPath={backdropPath}",
                    arguments = listOf(
                        navArgument("streamUrl") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType },
                        navArgument("mediaType") { type = NavType.StringType; defaultValue = "movie" },
                        navArgument("tmdbId") { type = NavType.IntType; defaultValue = 0 },
                        navArgument("season") { type = NavType.IntType; defaultValue = 1 },
                        navArgument("episode") { type = NavType.IntType; defaultValue = 1 },
                        navArgument("posterPath") { type = NavType.StringType; defaultValue = "" },
                        navArgument("backdropPath") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStack ->
                    val rawUrl = backStack.arguments?.getString("streamUrl") ?: ""
                    val rawTitle = backStack.arguments?.getString("title") ?: "Direct Stream"
                    val mediaType = backStack.arguments?.getString("mediaType") ?: "movie"
                    val tmdbId = backStack.arguments?.getInt("tmdbId") ?: 0
                    val season = backStack.arguments?.getInt("season") ?: 1
                    val episode = backStack.arguments?.getInt("episode") ?: 1
                    val rawPoster = backStack.arguments?.getString("posterPath") ?: ""
                    val rawBackdrop = backStack.arguments?.getString("backdropPath") ?: ""

                    val decodedUrl = try { URLDecoder.decode(rawUrl, StandardCharsets.UTF_8.toString()) } catch (e: Exception) { rawUrl }
                    val decodedTitle = try { URLDecoder.decode(rawTitle, StandardCharsets.UTF_8.toString()) } catch (e: Exception) { rawTitle }
                    val decodedPoster = try { URLDecoder.decode(rawPoster, StandardCharsets.UTF_8.toString()) } catch (e: Exception) { rawPoster }
                    val decodedBackdrop = try { URLDecoder.decode(rawBackdrop, StandardCharsets.UTF_8.toString()) } catch (e: Exception) { rawBackdrop }

                    com.example.ui.screens.DirectPlayerScreen(
                        streamUrl = decodedUrl,
                        mediaTitle = decodedTitle,
                        mediaType = mediaType,
                        tmdbId = tmdbId,
                        season = if (mediaType == "tv") season else null,
                        episode = if (mediaType == "tv") episode else null,
                        posterPath = decodedPoster.ifEmpty { null },
                        backdropPath = decodedBackdrop.ifEmpty { null },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Fallback Embed Player Screen
                composable(
                    route = "player/{mediaType}/{id}/{season}/{episode}",
                    arguments = listOf(
                        navArgument("mediaType") { type = NavType.StringType },
                        navArgument("id") { type = NavType.IntType },
                        navArgument("season") { type = NavType.IntType; defaultValue = 1 },
                        navArgument("episode") { type = NavType.IntType; defaultValue = 1 }
                    )
                ) { backStack ->
                    val mediaType = backStack.arguments?.getString("mediaType") ?: "movie"
                    val id = backStack.arguments?.getInt("id") ?: 0
                    val season = backStack.arguments?.getInt("season") ?: 1
                    val episode = backStack.arguments?.getInt("episode") ?: 1

                    val mediaTitle = if (mediaType == "movie") "Feature Film" else "Series Stream"
                    val mediaSubtitle = if (mediaType == "tv") "Season $season • Episode $episode" else "Vision TV+ Cinema"

                    val vidSrcUrl = viewModel.buildVidSrcUrl(mediaType, id, season, episode)
                    val vixSrcUrl = viewModel.buildVixSrcUrl(mediaType, id, season, episode)

                    val availableStreams by viewModel.availableStreams.collectAsStateWithLifecycle()
                    val isScrapingNuvio by viewModel.isScrapingNuvio.collectAsStateWithLifecycle()
                    val scraperStates by viewModel.scraperStates.collectAsStateWithLifecycle()

                    PlayerScreen(
                        tmdbId = id,
                        mediaType = mediaType,
                        season = season,
                        episode = episode,
                        vidSrcUrl = vidSrcUrl,
                        vixSrcUrl = vixSrcUrl,
                        title = mediaTitle,
                        subtitle = mediaSubtitle,
                        availableStreams = availableStreams,
                        isScrapingNuvio = isScrapingNuvio,
                        scraperStates = scraperStates,
                        onStartNuvioScraping = { mId, mType, s, e, force ->
                            viewModel.startNuvioScraping(mId, mType, s, e, force)
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            if (showBottomBar) {
                GlassNavigationBar(
                    items = navItems,
                    currentRoute = currentRoute,
                    isCollapsed = isNavCollapsed,
                    onItemSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
