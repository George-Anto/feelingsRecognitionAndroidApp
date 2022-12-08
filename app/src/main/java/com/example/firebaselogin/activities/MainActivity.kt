package com.example.firebaselogin.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.firebaselogin.R
import com.example.firebaselogin.firebase.FirestoreClass
import com.example.firebaselogin.model.User
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

//This activity inherits from BaseActivity and can use its functions
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupActionBar()

        //Assign the NavigationView.OnNavigationItemSelectedListener to navigation view
        nav_view.setNavigationItemSelectedListener(this)

        //Get the current logged in user details from the Firestore class
        //that is in charge of the database connectivity and manipulation
        FirestoreClass().loadUserData(this@MainActivity)
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
}