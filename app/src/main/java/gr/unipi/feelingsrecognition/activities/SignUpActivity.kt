package gr.unipi.feelingsrecognition.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import gr.unipi.feelingsrecognition.R
import gr.unipi.feelingsrecognition.firebase.FirestoreClass
import gr.unipi.feelingsrecognition.model.User
import gr.unipi.feelingsrecognition.utils.Constants
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

        //Listener for the button that signs up the user
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
        //Get the text from editTexts and trim the spaces
        val name: String = et_name.text.toString().trim {
                name ->
            name <= Constants.ONE_BLANK_CHAR
        }
        val email: String = et_email.text.toString().trim {
                email ->
            email <= Constants.ONE_BLANK_CHAR
        }
        val password: String = et_password.text.toString().trim {
                password ->
            password <= Constants.ONE_BLANK_CHAR
        }

        //If the information the user gave us passes the validation process
        if (validateForm(name, email, password)) {
            //Show the progress dialog
            showProgressDialog(resources.getString(R.string.please_wait))
            //Create the user account
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                        task ->
                    //If the registration is successfully made (with the Authentication service)
                    if (task.isSuccessful) {

                        //Get the Firebase registered user
                        val firebaseUser: FirebaseUser = task.result!!.user!!
                        //Get the registered email
                        val registeredEmail = firebaseUser.email!!

                        //Create a user object according to our model
                        val user = User(firebaseUser.uid, name, registeredEmail)

                        //Call the registerUser() function of FirestoreClass to make an entry in the database
                        //The FirestoreClass is in charge of the database
                        //and storage communication and manipulation
                        //We create an extra entry to the firestore database for the user
                        //on top of the entry to the Authentication service to
                        //store extra information about our users
                        FirestoreClass().registerUser(this@SignUpActivity, user)
                    }
                    //If the registration with the Authentication service of the Firebase
                    //(not the firestore database) is not successful
                    //show the corresponding error snackbar
                    else {
                        super.showErrorSnackBar(task.exception!!.message!!)

                        super.hideProgressDialog()
                    }
                }
        }
    }

    //Function to validate the entries of a new user
    //If any of the necessary data is not provided,
    //the validation will not be passed and an error snackbar will be shown
    private fun validateForm(name: String, email: String, password: String): Boolean {
        return when {
            name.isEmpty() -> {
                showErrorSnackBar(resources.getString(R.string.enter_name))
                false
            }
            email.isEmpty() -> {
                showErrorSnackBar(resources.getString(R.string.enter_email))
                false
            }
            password.isEmpty() -> {
                showErrorSnackBar(resources.getString(R.string.enter_password))
                false
            }
            else -> {
                true
            }
        }
    }

    //Function to be called when user is registered successfully
    //and an entry is made in the firestore database.
    //THIS FUNCTION IS CALLED INSIDE THE REGISTERUSER() FUNCTION OF THE FIRESTORE CLASS
    fun userRegisteredSuccess() {

        // Hide the progress dialog
        hideProgressDialog()

        //The now registered user is automatically signed in so we just redirect them to the main menu
        //We put some extra info to the intent so the MainActivity knows that the SignUpActivity sent it
        //and we can show a success message (snackbar) to the user
        startActivity(Intent(this@SignUpActivity, MainActivity::class.java)
            .putExtra(Constants.SIGN_UP_SUCCESS, true))
        //Finish the sign up screen
        finish()
    }
}