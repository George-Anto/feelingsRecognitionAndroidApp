package com.example.firebaselogin.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.firebaselogin.R
import com.example.firebaselogin.firebase.FirestoreClass
import com.example.firebaselogin.model.User
import com.example.firebaselogin.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_my_profile.*
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

class MyProfileActivity : BaseActivity() {

    //Companion object to declare a constant
    companion object {
        //A unique code for asking the Read Storage Permission, using this we will check
        //and identify if the user gave permission for this action
        //in the onRequestPermissionsResult() function
        private const val READ_STORAGE_PERMISSION_CODE = 1
    }

    //Field for URI of a selected image from phone storage
    private var selectedImageFileUri: Uri? = null

    //Field for user details object
    private lateinit var userDetails: User

    //Field for user profile image URL
    private var profileImageURL: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        setupActionBar()

        //Get the current logged in user details from the Firestore class
        //that is in charge of the database connectivity and manipulation
        FirestoreClass().loadUserData(this@MyProfileActivity)

        //Set a listener to the image view
        iv_profile_user_image.setOnClickListener {

            //If we already have the permission to access the storage of the user's phone
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                //Call the method that redirects the user to pick a photo from their gallery
                showImageChooser()
            } else {
                //Requests permissions to be granted to this application
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_STORAGE_PERMISSION_CODE
                )
            }
        }

        //Listener for the update button
        btn_update.setOnClickListener {

            //If the user has selected an image, call the function that uploads it to the firebase storage
            if (selectedImageFileUri != null) {
                uploadUserImage()
            }
            //If an image is not selected, then update the other details of the user
            else {
                showProgressDialog(resources.getString(R.string.please_wait))
                //Call a function to update user details in the database
                updateUserProfileData()
            }
        }
    }

    //This function will identify the result of runtime permission after
    //the user allows or denies the permission based on the unique request code
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //If the request code matches this specific action
        if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            //If permission is granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Call the method that redirects the user to pick a photo from their gallery
                showImageChooser()
            } else {
                //Display the error snackbar if permission is not granted
                super.showErrorSnackBar(resources.getString(R.string.storage_permission_denied))
            }
        }
    }

    //Function to setup the action bar
    private fun setupActionBar() {

        setSupportActionBar(toolbar_my_profile_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = resources.getString(R.string.my_profile)
        }

        toolbar_my_profile_activity.setNavigationOnClickListener { onBackPressed() }
    }

    //Function to set the existing user details in UI
    //THIS FUNCTION IS CALLED INSIDE THE LOADUSERDATA() FUNCTION OF THE FIRESTORE CLASS
    //The loadUserData() function is called in the onCreate() function of this activity
    fun setUserDataInUI(user: User) {

        //Initialize the user details variable
        userDetails = user

        //Load the user image in the ImageView using a third party library
        Glide
            .with(this@MyProfileActivity)
            .load(userDetails.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(iv_profile_user_image)

        et_name.setText(userDetails.name)
        et_email.setText(userDetails.email)
        //If the mobile phone is empty, the user has not given any so we show nothing
        if (user.mobile != "") {
            et_mobile.setText(userDetails.mobile)
        }
    }

    //Function for user profile image selection from phone storage
    private fun showImageChooser() {
        //An intent for launching the image selection of phone storage
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        //Launches the image selection of phone storage
        resultLauncher.launch(galleryIntent)
    }

    //The new way of implementing startActivityForResult method
    //without using requests codes
    //The result that we want from the gallery activity we launched is of course an image
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        //If the result code is OK and the data is not null
        if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {

            try {
                //The uri of selected image from the phone storage
                selectedImageFileUri = result.data!!.data

                //Load the user image in the ImageView using the same library again
                Glide
                    .with(this@MyProfileActivity)
                    .load(Uri.parse(selectedImageFileUri.toString()))//URI of the image
                    .centerCrop() //Scale type of the image
                    .placeholder(R.drawable.ic_user_place_holder) //A default place holder
                    .into(iv_profile_user_image) //The view in which the image will be loaded
            }
            //Catch possible exceptions
            catch (e: IOException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        //If the result code is not OK or the data are empty, log the error that occurred
        else {
            Log.e("Error ${result.resultCode}", result.data.toString())
        }
    }

    //Function to upload the selected user image to firebase cloud storage
    private fun uploadUserImage() {

        super.showProgressDialog(resources.getString(R.string.please_wait))

        if (selectedImageFileUri != null) {

            //Get the storage reference
            //We store those images in the cloud storage of the firebase using unique
            //names for those files from both the if of the user and the current time in millis
            val storageRef: StorageReference = FirebaseStorage.getInstance().reference
                //The bucket name the photos of the users are stored
                .child(Constants.USERS_PROFILE_PHOTOS)
                .child("${FirestoreClass().getCurrentUserID()}-" +
                        "${System.currentTimeMillis()}.${super.getFileExtension(selectedImageFileUri)}")
            //Adding the file to the cloud storage
            storageRef.putFile(selectedImageFileUri!!)
                //If the operation was a success
                .addOnSuccessListener { taskSnapshot ->
                    //Get the downloadable url from the task snapshot
                    taskSnapshot.metadata!!.reference!!.downloadUrl
                        .addOnSuccessListener { uri ->
                            Log.i("Downloadable Image URL", uri.toString())
                            //Assign the image url to the activity field
                            profileImageURL = uri.toString()

                            //Call the function that updates the users details in the database
                            updateUserProfileData()
                        }
                }
                //If the image was not uploaded successfully
                .addOnFailureListener { exception ->
                    //Show the error snackbar
                    super.showErrorSnackBar(exception.message.toString())

                    super.hideProgressDialog()
                }
        }
    }

    //Function to update the user profile details into the database
    private fun updateUserProfileData() {

        //If the phone number field is not empty and the number provided is not a valid number
        //display the error snackbar and return
        if (et_mobile.text.toString().isNotEmpty()  && !isValidPhoneNumber(et_mobile.text.toString())) {
            profileUpdateFailure(resources.getString(R.string.not_valid_phone_number))
            return
        }

        if (et_name.text.toString().isEmpty()) {
            profileUpdateFailure(resources.getString(R.string.enter_name))
            return
        }

        //Create a hashmap object to add the fields that will be updated
        val userHashMap = HashMap<String, Any>()


        //If the user has uploaded a new image, add its url to the hashmap
        if (profileImageURL.isNotEmpty() && profileImageURL != userDetails.image) {
            userHashMap[Constants.IMAGE] = profileImageURL
        }

        //If the user has provided a new name, add it to the hashmap
        if (et_name.text.toString() != userDetails.name) {
            userHashMap[Constants.NAME] = et_name.text.toString()
        }

        //If the user has provided a new number, add it to the hashmap
        if (et_mobile.text.toString() != userDetails.mobile) {
            userHashMap[Constants.MOBILE] = et_mobile.text.toString()
        }

        //Update the data in the database by calling the corresponding
        //function of the FirestoreClass and by passing the userHashMap
        FirestoreClass().updateUserProfileData(this@MyProfileActivity, userHashMap)
    }

    //Function that uses a regular expression to validate the phone provided
    private fun isValidPhoneNumber(phone: String): Boolean {
        val expression = "^([0-9+]|\\(\\d{1,3}\\))[0-9\\-. ]{3,15}$"
        val inputString: CharSequence = phone
        val pattern: Pattern = Pattern.compile(expression)
        val matcher: Matcher = pattern.matcher(inputString)
        return matcher.matches()
    }

    //Function to notify the user profile is updated successfully
    //THIS FUNCTION IS CALLED INSIDE THE UPDATEUSERPROFILEDATA() FUNCTION OF THE FIRESTORE CLASS
    fun profileUpdateSuccess() {
        super.hideProgressDialog()

        //When the update is done, return to the MainActivity
        //with an OK result code to display the changes there too
        setResult(Activity.RESULT_OK)
        finish()
    }

    //Function to notify the user profile is could not be updated
    fun profileUpdateFailure(errorMessage: String) {
        super.hideProgressDialog()
        super.showErrorSnackBar(resources.getString(R.string.user_profile_update_error))

        //Log the error
        Log.e("Error while updating the user.", errorMessage)
    }
}