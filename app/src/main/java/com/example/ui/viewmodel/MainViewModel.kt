package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.EpisodeDto
import com.example.data.db.AppDatabase
import com.example.data.db.WatchHistoryEntity
import com.example.data.db.WatchlistEntity
import com.example.data.repository.MediaDetail
import com.example.data.repository.MediaItem
import com.example.data.repository.MediaRepository
import com.example.data.repository.NuvioEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val heroItem: MediaItem?,
        val trending: List<MediaItem>,
        val popularMovies: List<MediaItem>,
        val popularTv: List<MediaItem>,
        val topRatedMovies: List<MediaItem>,
        val topRatedTv: List<MediaItem>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

sealed class DetailUiState {
    object Idle : DetailUiState()
    object Loading : DetailUiState()
    data class Success(
        val detail: MediaDetail,
        val episodes: List<EpisodeDto> = emptyList(),
        val selectedSeason: Int = 1
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MediaRepository(database.appDao())
    val providerRepository = com.example.data.repository.ProviderRepository(application)

    val providers: StateFlow<List<com.example.data.repository.ProviderItem>> = providerRepository.providers

    private val addonCatalogRepo = com.example.data.repository.AddonCatalogRepository()
    private val _addonCatalogs = MutableStateFlow<List<com.example.data.repository.AddonCatalogRow>>(emptyList())
    val addonCatalogs: StateFlow<List<com.example.data.repository.AddonCatalogRow>> = _addonCatalogs.asStateFlow()

    val nuvioEngine = NuvioEngine(application, viewModelScope)
    val availableStreams: StateFlow<List<com.example.data.repository.NuvioStream>> = nuvioEngine.availableStreams
    val isScrapingNuvio: StateFlow<Boolean> = nuvioEngine.isScraping
    val scraperStates: StateFlow<Map<String, com.example.data.repository.ScraperExecutionState>> = nuvioEngine.scraperStates

    private val _allPlugins = MutableStateFlow<List<com.example.data.repository.NuvioScraper>>(emptyList())
    val allPlugins: StateFlow<List<com.example.data.repository.NuvioScraper>> = _allPlugins.asStateFlow()

    private val _isLoadingPlugins = MutableStateFlow(false)
    val isLoadingPlugins: StateFlow<Boolean> = _isLoadingPlugins.asStateFlow()

    private val _homeState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _detailState = MutableStateFlow<DetailUiState>(DetailUiState.Idle)
    val detailState: StateFlow<DetailUiState> = _detailState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        loadHomeData()
        viewModelScope.launch {
            providers.collect { activeList ->
                refreshAddonCatalogs(activeList)
                loadAllPlugins()
            }
        }
    }

    fun loadAllPlugins() {
        viewModelScope.launch {
            _isLoadingPlugins.value = true
            val list = nuvioEngine.nuvioRepo.getAllPlugins(providers.value)
            _allPlugins.value = list
            _isLoadingPlugins.value = false
        }
    }

    fun togglePluginEnabled(pluginId: String, isEnabled: Boolean) {
        nuvioEngine.nuvioRepo.togglePluginEnabled(pluginId, isEnabled)
        val current = _allPlugins.value.map {
            if (it.id == pluginId) it.copy(enabled = isEnabled) else it
        }
        _allPlugins.value = current
    }

    fun togglePluginPinned(pluginId: String, isPinned: Boolean) {
        nuvioEngine.nuvioRepo.togglePluginPinned(pluginId, isPinned)
        val current = _allPlugins.value.map {
            if (it.id == pluginId) it.copy(isPinned = isPinned) else it
        }
        _allPlugins.value = current
    }

    fun refreshAddonCatalogs(activeList: List<com.example.data.repository.ProviderItem> = providers.value) {
        viewModelScope.launch {
            val rows = addonCatalogRepo.fetchAddonCatalogs(activeList)
            _addonCatalogs.value = rows
        }
    }

    fun startNuvioScraping(tmdbId: Int, mediaType: String, season: Int = 1, episode: Int = 1, forceRefresh: Boolean = false) {
        nuvioEngine.startScraping(tmdbId, mediaType, season, episode, providers.value, forceRefresh)
    }

    fun testProvider(id: String) {
        viewModelScope.launch {
            val item = providers.value.find { it.id == id } ?: return@launch
            providerRepository.updateProviderStatus(id, "Testing", null)
            val res = providerRepository.testProvider(item.url, item.category)
            if (res.isSuccess) {
                providerRepository.updateProviderStatus(id, "Working", res.responseTimeMs)
            } else {
                providerRepository.updateProviderStatus(id, "Failed", res.responseTimeMs)
            }
        }
    }

    fun addProvider(
        url: String,
        category: com.example.data.repository.ProviderCategory,
        customName: String?,
        onResult: (com.example.data.repository.ProviderTestResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = providerRepository.addProvider(url, category, customName)
            if (res.isSuccess) {
                refreshAddonCatalogs()
            }
            onResult(res)
        }
    }

    fun removeProvider(id: String) {
        providerRepository.removeProvider(id)
        refreshAddonCatalogs()
    }

