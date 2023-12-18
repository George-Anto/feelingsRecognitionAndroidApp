package gr.unipi.feelingsrecognition.activities

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
import android.widget.MediaController
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import gr.unipi.feelingsrecognition.firebase.FirestoreClass
import gr.unipi.feelingsrecognition.model.VideoData
import gr.unipi.feelingsrecognition.utils.Constants
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import gr.unipi.feelingsrecognition.R
import gr.unipi.feelingsrecognition.interfaces.YouTubeApiService
import gr.unipi.feelingsrecognition.model.User
import gr.unipi.feelingsrecognition.model.VideoDetailsResponse
import gr.unipi.feelingsrecognition.utils.LoadPropertiesFile
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//This activity inherits from BaseActivity and can use its functions
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var videoCapture: VideoCapture? = null
    private var videoData: VideoData? = null
    private val apiService: YouTubeApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.YOUTUBE_BASE_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(YouTubeApiService::class.java)
    }
    private var youtubePlayerListener: YouTubePlayerListener? = null

    //Companion object to declare a constant
    companion object {
        //A unique code for asking the Permissions, using this we will check
        //and identify if the user gave permissions for this action
        //in the onRequestPermissionsResult() function
        private const val MULTIPLE_PERMISSIONS_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //If the intent has the extra info that the SignUpActivity only provides
        //that means that this is a new user and we show them a success message
        intent.extras.let { data ->
            if (data != null) {
                // Is this check the same? Could I use it instead?
//            data?.let {
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

        // TODO Make the start of the recording of the user automatic after they choose a video to watch
        // TODO Also make the stop recording also automatic when the video is finished
        // TODO Add comments to the new code related to the youtube videos
        //Listener for the button that starts the recording of the video
        btn_capture_video.setOnClickListener {

            //If the user now starts the recording of the video
            if (btn_capture_video.text.toString()
                == resources.getString(R.string.start_recording)) startRecording()
            //If the user has already started recording a video and presses the button again
             else stopRecording()
        }
    }

    //Each time the Activity starts, not with an OK result from the VideoChooserActivity
    override fun onStart() {
        super.onStart()
        //Make the video view invisible when the user has not selected a video to watch
        //and display the message that prompts them to select one
        video_player.visibility = View.GONE
        tv_select_video_to_watch.visibility = View.VISIBLE
        //Also make the button for the recording invisible and the informing textView visible
        btn_capture_video.visibility = View.GONE
        tv_select_a_video_to_start_recording.visibility = View.VISIBLE

        // Check if a youtube url string is present in the intent
        val youtubeUrl = intent.getStringExtra(Constants.YOUTUBE_URL)

        // Use the url to load and play the video if it's present
        if (!youtubeUrl.isNullOrEmpty()) {
            playYoutubeVideo(youtubeUrl)
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
            R.id.nav_choose_video_from_list -> {
                resultLauncherChooseVideo.launch(Intent
                    (this@MainActivity,
                    VideoChooserActivity::class.java)
                )
            }
            R.id.nav_choose_video_from_youtube -> {
                //Send the user to the youtube video chooser screen screen
                startActivity(Intent(this, YoutubeVideoChooserActivity::class.java))
                //Finish this activity so when the user returns here,
                //the activity will load from the start
                //We need this to load the youtube video property to the ui
                finish()
            }
            R.id.nav_my_profile -> {
                //Launch the corresponding activity
                resultLauncherUpdateUser.launch(Intent(
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

    //A test url: "https://www.youtube.com/watch?v=668nUCeBHyY"
    //Another test url: "https://www.youtube.com/watch?v=fLeJJPxua3E"
    private fun playYoutubeVideo(youtubeVideoUrl: String) {
        lifecycle.addObserver(youtube_video_player)
        tv_select_video_to_watch.visibility = View.GONE
        youtube_video_player.visibility = View.VISIBLE
        youtubePlayerListener = object : AbstractYouTubePlayerListener() {
            override fun onReady(@NonNull youTubePlayer: YouTubePlayer) {
                val videoId = extractVideoId(youtubeVideoUrl)
                Log.i("Video Id: ", videoId)
                if (videoId.isNotEmpty()) {
                    youTubePlayer.loadVideo(videoId, 0f)

                    val call: Call<VideoDetailsResponse> = apiService
                        .getVideoDetails(
                            "snippet", videoId,
                            LoadPropertiesFile.loadApiKey(this@MainActivity)
                        )

                    call.enqueue(object : Callback<VideoDetailsResponse> {
                        override fun onResponse(
                            call: Call<VideoDetailsResponse>,
                            response: Response<VideoDetailsResponse>
                        ) {
                            if (response.isSuccessful && response.body()?.items?.isEmpty() == false) {
                                val videoDetails = response.body()?.items?.get(0)
                                val videoTitle = videoDetails?.snippet?.title
                                val videoThumbnail = videoDetails?.snippet?.thumbnails?.default?.url

                                Log.i("Video Title: ", videoTitle ?: "null")
                                Log.i("Video Thumbnail: ", videoThumbnail ?: "null")
                            } else if (response.body()?.items?.isEmpty() == true) {
                                super@MainActivity.showErrorSnackBar(resources.getString(R.string.not_valid_youtube_url))
                            }
                        }

                        override fun onFailure(call: Call<VideoDetailsResponse>, t: Throwable) {
                            // Handle failure
                            Log.e("YouTube Data", "YouTube data could not be retrieved")
                        }
                    })
                }
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                super.onStateChange(youTubePlayer, state)
                if (state == PlayerConstants.PlayerState.ENDED) {
                    //End the recording here
                    Log.i("Youtube Video Ended", "Youtube Video Ended")
                }
            }
        }
        //Add the listener to the youtube_video_player
        youtube_video_player.addYouTubePlayerListener(youtubePlayerListener!!)
    }

    private fun startRecording() {
        //If the videoCapture object is not null,
        //meaning that all the necessary permissions are given by the user
        if (videoCapture != null) {
            //Start the recording and change the text of the button to "Stop Recording"
            btn_capture_video.text = resources.getString(R.string.stop_recording)
            //Change the color of the button to indicate the recording
            btn_capture_video.setBackgroundResource(R.drawable.shape_button_rounded_color_primary)
            //Set the REC indication on so the user knows that the recording has begun
            tv_rec.visibility = View.VISIBLE
            recordVideo()
            //If the videoCapture object is null, all the necessary permissions
            //are not given and we show the corresponding error snackbar
        } else {
            super.showErrorSnackBar(resources.getString(R.string.multiple_permissions_denied))
        }
    }

    @SuppressLint("RestrictedApi")
    private fun stopRecording() {
        //If the videoCapture object is not null, stop the recording
        //and change the text of the button to "Start Recording"
        if (videoCapture != null) {
            btn_capture_video.text = resources.getString(R.string.start_recording)
            //Change the color of the button to indicate the recording has ended
            btn_capture_video.setBackgroundResource(R.drawable.shape_button_rounded_color_text_primary)
            //Set the REC indication off so the user knows that the recording has finished
            tv_rec.visibility = View.INVISIBLE
            videoCapture?.stopRecording()
            //Else show the corresponding error snackbar
        } else {
            super.showErrorSnackBar(resources.getString(R.string.multiple_permissions_denied))
        }
    }

    //The new way of implementing startActivityForResult method
    //without using requests codes
    //We use activity for result, because the user can change their info in the MyProfileActivity
    //This activity will be informed for those actions and load the user data once more
    private val resultLauncherUpdateUser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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

    //We use activity for result when sending the user to choose a video to watch
    //when the user chooses a video or uses the back button, this function is executed
    private val resultLauncherChooseVideo = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()) {
            result ->
        //When the result code is OK, the user has chosen a video to watch
        if (result.resultCode == Activity.RESULT_OK) {
            //Get the videoData from the intent
            val videoData = result.data?.getParcelableExtra<VideoData>(Constants.VIDEO_DATA)
            //Log it to the console
            Log.i("VideoData", videoData.toString())

            //Call the function that loads the video to the UI
            loadVideoToUI(videoData)
        }
        //If the user presses the back button the resultCode will not be OK and
        //the else block will run, without loading any video to the screen
        else {
            Log.e("No Video Chosen", "Video choosing cancelled")
        }
    }

    //Function to load the video to the UI
    private fun loadVideoToUI(videoData: VideoData?) {
        //If the data is not null
        if (videoData != null) {

            //Store it to a field so we can use it anywhere in this activity
            this.videoData = videoData

            //Crete a MediaController instance
            val mediaController = MediaController(this)

            //Pass the uri to the video view and make it visible
            video_player.setVideoPath(videoData.uri)
            video_player.visibility = View.VISIBLE
            //Erase the message to select a video to watch
            tv_select_video_to_watch.visibility = View.GONE

            //Link the mediaController with the video view
            mediaController.setAnchorView(video_player)
            video_player.setMediaController(mediaController)

            //Listener for when the video is ready to play
            video_player.setOnPreparedListener {
                //Start the video
                video_player.start()

                //When the video is ready, make the button that starts the recording visible
                //and the textView invisible
                btn_capture_video.visibility = View.VISIBLE
                tv_select_a_video_to_start_recording.visibility = View.GONE
            }

            //Listener for when the video has ended
            video_player.setOnCompletionListener {

                //If the user has already started recording a video of themselves
                //and the video that they are watching is over
                if (btn_capture_video.text.toString() == resources.getString(R.string.stop_recording))
                    stopRecording()
            }
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

        super.showProgressDialog(resources.getString(R.string.uploading_video))

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

                        //If the videoData is not null
                        //Update the faceVideoLinked property to show to the face video of the user
                        //that we recorded while the user watched the current video (videoData object)
                        if (videoData != null) {
                            videoData!!.faceVideoLinked = uri.toString()
                        }

                        //The field of the current user we want to update is the faceVideos ArrayList<VideoData>()
                        //Because it is an ArrayList, we specify it with the FieldValue.arrayUnion()
                        userHashMap[Constants.FACE_VIDEOS] = FieldValue.arrayUnion(videoData)

                        //Update the data in the database (storing the videoData to the user document) by calling
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
            //If the video was not uploaded successfully
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

    //Clean the youtube_video_player when the activity is finished
    override fun onDestroy() {
        super.onDestroy()
        youtube_video_player.release()
        //Remove the existing YouTubePlayerListener
        if (youtubePlayerListener != null) {
            youtube_video_player.removeYouTubePlayerListener(youtubePlayerListener!!)
        }
    }
}