package com.example.data.repository

import com.example.data.api.CastDto
import com.example.data.api.EpisodeDto
import com.example.data.api.GenreDto
import com.example.data.api.MediaItemDto
import com.example.data.api.MovieDetailDto
import com.example.data.api.RetrofitClient
import com.example.data.api.SeasonDetailDto
import com.example.data.api.TvDetailDto
import com.example.data.db.AppDao
import com.example.data.db.WatchHistoryEntity
import com.example.data.db.WatchlistEntity
import kotlinx.coroutines.flow.Flow

data class MediaItem(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val mediaType: String, // "movie" or "tv"
    val voteAverage: Double,
    val releaseYear: String,
    val genreIds: List<Int> = emptyList()
)

data class MediaDetail(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val mediaType: String,
    val voteAverage: Double,
    val releaseYear: String,
    val tagline: String,
    val genres: List<GenreDto>,
    val durationOrSeasons: String,
    val cast: List<CastDto>,
    val recommendations: List<MediaItem>,
    val seasons: List<TvSeasonSummary> = emptyList()
)

data class TvSeasonSummary(
    val id: Int,
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val posterUrl: String?
)

class MediaRepository(private val appDao: AppDao) {
    private val apiKey = com.example.BuildConfig.TMDB_API_KEY.ifEmpty { "54c3dc672ff2776242c7a468984bcf05" }
    private val api = RetrofitClient.tmdbApi

    fun getImageUrl(path: String?, size: String = "w500"): String? {
        if (path.isNullOrEmpty()) return null
        return "https://image.tmdb.org/t/p/$size$path"
    }

    private fun MediaItemDto.toDomain(): MediaItem {
        val calculatedType = mediaType ?: if (title != null) "movie" else "tv"
        val displayTitle = title ?: name ?: originalTitle ?: originalName ?: "Untitled"
        val date = releaseDate ?: firstAirDate ?: ""
        val year = if (date.length >= 4) date.substring(0, 4) else ""

        return MediaItem(
            id = id,
            title = displayTitle,
            overview = overview ?: "",
            posterUrl = getImageUrl(posterPath, "w500"),
            backdropUrl = getImageUrl(backdropPath, "w1280"),
            mediaType = calculatedType,
            voteAverage = voteAverage ?: 0.0,
            releaseYear = year,
            genreIds = genreIds ?: emptyList()
        )
    }

    suspend fun getTrendingAll(page: Int = 1): Result<List<MediaItem>> = runCatching {
        api.getTrendingAll(apiKey, page).results.map { it.toDomain() }
    }

    suspend fun getTrendingMovies(page: Int = 1): Result<List<MediaItem>> = runCatching {
        api.getTrendingMovies(apiKey, page).results.map { it.toDomain().copy(mediaType = "movie") }
    }

    suspend fun getTrendingTvShows(page: Int = 1): Result<List<MediaItem>> = runCatching {
        api.getTrendingTvShows(apiKey, page).results.map { it.toDomain().copy(mediaType = "tv") }
    }

    suspend fun getPopularMovies(page: Int = 1): Result<List<MediaItem>> = runCatching {
        api.getPopularMovies(apiKey, page).results.map { it.toDomain().copy(mediaType = "movie") }
    }

    suspend fun getPopularTvShows(page: Int = 1): Result<List<MediaItem>> = runCatching {
        api.getPopularTvShows(apiKey, page).results.map { it.toDomain().copy(mediaType = "tv") }
    }

    suspend fun getTopRatedMovies(page: Int = 1): Result<List<MediaItem>> = runCatching {
        api.getTopRatedMovies(apiKey, page).results.map { it.toDomain().copy(mediaType = "movie") }
    }

    suspend fun getTopRatedTvShows(page: Int = 1): Result<List<MediaItem>> = runCatching {
        api.getTopRatedTvShows(apiKey, page).results.map { it.toDomain().copy(mediaType = "tv") }
    }

    suspend fun searchMulti(query: String): Result<List<MediaItem>> = runCatching {
        if (query.isBlank()) emptyList()
        else api.searchMulti(query, apiKey).results.map { it.toDomain() }
    }

    suspend fun getMovieDetails(movieId: Int): Result<MediaDetail> = runCatching {
        val dto = api.getMovieDetails(movieId, apiKey)
        val date = dto.releaseDate ?: ""
        val year = if (date.length >= 4) date.substring(0, 4) else ""
        val runtimeStr = dto.runtime?.let { "${it / 60}h ${it % 60}m" } ?: "2h"

        MediaDetail(
            id = dto.id,
            title = dto.title ?: "Untitled",
            overview = dto.overview ?: "",
            posterUrl = getImageUrl(dto.posterPath, "w500"),
            backdropUrl = getImageUrl(dto.backdropPath, "w1280"),
            mediaType = "movie",
            voteAverage = dto.voteAverage ?: 0.0,
            releaseYear = year,
            tagline = dto.tagline ?: "",
            genres = dto.genres ?: emptyList(),
            durationOrSeasons = runtimeStr,
            cast = dto.credits?.cast?.take(10) ?: emptyList(),
            recommendations = dto.recommendations?.results?.map { it.toDomain() } ?: emptyList()
        )
    }

