package com.example.firebaselogin.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.example.firebaselogin.R
import com.example.firebaselogin.firebase.FirestoreClass
import com.example.firebaselogin.model.User
import com.example.firebaselogin.utils.Constants
import com.google.android.material.navigation.NavigationView
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

//This activity inherits from BaseActivity and can use its functions
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var videoCapture: VideoCapture? = null

    //Companion object to declare a constant
    companion object {
        //A unique code for asking the Permissions, using this we will check
        //and identify if the user gave permissions for this action
        //in the onRequestPermissionsResult() function
        private const val MULTIPLE_PERMISSIONS_CODE = 2
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //If the intent has the extra info that the SignUpActivity only provides
        //that means that this is a new user and we show them a success message
        intent.extras.let { data ->
            if (data != null) {
                if (data.getBoolean(Constants.SIGN_UP_SUCCESS))
                    super.showSuccessSnackBar(resources.getString(R.string.successfully_registered))
            }
        }

        setupActionBar()

        //Assign the NavigationView.OnNavigationItemSelectedListener to navigation view
        nav_view.setNavigationItemSelectedListener(this)

        //Get the current logged in user details from the Firestore class
        //that is in charge of the database connectivity and manipulation
        FirestoreClass().loadUserData(this@MainActivity)

        //Check for the necessary permissions when creating the activity
        checkPermissions()

        //Listener for the button that starts the recording of the video
        btn_capture_video.setOnClickListener {

            //If the user now starts the recording of the video
            if (btn_capture_video.text.toString() == resources.getString(R.string.start_recording)) {
                //And the videoCapture object is not null,
                //meaning that all the necessary permissions are given by the user
                if (videoCapture != null) {
                    //Start the recording and change the text of the button to "Stop Recording"
                    btn_capture_video.text = resources.getString(R.string.stop_recording)
                    //Set the REC indication on so the user knows that the recording has begun
                    tv_rec.visibility = View.VISIBLE
                    recordVideo()
                //If the videoCapture object is null, all the necessary permissions
                //are not given and we show the corresponding error snackbar
                } else {
                    super.showErrorSnackBar(resources.getString(R.string.multiple_permissions_denied))
                }
            //If the user has already started recording a video and presses the button again
            } else {
                //If the videoCapture object is not null, stop the recording
                //and change the text of the button to "Start Recording"
                if (videoCapture != null) {
                    btn_capture_video.text = resources.getString(R.string.start_recording)
                    //Set the REC indication off so the user knows that the recording has finished
                    tv_rec.visibility = View.INVISIBLE
                    videoCapture?.stopRecording()
                //Else show the corresponding error snackbar
                } else {
                    super.showErrorSnackBar(resources.getString(R.string.multiple_permissions_denied))
                }
            }
        }
    }

    //Check for the necessary permissions every time the activity restarts
    override fun onRestart() {
        previewCamera.setBackgroundResource(R.drawable.ic_camera_background)
        //Set the REC indication off so the user knows that the recording has finished
        tv_rec.visibility = View.INVISIBLE
        //Set the button text to its initial text
        btn_capture_video.text = resources.getString(R.string.start_recording)
        checkPermissions()
        super.onRestart()
    }

    //Override the onBackPressed() to close the drawer when open
    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            //Call this method of the BaseActivity to prevent accidental closing of the activity
            super.doubleBackToExit()
        }
    }

    //Function to implement the functionality of the buttons inside of the menu in the drawer
    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        //If the user presses the My Profile button
        when (menuItem.itemId) {
            R.id.nav_my_profile -> {
                //Launch the corresponding activity
                resultLauncher.launch(Intent(
                    this@MainActivity,
                    MyProfileActivity::class.java)
                )
            }
            //If the user presses the sign out button
            R.id.nav_sign_out -> {
                //Sign out the user from firebase in this device
                FirebaseAuth.getInstance().signOut()

                //Send the user to the intro screen of the application
                val intent = Intent(this, IntroActivity::class.java)
                //If the IntroActivity has already started once before,
                //reopen it and not start a new one
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                //Close this activity so the user can not access it again, except by logging in again
                finish()
            }
        }
        //Close the drawer after the above actions are done
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    //The new way of implementing startActivityForResult method
    //without using requests codes
    //We use activity for result, because the user can change their info in the MyProfileActivity
    //This activity will be informed for those actions and load the user data once more
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK) {
            super.showSuccessSnackBar(resources.getString(R.string.profile_data_updated_successfully))
            //Get the user updated details from the database
            FirestoreClass().loadUserData(this@MainActivity)
        }
        //If the user presses the back button the resultCode will not be OK and
        //the else block will run, without having to reach to the database again
        else {
            Log.e("User Update", "Update Cancelled")
        }
    }

    //Function to setup action bar
    private fun setupActionBar() {

        setSupportActionBar(toolbar_main_activity)
        //Set the icon for the menu (drawer) toggle
        toolbar_main_activity.setNavigationIcon(R.drawable.ic_action_navigation_menu)

        toolbar_main_activity.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    //Function for opening and closing the Navigation Drawer (menu)
    private fun toggleDrawer() {

        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    //Function to get the current user details from firestore database
    //THIS FUNCTION IS CALLED INSIDE THE LOADUSERDATA() FUNCTION OF THE FIRESTORE CLASS
    //The loadUserData() function is called in the onCreate() function and when the
    //user has updated their info in the MyProfileActivity
    fun updateNavigationUserDetails(user: User) {
        //The instance of the header view of the navigation view
        val headerView = nav_view.getHeaderView(0)

        //The instance of the user image of the navigation view
        val navUserImage = headerView.findViewById<ImageView>(R.id.iv_user_image)

        //Load the user image in the ImageView using a third party library
        Glide
            .with(this@MainActivity)
            .load(user.image) //URL of the image
            .centerCrop() //Scale type of the image
            .placeholder(R.drawable.ic_user_place_holder) //A default place holder
            .into(navUserImage) //The view in which the image will be loaded

        //The instance of the user name TextView of the navigation view
        val navUsername = headerView.findViewById<TextView>(R.id.tv_username)
        //Set the user name
        navUsername.text = user.name
    }

    //Initialize the Camera Provider object
    private fun initializeCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture!!.addListener(
            {
                try {
                    val cameraProvider = cameraProviderFuture!!.get()
                    //If the camera provider is initialized correctly, start the camera
                    startCameraX(cameraProvider)
                }
                //Else log the error
                catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }, getExecutor()
        )
    }

    //Helper function to get the MainExecutor
    private fun getExecutor(): Executor {
        return ContextCompat.getMainExecutor(this)
    }

    //Function that starts the camera
    @SuppressLint("RestrictedApi")
    private fun startCameraX(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        val cameraSelector = CameraSelector.Builder()
            //We start the front (selfie) camera of the user's phone
            //so we can record their face
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        //Set the preview of the camera so the user can see what the camera will be recording
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewCamera.surfaceProvider)

        //Initialize the videoCapture object that we will use in the recordVideo() function
        videoCapture = VideoCapture.Builder().setVideoFrameRate(30).build()

        //Bind the camera provider with the videoCapture object
        cameraProvider.bindToLifecycle(
            (this as LifecycleOwner), cameraSelector, preview, videoCapture
        )
    }

    //The function that records the video
    //We do not ask for permissions in this function se we have to suppress the error that was shown
    //But before we call this function, we make sure we already have all the necessary permissions
    @SuppressLint("RestrictedApi", "MissingPermission")
    private fun recordVideo() {
        //If the videoCapture is not null, we can proceed with the recording
        if (videoCapture == null) return

        val timestamp = System.currentTimeMillis()
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        //Start the actual recording
        try {
            videoCapture!!.startRecording(
                VideoCapture.OutputFileOptions.Builder(
                    contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).build(),
                getExecutor(),
                //Save the video to the user's phone (locally)
                object : VideoCapture.OnVideoSavedCallback {
                    //If the video is saved successfully (locally)
                    override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                        //Show the corresponding success message
//                        super@MainActivity.showSuccessSnackBar(resources.getString(R.string.video_save_locally_success))

                        //Then upload the video to the firebase storage too
                        uploadVideo(outputFileResults.savedUri)
                    }

                    //If the video local saving failed
                    override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                        //Show the corresponding error message
                        super@MainActivity.showErrorSnackBar(resources.getString(R.string.video_save_locally_error))
                        //And print the error message to the console
                        Log.e("Video Saving Locally Error", message)
                    }
                }
            )
        }
        //If the recording produced an error, log it to the console
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //Function to upload the captured video to the firebase storage
    private fun uploadVideo(videoUri: Uri?) {

        super.showProgressDialog(resources.getString(R.string.please_wait))

        //If no URI is present, then show an error message to the user
        if (videoUri == null) {
            Log.e("No Video URI", "No Video URI detected.")
            super.hideProgressDialog()
            super.showErrorSnackBar(resources.getString(R.string.video_cloud_save_error))
            return
        }

        //Get the storage reference
        //We store those videos in the cloud storage of the firebase using unique
        //names for those files from both the id of the user and the current time in millis
        val storageRef: StorageReference = FirebaseStorage.getInstance().reference
            //The bucket name the videos of the users faces are stored
            .child(Constants.FACES_VIDEOS_BUCKET)
            .child("${FirestoreClass().getCurrentUserID()}-" +
                    "${System.currentTimeMillis()}.${super.getFileExtension(videoUri)}")
        //Adding the file to the cloud storage
        storageRef.putFile(videoUri)
            //If the operation was a success
            .addOnSuccessListener { taskSnapshot ->
                //Get the downloadable url from the task snapshot
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        Log.i("Downloadable Image URL", uri.toString())

                        //Create a hashmap object to add the reference of the video we just uploaded
                        //and link it with the current user
                        val userHashMap = HashMap<String, Any>()

                        //The field of the current user we want to update is the faceVideos ArrayList<String>()
                        //Because it is an ArrayList, we specify it with the FieldValue.arrayUnion()
                        userHashMap[Constants.FACE_VIDEOS] = FieldValue.arrayUnion(uri.toString())

                        //Update the data in the database (add the link to the new video) by calling
                        //the corresponding function of the FirestoreClass and by passing the userHashMap
                        FirestoreClass().updateUserProfileData(this, userHashMap)
                    }
                    //If we could not parse the downloadable url, log the error
                    .addOnFailureListener {
                            e ->
                        Log.e("Downloadable Url Error", e.message.toString())
                        super.hideProgressDialog()
                        super.showErrorSnackBar(e.message.toString())
                    }
            }
            //If the image was not uploaded successfully
            .addOnFailureListener { e ->
                //Show the error snackbar and also log the error
                Log.e("Video Upload Error", e.message.toString())
                super.hideProgressDialog()
                super.showErrorSnackBar(resources.getString(R.string.video_cloud_save_error))
            }
    }

    //If the video was uploaded to the storage successfully and then an entry
    //of the link of that video is made to the corresponding user (in the firestore database),
    //then show a success message
    fun videoUploadToUserTableSuccess() {
        super.hideProgressDialog()
        super.showSuccessSnackBar(resources.getString(R.string.video_cloud_save_success))
    }

    //If the link in the corresponding user is not made in the database,
    //then show an error message
    fun videoUploadToUserTableError(errorMessage: String) {
        super.hideProgressDialog()
        super.showErrorSnackBar(resources.getString(R.string.video_cloud_save_error))
        //Log the error
        Log.e("Error while updating the user.", errorMessage)
    }

    //Function that requests the necessary permissions
    private fun checkPermissions() {
        //If we already have the permissions to write to the storage, to use the camera and the microphone
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            //If we have all the necessary permissions, we call the function for the camera initialization
            initializeCamera()
        } else {
            //Requests permissions to be granted to this application
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ),
                MULTIPLE_PERMISSIONS_CODE
            )
        }
    }

    //This function will identify the result of runtime permissions after
    //the user allows or denies the permissions based on the unique request code
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //If the request code matches this specific action
        if (requestCode == MULTIPLE_PERMISSIONS_CODE) {
            //If permissions are granted (all three of them)
            if (
                grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED
            ) {
                //If the user gave all the necessary permissions,
                //call the function that initializes the camera
                initializeCamera()
            } else {
                //Display the error snackbar if permissions are not granted
                //(or) at least one of them
                super.showErrorSnackBar(resources.getString(R.string.multiple_permissions_denied))
            }
        }
    }
}