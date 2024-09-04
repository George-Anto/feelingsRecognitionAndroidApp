package gr.unipi.feelingsrecognition.activities

import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import gr.unipi.feelingsrecognition.R
import gr.unipi.feelingsrecognition.utils.Constants
import kotlinx.android.synthetic.main.dialog_progress.*
import java.util.regex.Pattern

//We created this BaseActivity that inherits from the AppCompatActivity to use it as the
//activity some of the other activities in our app will inherit from
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

    //The red snackbar that is displayed to the user when needed (in error events)
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

    //The green snackbar that is displayed to the user when needed
    fun showSuccessSnackBar(message: String) {
        val snackBar =
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(
            ContextCompat.getColor(
                this@BaseActivity,
                R.color.snackbar_success_color
            )
        )
        snackBar.show()
    }

    //Function to get the extension of selected image
    fun getFileExtension(uri: Uri?): String? {

        //MimeTypeMap: Two-way map that maps MIME-types to file extensions and vice versa
        //getSingleton(): Get the singleton instance of MimeTypeMap
        //getExtensionFromMimeType(): Return the registered extension for the given MIME type
        //contentResolver.getType(): Return the MIME type of the given content URL
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

    //Function to extract the video id of a youtube url
    fun extractVideoId(url: String): String {
        val pattern = Pattern.compile(Constants.VALID_YOUTUBE_URL_REG_EX)
        val matcher = pattern.matcher(url)

        return if (matcher.matches()) {
            matcher.group(1) ?: Constants.EMPTY_STRING
        } else {
            Constants.EMPTY_STRING
        }
    }

    //Function to check if there is an active network connection
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        //Check if there is an active network
        connectivityManager.activeNetwork?.let { activeNetwork ->
            connectivityManager.getNetworkCapabilities(activeNetwork)?.let { networkCapabilities ->
                //Check for different types of internet network (Cellular or WiFi)
                return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            }
        }
        //No active network or network capabilities detected
        return false
    }
}