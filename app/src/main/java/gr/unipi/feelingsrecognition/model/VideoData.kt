package gr.unipi.feelingsrecognition.model

import android.os.Parcelable
import gr.unipi.feelingsrecognition.utils.Constants
import kotlinx.android.parcel.Parcelize

//Data class to store all the video related info
//By making this class parcelable we can pass the object
//from one activity to another using intents
@Parcelize
data class VideoData (
    val id: String = Constants.EMPTY_STRING,
    val uri: String = Constants.EMPTY_STRING,
    val name: String = Constants.EMPTY_STRING,
    val thumbnail: String = Constants.EMPTY_STRING,
    //This field is used when we store the info of a video with
    //the face video of the user in the firestore database
    var faceVideoLinked: String = Constants.VIDEO_DATA_FACE_VIDEO_DEFAULT_VALUE,
    var videoType: String = Constants.EMPTY_STRING
): Parcelable