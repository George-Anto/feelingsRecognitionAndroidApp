package gr.unipi.feelingsrecognition.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import androidx.recyclerview.widget.GridLayoutManager
import gr.unipi.feelingsrecognition.R
import gr.unipi.feelingsrecognition.adapters.VideosToWatchAdapter
import gr.unipi.feelingsrecognition.firebase.FirestoreClass
import gr.unipi.feelingsrecognition.model.VideoData
import gr.unipi.feelingsrecognition.utils.Constants
import kotlinx.android.synthetic.main.activity_video_chooser.*
import java.util.regex.Pattern

class VideoChooserActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chooser)

        setupActionBar()

        //Listener for the button that lets the user watch a youtube video
        btn_watch_youtube_video.setOnClickListener { enterYoutubeUrl() }

        //Check if there is an active network connection
        if (!super.isNetworkAvailable(this)) {
            //Display a message to the user indicating no internet connectivity
            super.showErrorSnackBar(resources.getString(R.string.no_internet_connection))
            return
        }

        //Use the function in the firestore class that retrieves
        //all the available videos from the firebase
        FirestoreClass().getVideos(this)
    }

    //Go to the Main Activity, we do it that way because we have called finish() in it
    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    //Function to setup the action bar
    private fun setupActionBar() {

        setSupportActionBar(toolbar_video_chooser_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = resources.getString(R.string.choose_video)
        }

        toolbar_video_chooser_activity.setNavigationOnClickListener { onBackPressed() }
    }

    //Method that executes when the user presses the Watch YouTube Video button
    //Retrieves the url the user entered
    private fun enterYoutubeUrl() {
        //Get the video url from editTexts and trim the spaces
        val youtubeUrl: String = et_youtube_url.text.toString().trim {
                youtubeUrl ->
            youtubeUrl <= Constants.ONE_BLANK_CHAR
        }

        //Check that the user input is a valid url, has a valid url format and we can extract the video id
        //If this is not the case and the validation is not passed, the process terminates
        if (!URLUtil.isValidUrl(youtubeUrl)
            || super.extractVideoId(youtubeUrl).isEmpty()
            || !isValidYoutubeUrl(youtubeUrl)) {
            super@VideoChooserActivity
                .showErrorSnackBar(resources.getString(R.string.not_valid_youtube_url))
            return
        }

        //Open the Main Activity and send it the url of the youtube video the user inserted
        val intent = Intent(this@VideoChooserActivity, MainActivity::class.java)
        intent.putExtra(Constants.YOUTUBE_URL, expandYouTubeUrl(youtubeUrl))
        startActivity(intent)
        finish()
    }

    //We check if this is a valid youtube video url
    //We allow only the standard and the shortened video url formats
    private fun isValidYoutubeUrl(url: String): Boolean {
        val pattern = Pattern.compile(Constants.VALID_YOUTUBE_URL_REG_EX)
        val matcher = pattern.matcher(url)

        return matcher.matches()
    }

    //If the youtube video url is in the shortened form, change it to the standard form
    private fun expandYouTubeUrl(url: String): String {
        //Check if the URL matches the format of a shortened YouTube URL
        val isShortenedUrl = url.matches(Regex(Constants.SHORTENED_YOUTUBE_URL_REG_EX))

        if (isShortenedUrl) {
            //Extract the video ID from the shortened URL
            val videoId = url.substringAfterLast(Constants.SLASH).substringBefore(Constants.QUESTION_MARK)
            //Construct the original URL
            return Constants.STANDARD_YOUTUBE_URL + videoId
        }
        //If the URL is not in the shortened format, return the original URL
        return url
    }

    //Function that is called in the getVideos() function of the Firestore class
    //It displays all the available videos in the UI
    fun loadVideosToUI(videosData: ArrayList<VideoData>) {
        super.hideProgressDialog()
        //Make visible the view for the videos and invisible the one that states that there are no videos
        videos_recycler_view.visibility = View.VISIBLE
        tv_no_videos_present.visibility = View.GONE
        //Create a grid view for the our recycler view that contains all the videos
        videos_recycler_view.layoutManager = GridLayoutManager(this, 2)
        videos_recycler_view.setHasFixedSize(true)

        //Create an adapter instance and pass it the context and the ArrayList of all the retrieved videos
        val adapter = VideosToWatchAdapter(this, videosData)
        //Pass this adapter to the corresponding property of the recycler view
        videos_recycler_view.adapter = adapter

        //Set an onClick Listener to the adapter and override the function from the interface
        adapter.setOnClickListener(object : VideosToWatchAdapter.OnClickListener {
            override fun onClick(position: Int, model: VideoData) {

                //When the user clicks inside of a video card, they will be redirected to the
                //MainActivity and we also pass the videoData object (model) to the MainActivity
                val intent = Intent(this@VideoChooserActivity, MainActivity::class.java)
                intent.putExtra(Constants.VIDEO_DATA, model)
                startActivity(intent)
                finish()
            }
        })
    }

    //Function that is called in the getVideos() function of the Firestore class
    //If there are no videos retrieved, notify the user about it
    fun noVideosAvailable() {
        super.hideProgressDialog()
        videos_recycler_view.visibility = View.GONE
        tv_no_videos_present.visibility = View.VISIBLE
    }
}