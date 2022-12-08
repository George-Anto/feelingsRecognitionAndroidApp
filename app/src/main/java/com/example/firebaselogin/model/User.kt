package com.example.firebaselogin.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

//Data class to store all the user related info
//By making this class parcelable we can pass the object
//from one activity to another using intents (not needed yet)
@Parcelize
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val image: String = "",
    val mobile: String = "",
) : Parcelable