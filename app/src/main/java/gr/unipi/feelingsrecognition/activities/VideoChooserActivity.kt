package gr.unipi.feelingsrecognition.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import gr.unipi.feelingsrecognition.R
import gr.unipi.feelingsrecognition.adapters.VideosToWatchAdapter
import gr.unipi.feelingsrecognition.firebase.FirestoreClass
import gr.unipi.feelingsrecognition.model.VideoData
import gr.unipi.feelingsrecognition.utils.Constants
import kotlinx.android.synthetic.main.activity_video_chooser.*

class VideoChooserActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chooser)

        setupActionBar()

        //Use the function in the firestore class that retrieves
        //all the available videos from the firebase
        FirestoreClass().getVideos(this)
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
                //MainActivity (that started this Activity with a startActivityForResult) and
                //we also pass the videoData object (model) to the MainActivity
                setResult(Activity.RESULT_OK, Intent().putExtra(Constants.VIDEO_DATA, model))
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