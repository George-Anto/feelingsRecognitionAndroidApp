package gr.unipi.feelingsrecognition.interfaces

import gr.unipi.feelingsrecognition.model.FaceApiVideoData
import gr.unipi.feelingsrecognition.utils.Constants
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

//Interface for interacting with the face analysis API to send video data
interface FaceApiService {

    /**
     * Uploads a video file and associated metadata to the specified face video analysis API endpoint.
     *
     * @param video               The video file to be uploaded as a MultipartBody.Part.
     * @param faceApiVideoData    The metadata associated with the video as a FaceApiVideoData object.
     *                            This will be included as a part named "videoData" in the multipart request.
     * @return                    A [Call] object representing the asynchronous HTTP request.
     *                            The response body is expected to be a ResponseBody.
     */
    @Multipart
    @POST("/api/videoConvertAndUpload")
    fun uploadVideo(
        @Part video: MultipartBody.Part,
        @Part(Constants.VIDEO_DATA_FACE_API) faceApiVideoData: FaceApiVideoData
    ): Call<ResponseBody>
}