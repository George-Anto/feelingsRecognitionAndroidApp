package gr.unipi.feelingsrecognition.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.google.firebase.auth.FirebaseAuth
import gr.unipi.feelingsrecognition.R
import gr.unipi.feelingsrecognition.firebase.FirestoreClass
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

        //Listener for the button that signs in the user
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

    //Function to sign in a registered user using the email and password
    private fun signInRegisteredUser() {
        //Get the text from editTexts and trim the spaces
        val email: String = et_email.text.toString().trim {
                email ->
            email <= ' '
        }
        val password: String = et_password.text.toString().trim {
                password ->
            password <= ' '
        }

        //If the email and password pass the validation process
        if (validateForm(email, password)) {
            //Show the progress dialog
            super.showProgressDialog(resources.getString(R.string.please_wait))

            //Sign in using FirebaseAuth
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    //If the user is logged in successfully
                    if (task.isSuccessful) {
                        //We call the FirestoreClass signInUser() function
                        //to get the data of the user from the database
                        FirestoreClass().loadUserData(this@SignInActivity)
                    }
                    //If the user could not log in, display the error
                    else {
                        super.showErrorSnackBar(task.exception!!.message!!)
                        super.hideProgressDialog()
                    }
                }
        }
    }

    //Function to validate the entries of the user
    //If the email or the password is empty, display an appropriate error snackbar message
    private fun validateForm(email: String, password: String): Boolean {
        return if (email.isEmpty()) {
            super.showErrorSnackBar(resources.getString(R.string.enter_email))
            false
        } else if (password.isEmpty()) {
            super.showErrorSnackBar(resources.getString(R.string.enter_password))
            false
        } else {
            true
        }
    }

    //Function to send the user to the MainActivity after successful authentication
    //THIS FUNCTION IS CALLED INSIDE THE LOADUSERDATA() FUNCTION OF THE FIRESTORE CLASS
    fun signInSuccess() {

        super.hideProgressDialog()

        startActivity(Intent(this@SignInActivity, MainActivity::class.java))
        this.finish()
    }
}