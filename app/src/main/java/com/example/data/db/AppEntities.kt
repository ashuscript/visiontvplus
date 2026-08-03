package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val mediaId: String, // e.g. "movie_550" or "tv_1399_1_2"
    val tmdbId: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val currentTimeSec: Long = 0L,
    val durationSec: Long = 0L,
    val streamUrl: String? = null,
    val streamName: String? = null,
    val lastWatchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val mediaId: String, // "movie_550" or "tv_1399"
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val voteAverage: Double = 0.0,
    val overview: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
