package gr.unipi.feelingsrecognition.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import gr.unipi.feelingsrecognition.R
import gr.unipi.feelingsrecognition.firebase.FirestoreClass
import gr.unipi.feelingsrecognition.utils.Constants
import kotlinx.android.synthetic.main.activity_splash.*

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        //This is used to hide the status bar and make the splash screen as a full screen activity
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        //Get the custom font file from the assets folder and set it to the title textView
        tv_app_name.typeface = Typeface.createFromAsset(assets, Constants.KYOK_FONT)

        //The code inside the Handler will be executed after a delay (2.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({

            //If the user has already signed in and not signed out,
            //we redirect them to the MainActivity,
            //else we redirect them to the IntroActivity

            //Get the current user id
            val currentUserID = FirestoreClass().getCurrentUserID()

            //If the id is not empty, there is a user already signed in
            if (currentUserID.isNotEmpty()) {
                //Start the MainActivity
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            }
            //If the id is empty, there is n user for the moment
            else {
                //Start the IntroActivity
                startActivity(Intent(this@SplashActivity, IntroActivity::class.java))
            }
            //Finish this activity so the user can not go back to it using the back button
            finish()
        }, Constants.SPLASH_DELAY.toLong()) //Here we pass the delay time in milliSeconds after which the splash activity will disappear
    }
}