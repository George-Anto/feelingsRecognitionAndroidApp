package gr.unipi.feelingsrecognition.model

//Data class to store all the data needed for the http POST request to the face analysis API
data class FaceApiVideoData(
    val videoUrl: String,
    val videoId: String,
    val username: String,
    val videoFileName: String
)
