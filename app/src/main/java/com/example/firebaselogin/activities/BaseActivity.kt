package com.example.firebaselogin.activities

import android.app.Dialog
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.example.firebaselogin.R
import kotlinx.android.synthetic.main.dialog_progress.*

//We created this BaseActivity that inherits from the AppCompatActivity to use it as the
//activity some of the others activities in our app will inherit from
//We created some methods in here that can be reused in every child activity
open class BaseActivity : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false

    //This is a progress dialog instance which will be initialized later on
    private lateinit var progressDialog: Dialog

    //This function is used to show the progress dialog with the title and message to user
    fun showProgressDialog(text: String) {
        progressDialog = Dialog(this)

        //Set the screen content from a layout resource
        //The resource will be inflated, adding all top-level views to the screen
        progressDialog.setContentView(R.layout.dialog_progress)

        progressDialog.tv_progress_text.text = text

        //Start the dialog and display it on screen.
        progressDialog.show()
    }

    //This function is used to dismiss the progress dialog if it is visible to the user
    fun hideProgressDialog() {
        progressDialog.dismiss()
    }

    //This function is used to prevent the accidental back button pressing
    //in an activity that calls this function in its onBackPressed() function
    //We mainly use it in the MainActivity so the user does not accidentally
    //go back in the login screen
    fun doubleBackToExit() {
        //2) if the user presses it again in a 2 second timeframe from the first press,
        //the onBackPressed function will be called
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        //1) If the user clicks the back button once, this is the message that will be shown
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(
                this,
                resources.getString(R.string.please_click_back_again_to_exit),
                Toast.LENGTH_SHORT
        ).show()

        //3) If 2 seconds have passed and the button is not pressed again, it will be reset
        Handler(Looper.getMainLooper())
            .postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    //The red snackbar that is displayed to the user when needed
    fun showErrorSnackBar(message: String) {
        val snackBar =
                Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(
                ContextCompat.getColor(
                        this@BaseActivity,
                        R.color.snackbar_error_color
                )
        )
        snackBar.show()
    }
}