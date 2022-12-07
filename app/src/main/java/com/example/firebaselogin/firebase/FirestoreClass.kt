package com.example.firebaselogin.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.firebaselogin.activities.MainActivity
import com.example.firebaselogin.activities.MyProfileActivity
import com.example.firebaselogin.activities.SignInActivity
import com.example.firebaselogin.activities.SignUpActivity
import com.example.firebaselogin.model.User
import com.example.firebaselogin.utils.Constants
import com.example.firebaselogin.R

//Custom class where we add the operation performed for the firestore database
class FirestoreClass {

    //Create an instance of Firebase Firestore
    private val fireStore = FirebaseFirestore.getInstance()

    //Function to make an entry of the registered user in the firestore database
    fun registerUser(activity: SignUpActivity, userInfo: User) {

        fireStore.collection(Constants.USERS)
            //Document ID for users fields
            .document(getCurrentUserID())
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                //We call a function of the sign up activity for transferring the result to it
                activity.userRegisteredSuccess()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error writing document",
                    e
                )
            }
    }

    //Function to Sign in using firebase and get the user details from Firestore Database
    fun loadUserData(activity: Activity) {

        //Here we pass the collection name from which we want the data
        fireStore.collection(Constants.USERS)
            //The document id to get the fields of the user
            .document(getCurrentUserID())
            .get()
            .addOnSuccessListener { document ->
                Log.i(activity.javaClass.simpleName, document.toString())

                //Here we have received the document snapshot which is converted into the User data model object
                val loggedInUser = document.toObject(User::class.java)!!

                //Here we call different functions depending on which activity called this method
                when (activity) {
                    //If the SignInActivity called, the loadUserData, we call the corresponding function
                    is SignInActivity -> {
                        activity.signInSuccess()
                    }
                    is MainActivity -> {
                        //If the MainActivity called it, we update the user info on the drawer
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
                //Here if an error occurs, we just hide the progress bar
                when (activity) {
                    is SignInActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MyProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                //And log the error
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while getting loggedIn user details",
                    e
                )
            }
    }

    //Function to update the user profile data into the database
    fun updateUserProfileData(activity: MyProfileActivity, userHashMap: HashMap<String, Any>) {
        fireStore.collection(Constants.USERS) //Collection Name
            .document(getCurrentUserID()) //Document ID
            .update(userHashMap) //A hashmap of fields which are to be updated.
            .addOnSuccessListener {
                //Profile data is updated successfully
                Log.i(activity.javaClass.simpleName,
                    activity.resources.getString(R.string.profile_data_updated_successfully))

                Toast.makeText(activity,
                    activity.resources.getString(R.string.profile_data_updated_successfully),
                    Toast.LENGTH_SHORT).show()

                //Notify the MyProfileActivity for the success result
                activity.profileUpdateSuccess()
            }
            .addOnFailureListener { e ->
                val message: String = if (e.message != null) e.message!! else activity.getString(R.string.unexpected_error)

                //Notify the MyProfileActivity for the failure result
                activity.profileUpdateFailure(message)
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while updating the user.",
                    e
                )
            }
    }

    //Function for getting the user id of current logged in user
    fun getCurrentUserID(): String {
        //An Instance of currentUser using FirebaseAuth
        val currentUser = FirebaseAuth.getInstance().currentUser

        //A variable to assign the currentUserId if it is not null or else it will be blank
        var currentUserID = ""
        if (currentUser != null) {
            currentUserID = currentUser.uid
        }

        return currentUserID
    }
}