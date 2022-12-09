package com.example.firebaselogin.utils

object Constants {

    //Firestore Database Constants
    //users table in the database
    const val USERS: String = "users"

    //Firestore Database Constants
    //Fields of the users table in the database
    const val IMAGE: String = "image"
    const val NAME: String = "name"
    const val MOBILE: String = "mobile"

    //Firebase Storage Constants
    //The bucket in which the photos of the users will be stored
    const val USERS_PROFILE_PHOTOS = "users_profile_photos"

    //The name of an intent extra information
    const val SIGN_UP_SUCCESS = "Sign Up Success"
}