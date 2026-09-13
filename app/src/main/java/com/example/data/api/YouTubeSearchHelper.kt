package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URLEncoder

/**
 * Direct YouTube search helper that finds embeddable full-length lyric streams (e.g. 7Clouds)
 * without requiring the user to obtain or enter a personal YouTube Data API v3 key.
 */
object YouTubeSearchHelper {
    private val VIDEO_ID_REGEX = Regex(""""videoId"\s*:\s*"([a-zA-Z0-9_-]{11})"""")

    suspend fun findDirectLyricVideo(
        title: String,
        artist: String,
        excludeVideoIds: Set<String> = emptySet()
    ): String? = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()

        // 1. Try specifically for 7Clouds lyrics video
        val id7Clouds = queryYouTubeForFirstVideoId("$cleanTitle $cleanArtist lyrics 7clouds", excludeVideoIds)
        if (!id7Clouds.isNullOrBlank()) return@withContext id7Clouds

        // 2. Try for dedicated lyrics video
        val idLyrics = queryYouTubeForFirstVideoId("$cleanTitle $cleanArtist lyrics", excludeVideoIds)
        if (!idLyrics.isNullOrBlank()) return@withContext idLyrics

        // 3. Try for official audio / full song stream
        val idAudio = queryYouTubeForFirstVideoId("$cleanTitle $cleanArtist audio", excludeVideoIds)
        if (!idAudio.isNullOrBlank()) return@withContext idAudio

        // 4. General fallback
        return@withContext queryYouTubeForFirstVideoId("$cleanTitle $cleanArtist", excludeVideoIds)
    }

    private fun queryYouTubeForFirstVideoId(
        query: String,
        excludeVideoIds: Set<String>
    ): String? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encoded"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            ApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val matches = VIDEO_ID_REGEX.findAll(body)
                for (match in matches) {
                    val candidate = match.groupValues[1]
                    if (candidate.length == 11 && !excludeVideoIds.contains(candidate)) {
                        return candidate
                    }
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
