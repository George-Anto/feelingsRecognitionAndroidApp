package com.example.firebaselogin.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import com.google.firebase.auth.FirebaseAuth
import com.example.firebaselogin.R
import com.example.firebaselogin.firebase.FirestoreClass
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        //This is used to hide the status bar and make the sign in screen as a full screen activity
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setupActionBar()

        btn_sign_in.setOnClickListener {
            signInRegisteredUser()
        }
    }

    //Function for actionBar Setup
    private fun setupActionBar() {

        setSupportActionBar(toolbar_sign_in_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
        }

        toolbar_sign_in_activity.setNavigationOnClickListener { onBackPressed() }
    }

    //Function for Sign-In using the registered user using the email and password
    private fun signInRegisteredUser() {
        //Here we get the text from editText and trim the space
        val email: String = et_email.text.toString().trim {
                email ->
            email <= ' '
        }
        val password: String = et_password.text.toString().trim {
                password ->
            password <= ' '
        }

        if (validateForm(email, password)) {
            //Show the progress dialog
            super.showProgressDialog(resources.getString(R.string.please_wait))

            //Sign-In using FirebaseAuth
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        //Calling the FirestoreClass signInUser function to get the data of user from database
                        FirestoreClass().loadUserData(this@SignInActivity)
                    } else {
                        super.showErrorSnackBar(task.exception!!.message!!)
                        super.hideProgressDialog()
                    }
                }
        }
    }

    //Function to validate the entries of the user
    private fun validateForm(email: String, password: String): Boolean {
        return if (TextUtils.isEmpty(email)) {
            super.showErrorSnackBar(resources.getString(R.string.enter_email))
            false
        } else if (TextUtils.isEmpty(password)) {
            super.showErrorSnackBar(resources.getString(R.string.enter_password))
            false
        } else {
            true
        }
    }

    //Function to get the user details from the firestore database after authentication
    fun signInSuccess() {

        super.hideProgressDialog()

        startActivity(Intent(this@SignInActivity, MainActivity::class.java))
        this.finish()
    }
}