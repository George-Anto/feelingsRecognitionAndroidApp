package gr.unipi.feelingsrecognition.interfaces

import gr.unipi.feelingsrecognition.model.VideoDetailsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("videos")
    fun getVideoDetails(
        @Query("part") part: String,
        @Query("id") videoId: String,
        @Query("key") apiKey: String
    ): Call<VideoDetailsResponse>
}