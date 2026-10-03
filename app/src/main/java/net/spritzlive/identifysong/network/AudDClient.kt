package net.spritzlive.identifysong.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class AudDRecognitionException(message: String) : Exception(message)

/**
 * Thin client for the AudD song-recognition API (https://docs.audd.io/).
 * Sign up at https://dashboard.audd.io/ for a free API token.
 */
object AudDClient {

    private const val ENDPOINT = "https://api.audd.io/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun recognize(apiToken: String, wavFile: File): SongMatch? = withContext(Dispatchers.IO) {
        val fileBody = wavFile.asRequestBody("audio/wav".toMediaTypeOrNull())

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("api_token", apiToken)
            .addFormDataPart("return", "apple_music,spotify")
            .addFormDataPart("file", wavFile.name, fileBody)
            .build()

        val request = Request.Builder()
            .url(ENDPOINT)
            .post(requestBody)
            .build()

        val responseBody = try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw AudDRecognitionException("Server returned HTTP ${response.code}")
                }
                response.body?.string() ?: throw AudDRecognitionException("Empty response from server")
            }
        } catch (e: IOException) {
            throw AudDRecognitionException("Network error: ${e.message}")
        }

        parseResponse(responseBody)
    }

    private fun parseResponse(raw: String): SongMatch? {
        val json = JSONObject(raw)
        val status = json.optString("status")

        if (status != "success") {
            val errorMessage = json.optJSONObject("error")?.optString("error_message")
                ?: "Unknown error from recognition service"
            throw AudDRecognitionException(errorMessage)
        }

        val result = json.optJSONObject("result") ?: return null
        val appleMusic = result.optJSONObject("apple_music")
        val spotify = result.optJSONObject("spotify")

        val artworkUrl = appleMusic
            ?.optJSONObject("artwork")
            ?.optString("url")
            ?.takeIf { it.isNotBlank() }
            ?.replace("{w}x{h}", "500x500")
            ?: spotify
                ?.optJSONObject("album")
                ?.optJSONArray("images")
                ?.optJSONObject(0)
                ?.optString("url")

        // The raw fingerprint match's title/artist can be a fan-uploaded remix or "slowed"
        // edit if that's what's in AudD's catalog; Apple Music / Spotify entries (matched by
        // the same lookup) are curated/official, so prefer those when present.
        val title = appleMusic?.optString("name")?.takeIf { it.isNotBlank() }
            ?: spotify?.optString("name")?.takeIf { it.isNotBlank() }
            ?: result.optString("title", "Unknown title")

        val artist = appleMusic?.optString("artistName")?.takeIf { it.isNotBlank() }
            ?: spotify?.optJSONArray("artists")?.optJSONObject(0)?.optString("name")?.takeIf { it.isNotBlank() }
            ?: result.optString("artist", "Unknown artist")

        return SongMatch(
            title = title,
            artist = artist,
            album = result.optString("album").takeIf { it.isNotBlank() },
            releaseDate = result.optString("release_date").takeIf { it.isNotBlank() },
            songLink = result.optString("song_link").takeIf { it.isNotBlank() },
            artworkUrl = artworkUrl
        )
    }
}
