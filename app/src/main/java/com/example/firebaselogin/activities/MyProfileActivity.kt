package com.example.firebaselogin.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
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

    //Companion object to declare the constants.
    companion object {
        //A unique code for asking the Read Storage Permission using this we will check
        //and identify in the method onRequestPermissionsResult
        private const val READ_STORAGE_PERMISSION_CODE = 1
    }

    //Global variable for URI of a selected image from phone storage
    private var selectedImageFileUri: Uri? = null

    //Global variable for user details
    private lateinit var userDetails: User

    //Global variable for a user profile image URL
    private var profileImageURL: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        setupActionBar()

        FirestoreClass().loadUserData(this@MyProfileActivity)

        iv_profile_user_image.setOnClickListener {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
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

        btn_update.setOnClickListener {

            //Here if the image is not selected then update the other details of user
            if (selectedImageFileUri != null) {
                uploadUserImage()
            } else {
                showProgressDialog(resources.getString(R.string.please_wait))
                //Call a function to update user details in the database
                updateUserProfileData()
            }
        }
    }

    //This function will identify the result of runtime permission after
    // the user allows or deny permission based on the unique code
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            //If permission is granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImageChooser()
            } else {
                //Displaying another toast if permission is not granted
                Toast.makeText(
                    this,
                    "You denied the permission for storage. " +
                            "You can also allow it from settings.",
                    Toast.LENGTH_LONG
                ).show()
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

     //Function to set the existing details in UI
    fun setUserDataInUI(user: User) {

        //Initialize the user details variable
        userDetails = user

        Glide
            .with(this@MyProfileActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(iv_profile_user_image)

        et_name.setText(user.name)
        et_email.setText(user.email)
        if (user.mobile != "") {
            et_mobile.setText(user.mobile)
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

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {

            try {
                //The uri of selection image from phone storage
                selectedImageFileUri = result.data!!.data

                //Load the user image in the ImageView
                Glide
                    .with(this@MyProfileActivity)
                    .load(Uri.parse(selectedImageFileUri.toString()))//URI of the image
                    .centerCrop() //Scale type of the image
                    .placeholder(R.drawable.ic_user_place_holder) //A default place holder
                    .into(iv_profile_user_image) //The view in which the image will be loaded
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.e("Error ${result.resultCode}", result.data.toString())
        }
    }

    //Function to upload the selected user image to firebase cloud storage
    private fun uploadUserImage() {

        super.showProgressDialog(resources.getString(R.string.please_wait))

        if (selectedImageFileUri != null) {

            //Getting the storage reference
            val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE-" + System.currentTimeMillis() + "." + getFileExtension(
                    selectedImageFileUri
                )
            )
            //Adding the file to reference
            sRef.putFile(selectedImageFileUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    // The image upload is success
                    Log.i(
                        "Firebase Image URL",
                        taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                    )

                    //Get the downloadable url from the task snapshot
                    taskSnapshot.metadata!!.reference!!.downloadUrl
                        .addOnSuccessListener { uri ->
                            Log.i("Downloadable Image URL", uri.toString())
                            // assign the image url to the variable.
                            profileImageURL = uri.toString()

                            // Call a function to update user details in the database.
                            updateUserProfileData()
                        }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this@MyProfileActivity,
                        exception.message,
                        Toast.LENGTH_LONG
                    ).show()

                    super.hideProgressDialog()
                }
        }
    }

    //Function to get the extension of selected image
    private fun getFileExtension(uri: Uri?): String? {

        //MimeTypeMap: Two-way map that maps MIME-types to file extensions and vice versa
        //getSingleton(): Get the singleton instance of MimeTypeMap
        //getExtensionFromMimeType: Return the registered extension for the given MIME type
        //contentResolver.getType: Return the MIME type of the given content URL
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

    //Function to update the user profile details into the database
    private fun updateUserProfileData() {

        if (!isValidPhoneNumber(et_mobile.text.toString())) {
            profileUpdateFailure(resources.getString(R.string.not_valid_phone_number))
            return
        }

        val userHashMap = HashMap<String, Any>()

        if (profileImageURL.isNotEmpty() && profileImageURL != userDetails.image) {
            userHashMap[Constants.IMAGE] = profileImageURL
        }

        if (et_name.text.toString() != userDetails.name) {
            userHashMap[Constants.NAME] = et_name.text.toString()
        }

        if (et_mobile.text.toString() != userDetails.mobile) {
            userHashMap[Constants.MOBILE] = et_mobile.text.toString()
        }

        //Update the data in the database
        FirestoreClass().updateUserProfileData(this@MyProfileActivity, userHashMap)
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        val expression = "^([0-9+]|\\(\\d{1,3}\\))[0-9\\-. ]{3,15}$"
        val inputString: CharSequence = phone
        val pattern: Pattern = Pattern.compile(expression)
        val matcher: Matcher = pattern.matcher(inputString)
        return matcher.matches()
    }

    //Function to notify the user profile is updated successfully
    fun profileUpdateSuccess() {
        super.hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }

    fun profileUpdateFailure(errorMessage: String) {
        super.hideProgressDialog()

        super.showErrorSnackBar(errorMessage)
    }
}