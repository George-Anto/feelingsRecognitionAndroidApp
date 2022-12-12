package com.example.firebaselogin.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.firebaselogin.R
import com.example.firebaselogin.firebase.FirestoreClass
import com.example.firebaselogin.model.User
import com.example.firebaselogin.utils.Constants
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

//This activity inherits from BaseActivity and can use its functions
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    //Companion object to declare a constant
    companion object {
        //A unique code for asking the Permissions, using this we will check
        //and identify if the user gave permissions for this action
        //in the onRequestPermissionsResult() function
        private const val MULTIPLE_PERMISSIONS_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //If the intent has the extra info that the SignUpActivity only provides
        //that means that this is a new user and we show them a success message
        intent.extras.let { data ->
            if (data != null) {
                if (data.getBoolean(Constants.SIGN_UP_SUCCESS))
                    super.showSuccessSnackBar(resources.getString(R.string.successfully_registered))
            }
        }

        setupActionBar()

        //Assign the NavigationView.OnNavigationItemSelectedListener to navigation view
        nav_view.setNavigationItemSelectedListener(this)

        //Get the current logged in user details from the Firestore class
        //that is in charge of the database connectivity and manipulation
        FirestoreClass().loadUserData(this@MainActivity)

        //Listener for the button that requests the permissions
        btn_main_request_permissions.setOnClickListener {
            checkPermissions()
        }
    }

    //Override the onBackPressed() to close the drawer when open
    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            //Call this method of the BaseActivity to prevent accidental closing of the activity
            super.doubleBackToExit()
        }
    }

    //Function to implement the functionality of the buttons inside of the menu in the drawer
    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        //If the user presses the My Profile button
        when (menuItem.itemId) {
            R.id.nav_my_profile -> {
                //Launch the corresponding activity
                resultLauncher.launch(Intent(
                    this@MainActivity,
                    MyProfileActivity::class.java)
                )
            }
            //If the user presses the sign out button
            R.id.nav_sign_out -> {
                //Sign out the user from firebase in this device
                FirebaseAuth.getInstance().signOut()

                //Send the user to the intro screen of the application
                val intent = Intent(this, IntroActivity::class.java)
                //If the IntroActivity has already started once before,
                //reopen it and not start a new one
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                //Close this activity so the user can not access it again, except by logging in again
                finish()
            }
        }
        //Close the drawer after the above actions are done
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    //The new way of implementing startActivityForResult method
    //without using requests codes
    //We use activity for result, because the user can change their info in the MyProfileActivity
    //This activity will be informed for those actions and load the user data once more
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK) {
            super.showSuccessSnackBar(resources.getString(R.string.profile_data_updated_successfully))
            //Get the user updated details from the database
            FirestoreClass().loadUserData(this@MainActivity)
        }
        //If the user presses the back button the resultCode will not be OK and
        //the else block will run, without having to reach to the database again
        else {
            Log.e("User Update", "Update Cancelled")
        }
    }

    //Function to setup action bar
    private fun setupActionBar() {

        setSupportActionBar(toolbar_main_activity)
        //Set the icon for the menu (drawer) toggle
        toolbar_main_activity.setNavigationIcon(R.drawable.ic_action_navigation_menu)

        toolbar_main_activity.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    //Function for opening and closing the Navigation Drawer (menu)
    private fun toggleDrawer() {

        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    //Function to get the current user details from firestore database
    //THIS FUNCTION IS CALLED INSIDE THE LOADUSERDATA() FUNCTION OF THE FIRESTORE CLASS
    //The loadUserData() function is called in the onCreate() function and when the
    //user has updated their info in the MyProfileActivity
    fun updateNavigationUserDetails(user: User) {
        //The instance of the header view of the navigation view
        val headerView = nav_view.getHeaderView(0)

        //The instance of the user image of the navigation view
        val navUserImage = headerView.findViewById<ImageView>(R.id.iv_user_image)

        //Load the user image in the ImageView using a third party library
        Glide
            .with(this@MainActivity)
            .load(user.image) //URL of the image
            .centerCrop() //Scale type of the image
            .placeholder(R.drawable.ic_user_place_holder) //A default place holder
            .into(navUserImage) //The view in which the image will be loaded

        //The instance of the user name TextView of the navigation view
        val navUsername = headerView.findViewById<TextView>(R.id.tv_username)
        //Set the user name
        navUsername.text = user.name
    }

    //Function that requests the necessary permissions
    private fun checkPermissions() {
        //If we already have the permissions to write to the storage, to use the camera and the microphone
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            //Call the method that we need
            // foo()
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_LONG).show()
        } else {
            //Requests permissions to be granted to this application
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ),
                MULTIPLE_PERMISSIONS_CODE
            )
        }
    }

    //This function will identify the result of runtime permissions after
    //the user allows or denies the permissions based on the unique request code
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //If the request code matches this specific action
        if (requestCode == MULTIPLE_PERMISSIONS_CODE) {
            //If permissions are granted (all three of them)
            if (
                grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED
            ) {
                //Call the method that we need
                //foo()
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_LONG).show()
            } else {
                //Display the error snackbar if permissions are not granted
                //(or) at least one of them
                super.showErrorSnackBar(resources.getString(R.string.multiple_permissions_denied))
            }
        }
    }
}