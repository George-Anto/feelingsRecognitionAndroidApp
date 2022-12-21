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
}