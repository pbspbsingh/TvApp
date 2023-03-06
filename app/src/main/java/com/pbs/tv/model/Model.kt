package com.pbs.tv.model

data class TvShow(val title: String, val icon: String)

data class TvShowEpisodes(val episodes: List<String>, val hasMore: Boolean)

data class Episode(val parts: List<EpisodePart>)

data class EpisodePart(val title: String, val url: String)
