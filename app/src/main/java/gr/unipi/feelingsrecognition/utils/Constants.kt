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

    // Delay for the execution of a function
    const val DELAY = 3000

    //The name of an intent extra information
    const val YOUTUBE_URL = "Youtube URL"
    //Youtube api url
    const val YOUTUBE_BASE_API_URL = "https://www.googleapis.com/youtube/v3/"
    //Youtube video url format checker regex
    const val  YOUTUBE_ID_REG_EX = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|" +
            "watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200C2F|youtu." +
            "be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
    //Youtube http call parameter
    const val SNIPPET = "snippet"
    //Type of the youtube videos
    const val YOUTUBE_VIDEO_TYPE = "Youtube Video"
}