package gr.unipi.feelingsrecognition.utils

object Constants {

    //Firestore Database Constants
    //users table in the database
    const val USERS: String = "users"
    //videos_to_watch table in the database
    const val VIDEOS_TO_WATCH: String = "videos_to_watch"

    //Firestore Database Constants
    //Fields of the users table in the database
    const val IMAGE: String = "image"
    const val NAME: String = "name"
    const val MOBILE: String = "mobile"
    const val FACE_VIDEOS = "faceVideos"

    //Firebase Storage Constants
    //The bucket in which the photos of the users will be stored
    const val USERS_PROFILE_PHOTOS = "users_profile_photos"
    //The bucket in which the videos of the user's faces are stored
    const val FACES_VIDEOS_BUCKET = "faces_videos"

    //The name of an intent extra information
    const val SIGN_UP_SUCCESS = "Sign Up Success"

    //The name of an intent extra information
    const val VIDEO_DATA = "Video Data"
    //VideoData property default value
    const val VIDEO_DATA_FACE_VIDEO_DEFAULT_VALUE = "Only used when linked with the face video of a user"
    //VideoData property: VideoType - potential value
    const val OUR_COLLECTION_VIDEO_TYPE = "Our Collection Video"

    const val EMPTY_STRING = ""
    const val ONE_BLANK_CHAR = ' '

    // Delays for the execution of some functions
    const val DELAY = 3000
    const val SPLASH_DELAY = 2500

    //The name of an intent extra information
    const val YOUTUBE_URL = "Youtube URL"
    //Youtube API url
    const val YOUTUBE_BASE_API_URL = "https://www.googleapis.com/youtube/v3/"
    //Youtube video url format checker regex
    //Only the standard and the shortened video url formats are allowed
    const val VALID_YOUTUBE_URL_REG_EX = "(?:(?:https?:)?//)?(?:www\\.)?(?:youtube\\.com/(?:[^/]+/\\.+" +
            "/|(?:v|e(?:mbed)?)|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})(?:\\?[\\w\\-]+=[^&]*)*"
    //Shortened youtube video url format checker regex
    const val SHORTENED_YOUTUBE_URL_REG_EX = """^https?://youtu\.be/[^?/]+(\?.*)?$"""
    const val STANDARD_YOUTUBE_URL = "https://www.youtube.com/watch?v="
    //Youtube http call parameter
    const val SNIPPET = "snippet"
    //Type of the youtube videos
    const val YOUTUBE_VIDEO_TYPE = "Youtube Video"
    const val SLASH = "/"
    const val QUESTION_MARK = "?"

    //MP4 mime type
    const val MP4 = "video/mp4"
    //MP4 Extension
    const val MP4_EXTENSION = ".mp4"

    //Face video analysis API related properties
    const val FACE_API_URL = "https://emotion-analyser-service-3nsx.onrender.com"
    const val VIDEO_DATA_FACE_API = "videoData"
    const val SECONDS_90 = 90L
    const val VIDEO = "video"
    const val VIDEO_STAR = "video/*"
    const val GUEST = "guest"
    const val V = "v="
    const val DASH = "-"
    const val ANDROID = "android"
    const val DATE_FORMAT = "yyyyMMddHHmmss"
    const val FOR_UPLOAD_TO_FACE_ANALYSIS_API = "For upload to face analysis API"

    //8 Kilobytes value
    const val KILOBYTES_8 = 8 * 1024
}