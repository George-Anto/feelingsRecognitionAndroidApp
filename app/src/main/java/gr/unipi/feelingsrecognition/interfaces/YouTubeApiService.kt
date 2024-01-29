package gr.unipi.feelingsrecognition.interfaces

import gr.unipi.feelingsrecognition.model.VideoDetailsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

//Interface for interacting with the YouTube API to retrieve video data
interface YouTubeApiService {

    /**
     * Retrieves details for a specific video based on its ID.
     *
     * @param part The part parameter specifies the video resource properties that the API response will include.
     * @param videoId The ID of the YouTube video for which details are requested.
     * @param apiKey The API key for authenticating and authorizing the request to the YouTube api.
     *
     * @return A [Call] object representing the API call, which can be executed to get the response.
     */
    @GET("videos")
    fun getVideoDetails(
        @Query("part") part: String,
        @Query("id") videoId: String,
        @Query("key") apiKey: String
    ): Call<VideoDetailsResponse>
}