package com.example.firebaselogin.activities

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.example.firebaselogin.R
import com.example.firebaselogin.adapters.VideosToWatchAdapter
import com.example.firebaselogin.firebase.FirestoreClass
import com.example.firebaselogin.model.VideoData
import kotlinx.android.synthetic.main.activity_video_chooser.*

class VideoChooserActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chooser)

        setupActionBar()

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

    fun loadVideosToUI(videosData: ArrayList<VideoData>) {
        super.hideProgressDialog()
        videos_recycler_view.visibility = View.VISIBLE
        tv_no_videos_present.visibility = View.GONE
        videos_recycler_view.layoutManager = GridLayoutManager(this, 2)
        videos_recycler_view.setHasFixedSize(true)

        val adapter = VideosToWatchAdapter(this, videosData)
        videos_recycler_view.adapter = adapter
    }

    fun noVideosAvailable() {
        super.hideProgressDialog()
        videos_recycler_view.visibility = View.GONE
        tv_no_videos_present.visibility = View.VISIBLE
    }
}