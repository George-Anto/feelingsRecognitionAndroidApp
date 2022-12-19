package com.example.firebaselogin.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.firebaselogin.R
import com.example.firebaselogin.firebase.FirestoreClass
import com.example.firebaselogin.model.VideoData
import kotlinx.android.synthetic.main.activity_video_chooser.*

class VideoChooserActivity : BaseActivity() {

    private lateinit var videosToWatch: ArrayList<VideoData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_chooser)

        setupActionBar()

        FirestoreClass().loadVideosToUI(this)
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

//    private fun loadVideos() {
//        super.showProgressDialog(resources.getString(R.string.please_wait))
//        videosToWatch = FirestoreClass().getVideosData()
//        super.hideProgressDialog()
//
//        Log.i("VideoChooserActivity", "Total Videos: ${videosToWatch.size}")
//        videosToWatch.forEach { videoData ->
//            Log.i("VideoChooserActivity", "Current Video: $videoData")
//        }
//    }
}