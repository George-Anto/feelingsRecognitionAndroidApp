package gr.unipi.feelingsrecognition.activities

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import gr.unipi.feelingsrecognition.R
import kotlinx.android.synthetic.main.activity_intro.*

class IntroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        //This is used to hide the status bar and make the splash screen as a full screen activity
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        //This is for the rounded corners of the intro image
        iv_intro_image.clipToOutline = true

        //Get the custom font file from the assets folder and set it to the title textView
        tv_app_name_intro.typeface = Typeface.createFromAsset(assets, "Kyok-Light.otf")

        //Listener for the button that sends user to the login screen
        btn_sign_in_intro.setOnClickListener {
            //Launch the login screen
            startActivity(Intent(this@IntroActivity, SignInActivity::class.java))
        }

        //Listener for the button that sends user to the sign up screen
        btn_sign_up_intro.setOnClickListener {
            //Launch the sign up screen
            startActivity(Intent(this@IntroActivity, SignUpActivity::class.java))
        }
    }
}