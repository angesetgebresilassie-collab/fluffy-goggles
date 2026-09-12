package com.example.data.api

import com.example.data.model.YouTubeSearchResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("videoEmbeddable") videoEmbeddable: String = "true", // Only embeddable videos
        @Query("videoCategoryId") videoCategoryId: String? = null, // Allow all categories (lyric videos may be in Entertainment or Music)
        @Query("videoSyndicated") videoSyndicated: String? = null,
        @Query("maxResults") maxResults: Int = 10
    ): YouTubeSearchResponse

    // Cheapest possible authenticated call (1 quota unit vs 100 for search) - used only
    // to confirm a key actually works before we save it. We don't care about the
    // response body, only whether the request succeeds or comes back with an auth error.
    @GET("videos")
    suspend fun validateApiKey(
        @Query("part") part: String = "id",
        @Query("chart") chart: String = "mostPopular",
        @Query("maxResults") maxResults: Int = 1,
        @Query("key") apiKey: String
    ): Response<ResponseBody>
}
