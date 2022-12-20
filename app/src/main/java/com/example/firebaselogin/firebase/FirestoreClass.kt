package com.example.firebaselogin.firebase

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.firebaselogin.model.User
import com.example.firebaselogin.utils.Constants
import com.example.firebaselogin.R
import com.example.firebaselogin.activities.*
import com.example.firebaselogin.model.VideoData

//Custom class where we add the operations performed for the firestore database
class FirestoreClass {

    //Create an instance of the Firestore database
    private val fireStore = FirebaseFirestore.getInstance()

    //Function to make an entry for the registered user in the firestore database
    //We enroll the user to the database and not only to the authentication module
    fun registerUser(activity: SignUpActivity, userInfo: User) {

        //We get the user table
        fireStore
            .collection(Constants.USERS)
            //Document ID of the current user as the key of the entry
            .document(getCurrentUserID())
            //Pass the user object we want to save
            .set(userInfo, SetOptions.merge())
            //If the operation is a success
            .addOnSuccessListener {
                //We call the function of the sign up activity that redirects the now registered user
                activity.userRegisteredSuccess()
            }
            //If the operation failed, log the error and display it to the user too
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                activity.showErrorSnackBar(e.message.toString())
                Log.e(
                    activity.javaClass.simpleName,
                    "Error writing document",
                    e
                )
            }
    }

    //Function to sign in using firebase and get the user details from firestore database
    fun loadUserData(activity: Activity) {

        //We specify the collection (users table) we want the data from
        fireStore
            .collection(Constants.USERS)
            //We specify the key of the document we want to retrieve from the database
            //The key is the id of the currently signed in user
            //We get this id from the Authentication service of the firebase
            //Review the last function of this class (getCurrentUserID())
            .document(getCurrentUserID())
            //Retrieve the document
            .get()
            //If the operation succeeded
            .addOnSuccessListener { document ->
                Log.i(activity.javaClass.simpleName, document.toString())

                //We receive the document snapshot which is converted into the User data model
                val loggedInUser = document.toObject(User::class.java)!!

                //We call different functions depending on which activity called this method
                when (activity) {
                    //If the SignInActivity called the loadUserData(), we call the corresponding function
                    //to redirect the user to the MainActivity screen
                    is SignInActivity -> {
                        activity.signInSuccess()
                    }
                    is MainActivity -> {
                        //If the MainActivity called it, we update the user info on the drawer (menu)
                        activity.updateNavigationUserDetails(loggedInUser)
                    }
                    is MyProfileActivity -> {
                        //If the MyProfileActivity called it, we prepopulate the edit texts
                        //of the user details in the activity
                        activity.setUserDataInUI(loggedInUser)
                    }
                }
            }
            .addOnFailureListener { e ->
                //If an error occurs, we just hide the progress bar and show the error snackbar
                when (activity) {
                    is SignInActivity -> {
                        activity.hideProgressDialog()
                        activity.showErrorSnackBar(activity.resources.getString(R.string.unexpected_error))
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog()
                        activity.showErrorSnackBar(activity.resources.getString(R.string.unexpected_error))
                    }
                    is MyProfileActivity -> {
                        activity.hideProgressDialog()
                        activity.showErrorSnackBar(activity.resources.getString(R.string.unexpected_error))
                    }
                }
                //Log the error too
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while getting loggedIn user details",
                    e
                )
            }
    }

    //Function to update the user profile data into the database
    //If called by the MyProfileActivity, then we update the user's personal data
    //If called by the MainActivity, then we add a reference to a face video in the storage
    //that belongs to the that user
    fun updateUserProfileData(activity: Activity, userHashMap: HashMap<String, Any>) {
        //The table (collection) we want to use
        fireStore
            .collection(Constants.USERS)
            //The key of the document we want to update
            //which is the id of the current user retrieved by the Authentication module
            .document(getCurrentUserID())
            //A hashmap of the fields of the user entry which are to be updated
            .update(userHashMap)
            //If the update was successful
            .addOnSuccessListener {
                when(activity) {
                    //If the MyProfileActivity called it
                    is MyProfileActivity -> {
                        //Notify the MyProfileActivity for the success result
                        //The user will be redirected to the MainActivity
                        activity.profileUpdateSuccess()
                    }
                    //If the MainActivity called it
                    is MainActivity -> {
                        //Notify the MainActivity for the success result
                        activity.videoUploadToUserTableSuccess()
                    }
                }
            }
            //If the operation failed
            .addOnFailureListener { e ->
                val message: String = if (e.message != null) e.message!! else activity.getString(R.string.unexpected_error)

               when(activity) {
                   //If the MyProfileActivity called it
                   is MyProfileActivity -> {
                       //Notify the MyProfileActivity for the failure result
                       activity.profileUpdateFailure(message)
                   }
                   //If the MainActivity called it
                   is MainActivity -> {
                       //Notify the MainActivity for the failure result
                       activity.videoUploadToUserTableError(message)
                   }
               }
            }
    }

    //Function for getting the user id of current logged in user
    //The user id is retrieved from the Authentication module of the firebase
    fun getCurrentUserID(): String {
        //An Instance of currentUser using FirebaseAuth
        val currentUser = FirebaseAuth.getInstance().currentUser

        //We assign the currentUserId to a variable, if the currentUser in not null
        //Or else the id remains empty and that means that no user is currently signed in
        var currentUserID = ""
        if (currentUser != null) {
            currentUserID = currentUser.uid
        }

        return currentUserID
    }

    //Function to retrieve the data about the available videos that exist in the firestore database
    fun getVideos(activity: VideoChooserActivity) {
        activity.showProgressDialog(activity.resources.getString(R.string.please_wait))

        //Initialize an ArrayList for the videos data
        val videosData: ArrayList<VideoData> = ArrayList()

        //Get all the data from the the videos_to_watch collection of the firestore database
        fireStore
            .collection(Constants.VIDEOS_TO_WATCH)
            .get()
            //If all the data was retrieved successfully
            .addOnSuccessListener { documents ->
                //Get each entry (document) of the collection
                for (document in documents) {
                    Log.i("Document", "${document.id} => ${document.data}")
                    //Add it to the ArrayList
                    videosData.add(document.toObject(VideoData::class.java))
                }

                //If there are some videosData, display them to the UI
                //Else notify the user that no videos were present
                if (videosData.size > 0) activity.loadVideosToUI(videosData) else activity.noVideosAvailable()
            }
            //If an error occurred
            .addOnFailureListener { e ->
                Log.e("Error", "Error getting documents: ", e)
                //Show an error message
                activity.showErrorSnackBar(activity.resources.getString(R.string.error_loading_videos))
            }
    }
}