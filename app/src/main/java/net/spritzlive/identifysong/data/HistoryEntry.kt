package net.spritzlive.identifysong.data

import kotlinx.serialization.Serializable
import net.spritzlive.identifysong.network.SongMatch
import java.util.UUID

@Serializable
data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String,
    val album: String?,
    val artworkUrl: String?,
    val songLink: String?,
    val identifiedAtMillis: Long
)

fun SongMatch.toHistoryEntry(identifiedAtMillis: Long = System.currentTimeMillis()) = HistoryEntry(
    title = title,
    artist = artist,
    album = album,
    artworkUrl = artworkUrl,
    songLink = songLink,
    identifiedAtMillis = identifiedAtMillis
)
