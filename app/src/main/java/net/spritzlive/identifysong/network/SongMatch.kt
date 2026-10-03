package net.spritzlive.identifysong.network

data class SongMatch(
    val title: String,
    val artist: String,
    val album: String?,
    val releaseDate: String?,
    val songLink: String?,
    val artworkUrl: String?
)
