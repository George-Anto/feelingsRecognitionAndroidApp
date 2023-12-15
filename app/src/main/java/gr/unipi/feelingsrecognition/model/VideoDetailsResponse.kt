package gr.unipi.feelingsrecognition.model

data class VideoDetailsResponse(
    val items: List<VideoItem>
)

data class VideoItem(
    val snippet: VideoSnippet
)

data class VideoSnippet(
    val title: String,
    val thumbnails: Thumbnails
    // We can add other fields as needed
)

data class Thumbnails(
    val default: Default
)

data class Default(
    val url: String
)
