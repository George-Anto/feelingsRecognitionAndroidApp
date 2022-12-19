package com.example.firebaselogin.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VideoData (
    val id: String = "",
    val uri: String = "",
    val name: String = "",
    val thumbnail: String = ""
): Parcelable