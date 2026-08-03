package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaResponse(
    @Json(name = "page") val page: Int? = 1,
    @Json(name = "results") val results: List<MediaItemDto> = emptyList(),
    @Json(name = "total_pages") val totalPages: Int? = 1,
    @Json(name = "total_results") val totalResults: Int? = 0
)

@JsonClass(generateAdapter = true)
data class MediaItemDto(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "original_name") val originalName: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "media_type") val mediaType: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = 0.0,
    @Json(name = "vote_count") val voteCount: Int? = 0,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "genre_ids") val genreIds: List<Int>? = emptyList(),
    @Json(name = "popularity") val popularity: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class MovieDetailDto(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String? = null,
    @Json(name = "tagline") val tagline: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "vote_average") val voteAverage: Double? = 0.0,
    @Json(name = "vote_count") val voteCount: Int? = 0,
    @Json(name = "genres") val genres: List<GenreDto>? = emptyList(),
    @Json(name = "imdb_id") val imdbId: String? = null,
    @Json(name = "credits") val credits: CreditsDto? = null,
    @Json(name = "recommendations") val recommendations: MediaResponse? = null
)

@JsonClass(generateAdapter = true)
data class TvDetailDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String? = null,
    @Json(name = "tagline") val tagline: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int? = 1,
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int? = 1,
    @Json(name = "vote_average") val voteAverage: Double? = 0.0,
    @Json(name = "vote_count") val voteCount: Int? = 0,
    @Json(name = "genres") val genres: List<GenreDto>? = emptyList(),
    @Json(name = "seasons") val seasons: List<TvSeasonSummaryDto>? = emptyList(),
    @Json(name = "credits") val credits: CreditsDto? = null,
    @Json(name = "recommendations") val recommendations: MediaResponse? = null
)

@JsonClass(generateAdapter = true)
data class TvSeasonSummaryDto(
    @Json(name = "id") val id: Int,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "name") val name: String? = null,
    @Json(name = "episode_count") val episodeCount: Int? = 0,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "air_date") val airDate: String? = null,
    @Json(name = "overview") val overview: String? = null
)

@JsonClass(generateAdapter = true)
data class SeasonDetailDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "name") val name: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "episodes") val episodes: List<EpisodeDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class EpisodeDto(
    @Json(name = "id") val id: Int,
    @Json(name = "episode_number") val episodeNumber: Int,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "name") val name: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "still_path") val stillPath: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = 0.0,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "air_date") val airDate: String? = null
)

@JsonClass(generateAdapter = true)
data class GenreDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class GenreListResponse(
    @Json(name = "genres") val genres: List<GenreDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CreditsDto(
    @Json(name = "cast") val cast: List<CastDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CastDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "character") val character: String? = null,
    @Json(name = "profile_path") val profilePath: String? = null
)
