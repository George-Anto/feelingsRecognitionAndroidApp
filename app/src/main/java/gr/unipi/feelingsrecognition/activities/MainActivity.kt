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
import gr.unipi.feelingsrecognition.interfaces.FaceApiService
import gr.unipi.feelingsrecognition.interfaces.YouTubeApiService
import gr.unipi.feelingsrecognition.model.FaceApiVideoData
import gr.unipi.feelingsrecognition.model.User
import gr.unipi.feelingsrecognition.model.VideoDetailsResponse
import gr.unipi.feelingsrecognition.utils.LoadPropertiesFile
import gr.unipi.feelingsrecognition.utils.UriToFileConverter
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import androidx.lifecycle.lifecycleScope
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import java.text.SimpleDateFormat
import kotlin.random.Random

//This activity inherits from BaseActivity and can use its functions
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var videoCapture: VideoCapture? = null
    private var videoData: VideoData? = null
    //The youtube service that we use to load the youtube video
    private val youTubeApiService: YouTubeApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.YOUTUBE_BASE_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        //Create an implementation of the YouTubeApiService
        //interface using the configured Retrofit instance
        retrofit.create(YouTubeApiService::class.java)
    }
    //The face video analysis API service that we use to send the video data to the API
    private val faceApiService: FaceApiService by lazy {

        //Create an OkHttpClient with custom timeouts and retry policy
        val okHttpClient = OkHttpClient.Builder()
            //Set the maximum time to wait for a connection to be established
            .connectTimeout(Constants.SECONDS_90, TimeUnit.SECONDS)
            //Set the maximum time to wait for the server to send data
            .readTimeout(Constants.SECONDS_90, TimeUnit.SECONDS)
            //Set the maximum time to wait for the server to accept data
            .writeTimeout(Constants.SECONDS_90, TimeUnit.SECONDS)
            //Enable automatic retry on connection failure
            .retryOnConnectionFailure(true)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.FACE_API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        //Create an implementation of the FaceApiService
        //interface using the configured Retrofit instance
        retrofit.create(FaceApiService::class.java)
    }
    private var youtubePlayerListener: YouTubePlayerListener? = null
    //Variable that indicates whether the onStart method runs for the first time since
    //the creation of the activity or not
    private var firstActivityLoad: Boolean = true

    private var userData: User? = null

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
            data?.let {
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
            if (btn_capture_video.text.toString()
                == resources.getString(R.string.start_recording)) startRecording()
            //If the user has already started recording a video and presses the button again
             else stopRecording()
        }

        //Launch a coroutine in the background using lifecycleScope to retrieve the user data
        //The execution of returnUserData will not block the main thread
        lifecycleScope.launch {
            userData = FirestoreClass().returnUserData(this@MainActivity)
        }
    }

    //Each time the Activity starts
    override fun onStart() {
        super.onStart()

        //If the onStart has ran before since the creation of the activity
        //Reset the ui and the video related variables
        if (!firstActivityLoad) resetActivityVideoViews()

        //Update the indicator variable
        firstActivityLoad = false

        //Check if a youtube url string is present in the intent
        val youtubeUrl = intent?.getStringExtra(Constants.YOUTUBE_URL)
        // Use the url to load and play the video if it's present
        if (!youtubeUrl.isNullOrEmpty()) {
            playYoutubeVideo(youtubeUrl)
            //Delete the youtube url data from the intent
            intent?.removeExtra(Constants.YOUTUBE_URL)
        }

        //Check if a videoData object is present in the intent
        val videoData = intent?.getParcelableExtra<VideoData>(Constants.VIDEO_DATA)
        if (videoData !== null) {
            //Log it to the console
            Log.i(Constants.VIDEO_DATA, videoData.toString())
            //Call the function that loads the video to the UI
            loadVideoToUI(videoData)
            //Delete the videoData from the intent
            intent?.removeExtra(Constants.VIDEO_DATA)
        }
    }

    //Check for the necessary permissions every time the activity restarts
    override fun onRestart() {
        super.onRestart()

        previewCamera.setBackgroundResource(R.drawable.ic_camera_background)
        //Set the REC indication off so the user knows that the recording has finished
        tv_rec.visibility = View.INVISIBLE
        //Set the button text to its initial text
        btn_capture_video.text = resources.getString(R.string.start_recording)
        checkPermissions()

        //Check if there is an active network connection
        if (!isNetworkAvailable(this)) {
            //Display a message to the user indicating no internet connectivity
            super.showErrorSnackBar(resources.getString(R.string.no_internet_connection))
        }
    }

    //Override the onBackPressed() to close the drawer when open
    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            //If there is a recording being made at the moment or an uploading is taking place
            //The user cannot use the back button for navigation
            if (recordingIsOnOrVideoIsBeingUploading()) {
                super.showErrorSnackBar(resources.getString(R.string.stop_the_recording_first))
                return
            }
            //Call this method of the BaseActivity to prevent accidental closing of the activity
            super.doubleBackToExit()
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

    //Function to implement the functionality of the buttons inside of the menu in the drawer
    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        //If there is a recording being made at the moment or an uploading is taking place
        //The user cannot use the menu to navigate
        if (recordingIsOnOrVideoIsBeingUploading()) {
            drawer_layout.closeDrawer(GravityCompat.START)
            super.showErrorSnackBar(resources.getString(R.string.stop_the_recording_first))
            return true
        }

        //If the user presses the Choose a Video to Watch button
        when (menuItem.itemId) {
            R.id.nav_choose_video_from_list -> {
                //Send the user to the Video Chooser Activity screen
                startActivity(Intent(this, VideoChooserActivity::class.java))
                //Finish this activity so when the user returns here,
                //the activity will load from the start
                //We need this to load the youtube video property to the ui
                finish()
            }
            //If the user presses the My Profile button
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

    //Checks if a video is being recorded or a video is being uploading
    //If this is the case it returns true else false
    private fun recordingIsOnOrVideoIsBeingUploading(): Boolean {
        return btn_capture_video.text.toString() == resources.getString(R.string.stop_recording)
                || (videoData?.faceVideoLinked == Constants.VIDEO_DATA_FACE_VIDEO_DEFAULT_VALUE)
    }

    //Method the resets the ui and the video related variables
    private fun resetActivityVideoViews() {
        tv_select_video_to_watch.visibility = View.VISIBLE
        tv_rec.visibility = View.INVISIBLE
        btn_capture_video.visibility = View.GONE
        tv_select_a_video_to_start_recording.visibility = View.VISIBLE
        //If the video_player has been initialized, delete it and remove it from ui
        if (video_player != null) {
            video_player.visibility = View.GONE
            video_player.stopPlayback()
        }
        //If the youtube_video_player has been initialized, delete it and remove it from ui
        if (youtube_video_player != null) {
            youtube_video_player.visibility = View.GONE
            youtube_video_player.release()
            //Remove the existing YouTubePlayerListener
            if (youtubePlayerListener != null) {
                youtube_video_player.removeYouTubePlayerListener(youtubePlayerListener!!)
            }
        }
    }

    //A test url: "https://www.youtube.com/watch?v=668nUCeBHyY"
    //A shortened url: "https://youtu.be/668nUCeBHyY?si=AqyYVst4ON-e5ukf"
    //Another test url: "https://www.youtube.com/watch?v=fLeJJPxua3E"
    private fun playYoutubeVideo(youtubeVideoUrl: String) {

        //Check if there is an active network connection
        if (!super.isNetworkAvailable(this)) {
            //Display a message to the user indicating no internet connectivity
            super.showErrorSnackBar(resources.getString(R.string.no_internet_connection))
            return
        }

        lifecycle.addObserver(youtube_video_player)
        tv_select_video_to_watch.visibility = View.GONE
        youtube_video_player.visibility = View.VISIBLE
        //Create a listener for the youtube video
        youtubePlayerListener = object : AbstractYouTubePlayerListener() {

            //When the video is ready
            override fun onReady(@NonNull youTubePlayer: YouTubePlayer) {
                //Extract the video id
                val videoId = extractVideoId(youtubeVideoUrl)
                Log.i("Video Id: ", videoId)
                //If there is a video id, play the video
                if (videoId.isNotEmpty()) {
                    youTubePlayer.loadVideo(videoId, 0f)

                    //Create the http call that we use to get the metadata for that youtube video
                    val call: Call<VideoDetailsResponse> = youTubeApiService
                        .getVideoDetails(
                            Constants.SNIPPET, videoId,
                            //Get the API key from the properties file in our file system
                            LoadPropertiesFile.loadApiKey(this@MainActivity)
                        )

                    //Send the http call to the youtube API
                    call.enqueue(object : Callback<VideoDetailsResponse> {
                        //When we get a response
                        override fun onResponse(
                            call: Call<VideoDetailsResponse>,
                            response: Response<VideoDetailsResponse>
                        ) {
                            //If is was successful and there are actual data
                            if (response.isSuccessful && response.body()?.items?.isEmpty() == false) {
                                //Get the data that we need
                                val videoDetails = response.body()?.items?.get(0)
                                val videoTitle = videoDetails?.snippet?.title
                                val videoThumbnail = videoDetails?.snippet?.thumbnails?.default?.url

                                //Log them
                                Log.i("Video Title: ", videoTitle ?: "null")
                                Log.i("Video Thumbnail: ", videoThumbnail ?: "null")

                                //Make the button that starts the recording visible and the textView invisible
                                btn_capture_video.visibility = View.VISIBLE
                                tv_select_a_video_to_start_recording.visibility = View.GONE

                                //Create the videoData object from data that we retrieved from the API
                                val videoData = VideoData(videoId, youtubeVideoUrl, videoTitle!!, videoThumbnail!!, videoType = Constants.YOUTUBE_VIDEO_TYPE)
                                Log.i(Constants.VIDEO_DATA, videoData.toString())

                                //Store it to a field so we can use it anywhere in this activity
                                this@MainActivity.videoData = videoData

                                //And then start the recording
                                //In this point we know that there is a youtube video with that ID
                                //because the API call for the data was successful
                                if (btn_capture_video.text.toString() == resources.getString(R.string.start_recording))
                                    startRecording()

                            //If there were no data, show an error message to the user
                            //Send the user back to the VideoChooserActivity
                            //after a small delay to choose again
                            } else if (response.body()?.items?.isEmpty() == true) {
                                super@MainActivity.showErrorSnackBar(resources.getString(R.string.not_valid_youtube_url))
                                sendUserToVideoChooserActivityWithSomeDelay()
                            }
                        }

                        //If we get no response from the youtube API, show an error message to the user
                        //Send the user back to the VideoChooserActivity after a small delay to choose again
                        override fun onFailure(call: Call<VideoDetailsResponse>, t: Throwable) {
                            Log.e("No YouTube API Response", "YouTube data could not be retrieved from the API")
                            super@MainActivity.showErrorSnackBar(resources.getString(R.string.youtube_api_error))
                            sendUserToVideoChooserActivityWithSomeDelay()
                        }
                    })
                }
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                super.onStateChange(youTubePlayer, state)
                //If the video has ended
                if (state == PlayerConstants.PlayerState.ENDED) {
                    //If the user has already started recording a video of themselves
                    //and the video that they are watching is over
                    //End the recording here
                    if (btn_capture_video.text.toString() == resources.getString(R.string.stop_recording))
                        stopRecording()
                }
            }
        }
        //Add the listener to the youtube_video_player
        youtube_video_player.addYouTubePlayerListener(youtubePlayerListener!!)
    }

    //Redirects the user to the VideoChooserActivity after the delay period
    private fun sendUserToVideoChooserActivityWithSomeDelay() {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                startActivity(Intent(this@MainActivity, VideoChooserActivity::class.java))
                finish()
            }
        }, Constants.DELAY.toLong())
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

    //Function to load the video to the UI
    private fun loadVideoToUI(videoData: VideoData?) {

        //Check if there is an active network connection
        if (!isNetworkAvailable(this)) {
            //Display a message to the user indicating no internet connectivity
            super.showErrorSnackBar(resources.getString(R.string.no_internet_connection))
            return
        }

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
                //And then start the recording
                if (btn_capture_video.text.toString() == resources.getString(R.string.start_recording))
                    startRecording()
            }

            //Listener for when the video has ended
            video_player.setOnCompletionListener {

                //If the recording of the video has started
                //and the video that they are watching is over
                if (btn_capture_video.text.toString() == resources.getString(R.string.stop_recording))
                    stopRecording()
            }
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
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, Constants.MP4)

        //Launch a coroutine in the background using lifecycleScope to start the recording
        lifecycleScope.launch {
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

                            runOnUiThread {
                                //If the video the user watched was from our collection
                                //Then upload the video to the firebase storage
                                if (videoData!!.videoType == Constants.OUR_COLLECTION_VIDEO_TYPE) {
                                    uploadVideoToFirebase(outputFileResults.savedUri)
                                }//If the video the user watched was from youtube
                                //Then upload the video to the face analysis API
                                else if (videoData!!.videoType == Constants.YOUTUBE_VIDEO_TYPE) {
                                    uploadVideoToFaceApi(outputFileResults.savedUri)
                                }
                            }
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
            //If the recording produced an error
            catch (e: Exception) {
                //Log it to the console
                e.printStackTrace()
                super.showErrorSnackBar(resources.getString(R.string.video_recording_error))
                sendUserToVideoChooserActivityWithSomeDelay()
            }
        }
    }

    //Function to upload the captured video to the firebase storage
    private fun uploadVideoToFirebase(videoUri: Uri?) {

        //Check if there is an active network connection
        if (!super.isNetworkAvailable(this)) {
            //Display a message to the user indicating no internet connectivity
            super.showErrorSnackBar(resources.getString(R.string.no_internet_connection))
            return
        }

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
                        Log.i("Downloadable Video URL", uri.toString())

                        //Create a hashmap object to add the reference of the video we just uploaded
                        //and link it with the current user
                        val userHashMap = HashMap<String, Any>()

                        //Update the faceVideoLinked property to show to the face video of the user
                        //that we recorded while the user watched the current video (videoData object)
                        videoData?.faceVideoLinked = uri.toString()

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

    private fun uploadVideoToFaceApi(videoUri: Uri?) {

        //Check if there is an active network connection
        if (!super.isNetworkAvailable(this)) {
            //Display a message to the user indicating no internet connectivity
            super.showErrorSnackBar(resources.getString(R.string.no_internet_connection))
            return
        }

        super.showProgressDialog(resources.getString(R.string.uploading_video))

        //If no URI is present, then show an error message to the user
        if (videoUri == null) {
            Log.e("No Video URI", "No Video URI detected.")
            super.hideProgressDialog()
            super.showErrorSnackBar(resources.getString(R.string.video_cloud_save_error))
            return
        }

        //Create the video File object, if anything goes wrong and the File is not created successfully
        //we show an error message to the user and stop the process
        val videoFile: File?
        try {
            videoFile = UriToFileConverter.createFileFromUri(this, contentResolver, videoUri, createVideoFileName())
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("No Video File", "Could not create the video file.")
            super.hideProgressDialog()
            super.showErrorSnackBar(resources.getString(R.string.video_cloud_save_error))
            return
        }

        //Create the FaceApiVideoData object that we will send to the API
        val youtubeUrl = videoData?.uri
        val videoId = youtubeUrl?.split(Constants.V)?.get(1)
        val username = userData?.email ?: Constants.GUEST
        val videoFileName = videoFile.name

        val faceApiVideoData = FaceApiVideoData(youtubeUrl!!, videoId!!, username, videoFileName)

        //Create the MultipartBody.Part object that we will send as the face video to the API
        val requestFile = videoFile.asRequestBody(Constants.VIDEO_STAR.toMediaTypeOrNull())
        val videoPart = MultipartBody.Part.createFormData(Constants.VIDEO, videoFile.name, requestFile)

        //Create the http call that we use to send the data to the API
        val call = faceApiService.uploadVideo(videoPart, faceApiVideoData)

        //Send the http call to the face video analysis API
        call.enqueue(object : Callback<ResponseBody> {
            //When we get a response
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                //Set the faceVideoLinked property to indicate that the face video
                //will be uploaded to face analysis API and not Firebase
                //We need to set that so we know the process is terminated
                videoData?.faceVideoLinked = Constants.FOR_UPLOAD_TO_FACE_ANALYSIS_API

                //If is was successful
                if (response.isSuccessful) {
                    Log.i("Video Data uploaded Successfully", response.toString())

                    super@MainActivity.hideProgressDialog()
                    super@MainActivity.showSuccessSnackBar(resources.getString(R.string.video_cloud_save_success))
                } else {
                    Log.e("Error in Face Analysis API Response", response.toString())

                    super@MainActivity.hideProgressDialog()
                    super@MainActivity.showErrorSnackBar(resources.getString(R.string.video_cloud_save_error))
                }
            }

            //If we get no response from theAPI, show an error message to the user
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                //Set the faceVideoLinked property to indicate that the face video
                //will be uploaded to face analysis API and not Firebase
                //We need to set that so we know the process is terminated (even with failure)
                videoData?.faceVideoLinked = Constants.FOR_UPLOAD_TO_FACE_ANALYSIS_API

                Log.e("No Face Analysis API Response", "Error: ${t.message}", t)
                super@MainActivity.hideProgressDialog()

                //Check if the failure is due to network issues
                if (t is IOException) {
                    //Display a message to the user indicating network error
                    super@MainActivity.showErrorSnackBar(resources.getString(R.string.video_cloud_save_network_error))
                } else {
                    //Generic error message
                    super@MainActivity.showErrorSnackBar(resources.getString(R.string.video_cloud_save_error))

                }
            }
        })
    }

    //Function that constructs the name of the File object that we create from the video Uri
    private fun createVideoFileName(): String {
        //A timestamp of the current time in a specific format
        val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())
        val currentDate = Calendar.getInstance().time

        //Five random characters String
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val randomChars = (1..5).map { allowedChars.random(Random(System.currentTimeMillis())) }
            .joinToString(Constants.EMPTY_STRING)

        //Create a StringBuilder to construct the String
        val builder = StringBuilder()
        builder.append(Constants.ANDROID).append(Constants.DASH)
        builder.append(dateFormat.format(currentDate)).append(Constants.DASH)
        builder.append(videoData?.uri?.split(Constants.V)?.get(1) ?: Constants.EMPTY_STRING).append(Constants.DASH)
        builder.append(userData?.email ?: Constants.GUEST).append(Constants.DASH)
        builder.append(randomChars).append(Constants.MP4_EXTENSION)

        return builder.toString()
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
                //If a videoData object is created but we do not have the necessary permissions
                //We set that field in order to indicate that no video upload is taking place
                videoData?.faceVideoLinked = Constants.NO_FACE_VIDEO_UPLOAD
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