    suspend fun getTvDetails(seriesId: Int): Result<MediaDetail> = runCatching {
        val dto = api.getTvDetails(seriesId, apiKey)
        val date = dto.firstAirDate ?: ""
        val year = if (date.length >= 4) date.substring(0, 4) else ""
        val seasonsStr = "${dto.numberOfSeasons ?: 1} Season${if ((dto.numberOfSeasons ?: 1) > 1) "s" else ""}"

        val seasonsList = dto.seasons?.filter { (it.seasonNumber) > 0 }?.map {
            TvSeasonSummary(
                id = it.id,
                seasonNumber = it.seasonNumber,
                name = it.name ?: "Season ${it.seasonNumber}",
                episodeCount = it.episodeCount ?: 0,
                posterUrl = getImageUrl(it.posterPath, "w500")
            )
        } ?: emptyList()

        MediaDetail(
            id = dto.id,
            title = dto.name ?: "Untitled",
            overview = dto.overview ?: "",
            posterUrl = getImageUrl(dto.posterPath, "w500"),
            backdropUrl = getImageUrl(dto.backdropPath, "w1280"),
            mediaType = "tv",
            voteAverage = dto.voteAverage ?: 0.0,
            releaseYear = year,
            tagline = dto.tagline ?: "",
            genres = dto.genres ?: emptyList(),
            durationOrSeasons = seasonsStr,
            cast = dto.credits?.cast?.take(10) ?: emptyList(),
            recommendations = dto.recommendations?.results?.map { it.toDomain() } ?: emptyList(),
            seasons = seasonsList
        )
    }

    suspend fun getSeasonEpisodes(seriesId: Int, seasonNumber: Int): Result<List<EpisodeDto>> = runCatching {
        val detail = api.getSeasonDetails(seriesId, seasonNumber, apiKey)
        detail.episodes
    }

    fun buildVidSrcUrl(mediaType: String, tmdbId: Int, season: Int = 1, episode: Int = 1): String {
        val accentColor = "2997ff"
        return if (mediaType == "tv") {
            "https://vidsrc.sbs/embed/tv/$tmdbId/$season/$episode?autoplay=1&color=$accentColor"
        } else {
            "https://vidsrc.sbs/embed/movie/$tmdbId?autoplay=1&color=$accentColor"
        }
    }

    fun buildVixSrcUrl(mediaType: String, tmdbId: Int, season: Int = 1, episode: Int = 1): String {
        val primaryColor = "2997FF"
        val secondaryColor = "10141E"
        return if (mediaType == "tv") {
            "https://vixsrc.to/tv/$tmdbId/$season/$episode?primaryColor=$primaryColor&secondaryColor=$secondaryColor&autoplay=true&lang=en"
        } else {
            "https://vixsrc.to/movie/$tmdbId?primaryColor=$primaryColor&secondaryColor=$secondaryColor&autoplay=true&lang=en"
        }
    }

    fun getDirectHlsStreamUrl(tmdbId: Int): String {
        val hlsStreams = listOf(
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        )
        return hlsStreams[tmdbId.coerceAtLeast(0) % hlsStreams.size]
    }

    // Room Database Operations
    val watchHistory: Flow<List<WatchHistoryEntity>> = appDao.getWatchHistory()
    val watchlist: Flow<List<WatchlistEntity>> = appDao.getWatchlist()
    val searchHistory: Flow<List<com.example.data.db.SearchHistoryEntity>> = appDao.getSearchHistory()

    fun isInWatchlist(mediaId: String): Flow<Boolean> = appDao.isInWatchlistFlow(mediaId)

    suspend fun toggleWatchlist(item: WatchlistEntity) {
        if (appDao.isInWatchlist(item.mediaId)) {
            appDao.removeFromWatchlist(item.mediaId)
        } else {
            appDao.addToWatchlist(item)
        }
    }

    suspend fun deleteWatchHistory(mediaId: String) {
        appDao.deleteWatchHistory(mediaId)
    }

    suspend fun clearWatchHistory() {
        appDao.clearWatchHistory()
    }

    suspend fun saveSearchQuery(query: String) {
        if (query.isNotBlank()) {
            appDao.saveSearchQuery(com.example.data.db.SearchHistoryEntity(query.trim()))
        }
    }

    suspend fun deleteSearchQuery(query: String) {
        appDao.deleteSearchQuery(query)
    }

    suspend fun clearSearchHistory() {
        appDao.clearSearchHistory()
    }

    suspend fun saveProgress(
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
        val formattedPoster = when {
            posterPath.isNullOrEmpty() -> null
            posterPath.startsWith("/") -> "https://image.tmdb.org/t/p/w500$posterPath"
            else -> posterPath
        }
        val formattedBackdrop = when {
            backdropPath.isNullOrEmpty() -> null
            backdropPath.startsWith("/") -> "https://image.tmdb.org/t/p/w500$backdropPath"
            else -> backdropPath
        }

        val entity = WatchHistoryEntity(
            mediaId = mediaId,
            tmdbId = tmdbId,
            mediaType = mediaType,
            title = title,
            posterPath = formattedPoster,
            backdropPath = formattedBackdrop,
            season = season,
            episode = episode,
            episodeTitle = episodeTitle,
            currentTimeSec = currentTimeSec,
            durationSec = durationSec,
            streamUrl = streamUrl,
            streamName = streamName,
            lastWatchedAt = System.currentTimeMillis()
        )
        appDao.saveWatchHistory(entity)
    }
}
