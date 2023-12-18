package gr.unipi.feelingsrecognition.activities

import android.content.Intent
import android.os.Bundle
import android.webkit.URLUtil
import gr.unipi.feelingsrecognition.R
import gr.unipi.feelingsrecognition.utils.Constants
import kotlinx.android.synthetic.main.activity_video_chooser.btn_watch_youtube_video
import kotlinx.android.synthetic.main.activity_video_chooser.et_youtube_url
import kotlinx.android.synthetic.main.activity_youtube_video_chooser.*

class YoutubeVideoChooserActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_youtube_video_chooser)

        setupActionBar()

        //Listener for the button that lets the user watch a youtube video
        btn_watch_youtube_video.setOnClickListener {

            //Get the video url from editTexts and trim the spaces
            val youtubeUrl: String = et_youtube_url.text.toString().trim {
                    youtubeUrl ->
                youtubeUrl <= ' '
            }

            //Check that the user input is a valid url and we can extract the video id
            //If this is not the case and the validation is not passed, the process terminates
            if (!URLUtil.isValidUrl(youtubeUrl) || super.extractVideoId(youtubeUrl).isEmpty()) {
                super@YoutubeVideoChooserActivity
                    .showErrorSnackBar(resources.getString(R.string.not_valid_youtube_url))
                return@setOnClickListener
            }

            //Open the Main Activity and send it the url of the youtube video the user inserted
            val intent = Intent(this@YoutubeVideoChooserActivity, MainActivity::class.java)
            intent.putExtra(Constants.YOUTUBE_URL, youtubeUrl)
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    //Function to setup the action bar
    private fun setupActionBar() {

        setSupportActionBar(toolbar_youtube_video_chooser_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = resources.getString(R.string.choose_youtube_video)
        }

        //When the user presses the back button on the actionbar
        //we send them to the Main Activity but we do it this way because
        //the Main Activity has called finish() and it should be created from the start
        toolbar_youtube_video_chooser_activity.setNavigationOnClickListener {
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
            onBackPressed()
        }
    }
}