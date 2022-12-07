package com.example.firebaselogin.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.firebaselogin.R
import com.example.firebaselogin.firebase.FirestoreClass
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

        //This is used to get the file from the assets folder and set it to the title textView
        tv_app_name.typeface = Typeface.createFromAsset(assets, "Kyok-Light.otf")

        //Adding the handler to after the a task after some delay
        Handler(Looper.getMainLooper()).postDelayed({

            //Here if the user is signed in once and not signed out again from the app
            //So next time while coming into the app
            //we will redirect him to MainScreen or else to the Intro Screen

            //Get the current user id
            val currentUserID = FirestoreClass().getCurrentUserID()

            if (currentUserID.isNotEmpty()) {
                //Start the Main Activity
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                // Start the Intro Activity
                startActivity(Intent(this@SplashActivity, IntroActivity::class.java))
            }
            finish()
        }, 2500) //Here we pass the delay time in milliSeconds after which the splash activity will disappear
    }
}