package gr.unipi.feelingsrecognition.model

//Model for the response that we get for the video data from the youtube API
data class VideoDetailsResponse(
    val items: List<VideoItem>
)

data class VideoItem(
    val snippet: VideoSnippet
)

data class VideoSnippet(
    val title: String,
    val thumbnails: Thumbnails
)

data class Thumbnails(
    val default: Default
)

data class Default(
    val url: String
)
