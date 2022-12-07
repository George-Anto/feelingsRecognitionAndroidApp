package com.example.firebaselogin.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.firebaselogin.R
import com.example.firebaselogin.firebase.FirestoreClass
import com.example.firebaselogin.model.User
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        //This is used to hide the status bar and make the sign up screen as a full screen activity
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setupActionBar()

        btn_sign_up.setOnClickListener {
            registerUser()
        }
    }

    //Function for actionBar Setup
    private fun setupActionBar() {

        setSupportActionBar(toolbar_sign_up_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
        }

        toolbar_sign_up_activity.setNavigationOnClickListener { onBackPressed() }
    }

    //Function to register a user to our app using the Firebase
    private fun registerUser() {
        //Here we get the text from editText and trim the space
        val name: String = et_name.text.toString().trim {
                name ->
            name <= ' '
        }
        val email: String = et_email.text.toString().trim {
                email ->
            email <= ' '
        }
        val password: String = et_password.text.toString().trim {
                password ->
            password <= ' '
        }

        if (validateForm(name, email, password)) {
            //Show the progress dialog
            showProgressDialog(resources.getString(R.string.please_wait))
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                        task ->
                    //If the registration is successfully done
                    if (task.isSuccessful) {

                        //Firebase registered user
                        val firebaseUser: FirebaseUser = task.result!!.user!!
                        //Registered Email
                        val registeredEmail = firebaseUser.email!!

                        val user = User(firebaseUser.uid, name, registeredEmail)

                        //Call the registerUser function of FirestoreClass to make an entry in the database
                        FirestoreClass().registerUser(this@SignUpActivity, user)
                    } else {
                        super.showErrorSnackBar(task.exception!!.message!!)

                        super.hideProgressDialog()
                    }
                }
        }
    }

    //Function to validate the entries of a new user.
    private fun validateForm(name: String, email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(name) -> {
                showErrorSnackBar(resources.getString(R.string.enter_name))
                false
            }
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar(resources.getString(R.string.enter_email))
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar(resources.getString(R.string.enter_password))
                false
            }
            else -> {
                true
            }
        }
    }

    //Function to be called when user is registered successfully and an entry is made in the firestore database.
    fun userRegisteredSuccess() {

        Toast.makeText(
            this@SignUpActivity,
            resources.getString(R.string.successfully_registered),
            Toast.LENGTH_SHORT
        ).show()

        // Hide the progress dialog
        hideProgressDialog()

        //Here the new user registered is automatically signed-in so we just redirect them to the main menu
        startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
        // Finish the Sign-Up Screen
        finish()
    }
}