    fun toggleProvider(id: String, enabled: Boolean) {
        providerRepository.toggleProvider(id, enabled)
        refreshAddonCatalogs()
    }

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.watchHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val watchlist: StateFlow<List<WatchlistEntity>> = repository.watchlist.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val searchHistory: StateFlow<List<com.example.data.db.SearchHistoryEntity>> = repository.searchHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadHomeData() {
        viewModelScope.launch {
            _homeState.value = HomeUiState.Loading

            val trendingRes = repository.getTrendingAll()
            val popMoviesRes = repository.getPopularMovies()
            val popTvRes = repository.getPopularTvShows()
            val topMoviesRes = repository.getTopRatedMovies()
            val topTvRes = repository.getTopRatedTvShows()

            if (trendingRes.isSuccess) {
                val trending = trendingRes.getOrDefault(emptyList())
                val hero = trending.firstOrNull()
                _homeState.value = HomeUiState.Success(
                    heroItem = hero,
                    trending = trending,
                    popularMovies = popMoviesRes.getOrDefault(emptyList()),
                    popularTv = popTvRes.getOrDefault(emptyList()),
                    topRatedMovies = topMoviesRes.getOrDefault(emptyList()),
                    topRatedTv = topTvRes.getOrDefault(emptyList())
                )
            } else {
                _homeState.value = HomeUiState.Error(
                    trendingRes.exceptionOrNull()?.localizedMessage ?: "Failed to load media catalog"
                )
            }
        }
    }

    fun loadDetails(mediaType: String, id: Int) {
        viewModelScope.launch {
            _detailState.value = DetailUiState.Loading

            val result = if (mediaType == "movie") {
                repository.getMovieDetails(id)
            } else {
                repository.getTvDetails(id)
            }

            if (result.isSuccess) {
                val detail = result.getOrThrow()
                var episodeList: List<EpisodeDto> = emptyList()
                val seasonNum = if (detail.seasons.isNotEmpty()) detail.seasons.first().seasonNumber else 1

                if (mediaType == "tv") {
                    val epRes = repository.getSeasonEpisodes(id, seasonNum)
                    episodeList = epRes.getOrDefault(emptyList())
                }

                _detailState.value = DetailUiState.Success(
                    detail = detail,
                    episodes = episodeList,
                    selectedSeason = seasonNum
                )
            } else {
                _detailState.value = DetailUiState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to load details"
                )
            }
        }
    }

    fun selectSeason(seriesId: Int, seasonNumber: Int) {
        val currentState = _detailState.value
        if (currentState is DetailUiState.Success) {
            viewModelScope.launch {
                val epRes = repository.getSeasonEpisodes(seriesId, seasonNumber)
                _detailState.value = currentState.copy(
                    episodes = epRes.getOrDefault(emptyList()),
                    selectedSeason = seasonNumber
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            val tmdbRes = repository.searchMulti(query).getOrDefault(emptyList())
            val activeProviders = providers.value
            val addonRes = addonCatalogRepo.searchStremioAddons(query, activeProviders)

            val merged = (tmdbRes + addonRes).distinctBy { "${it.mediaType}_${it.id}" }
            _searchResults.value = merged
            _isSearching.value = false
        }
    }

    fun saveSearchQuery(query: String) {
        viewModelScope.launch {
            repository.saveSearchQuery(query)
        }
    }

    fun deleteSearchQuery(query: String) {
        viewModelScope.launch {
            repository.deleteSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun deleteWatchHistory(mediaId: String) {
        viewModelScope.launch {
            repository.deleteWatchHistory(mediaId)
        }
    }

    fun buildEmbedUrl(mediaType: String, tmdbId: Int, season: Int = 1, episode: Int = 1): String {
        return repository.buildVidSrcUrl(mediaType, tmdbId, season, episode)
    }

    fun getDirectHlsStreamUrl(tmdbId: Int): String {
        return repository.getDirectHlsStreamUrl(tmdbId)
    }

    fun isInWatchlist(mediaId: String): Flow<Boolean> = repository.isInWatchlist(mediaId)

    fun toggleWatchlist(item: MediaItem) {
        viewModelScope.launch {
            val mediaId = "${item.mediaType}_${item.id}"
            val entity = WatchlistEntity(
                mediaId = mediaId,
                tmdbId = item.id,
                mediaType = item.mediaType,
                title = item.title,
                posterPath = item.posterUrl,
                backdropPath = item.backdropUrl,
                voteAverage = item.voteAverage,
                overview = item.overview
            )
            repository.toggleWatchlist(entity)
        }
    }

    fun toggleWatchlist(detail: MediaDetail) {
        viewModelScope.launch {
            val mediaId = "${detail.mediaType}_${detail.id}"
            val entity = WatchlistEntity(
                mediaId = mediaId,
                tmdbId = detail.id,
                mediaType = detail.mediaType,
                title = detail.title,
                posterPath = detail.posterUrl,
                backdropPath = detail.backdropUrl,
                voteAverage = detail.voteAverage,
                overview = detail.overview
            )
            repository.toggleWatchlist(entity)
        }
    }

    fun buildVidSrcUrl(mediaType: String, tmdbId: Int, season: Int = 1, episode: Int = 1): String =
        repository.buildVidSrcUrl(mediaType, tmdbId, season, episode)

    fun buildVixSrcUrl(mediaType: String, tmdbId: Int, season: Int = 1, episode: Int = 1): String =
        repository.buildVixSrcUrl(mediaType, tmdbId, season, episode)

    fun saveProgress(
        mediaId: String,
        tmdbId: Int,
        mediaType: String,
        title: String,
        posterPath: String?,
        backdropPath: String?,
        season: Int? = null,
        episode: Int? = null,
        episodeTitle: String? = null,
        currentTimeSec: Long,
        durationSec: Long,
        streamUrl: String? = null,
        streamName: String? = null
    ) {
        viewModelScope.launch {
            repository.saveProgress(
                mediaId = mediaId,
                tmdbId = tmdbId,
                mediaType = mediaType,
                title = title,
                posterPath = posterPath,
                backdropPath = backdropPath,
                season = season,
                episode = episode,
                episodeTitle = episodeTitle,
                currentTimeSec = currentTimeSec,
                durationSec = durationSec,
                streamUrl = streamUrl,
                streamName = streamName
            )
        }
    }
}
