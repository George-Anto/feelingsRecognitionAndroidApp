package com.example.firebaselogin.activities

import android.app.Dialog
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
//import com.google.firebase.auth.FirebaseAuth
import com.example.firebaselogin.R
import kotlinx.android.synthetic.main.dialog_progress.*

open class BaseActivity : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false

    //This is a progress dialog instance which will be initialized later on
    private lateinit var progressDialog: Dialog

    //This function is used to show the progress dialog with the title and message to user
    fun showProgressDialog(text: String) {
        progressDialog = Dialog(this)

        //Set the screen content from a layout resource.
        //The resource will be inflated, adding all top-level views to the screen
        progressDialog.setContentView(R.layout.dialog_progress)

        progressDialog.tv_progress_text.text = text

        //Start the dialog and display it on screen.
        progressDialog.show()
    }

    //This function is used to dismiss the progress dialog if it is visible to user
    fun hideProgressDialog() {
        progressDialog.dismiss()
    }

//    fun getCurrentUserID(): String {
//        return FirebaseAuth.getInstance().currentUser!!.uid
//    }

    fun doubleBackToExit() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(
                this,
                resources.getString(R.string.please_click_back_again_to_exit),
                Toast.LENGTH_SHORT
        ).show()

        Handler(Looper.getMainLooper())
            .postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

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