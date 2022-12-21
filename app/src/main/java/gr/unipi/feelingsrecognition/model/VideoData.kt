package gr.unipi.feelingsrecognition.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

//Data class to store all the video related info
//By making this class parcelable we can pass the object
//from one activity to another using intents
@Parcelize
data class VideoData (
    val id: String = "",
    val uri: String = "",
    val name: String = "",
    val thumbnail: String = "",
    //This field is used when we store the info of a video with
    //the face video of the user in the firestore database
    var faceVideoLinked: String =
        "Only used when linked with the face video of a user"
): Parcelable