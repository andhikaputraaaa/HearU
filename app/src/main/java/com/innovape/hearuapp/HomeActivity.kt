package com.innovape.hearuapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.innovape.hearuapp.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity(), Navbar.OnNavigationClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNavFragment = supportFragmentManager.findFragmentById(R.id.bottom_nav_container) as? Navbar
        bottomNavFragment?.setOnNavigationClickListener(this)
    }

    override fun onHomeClick() {
        val bottomNavFragment = supportFragmentManager.findFragmentById(R.id.bottom_nav_container) as? Navbar
        bottomNavFragment?.setActiveItem(0) // 0 is the position for home
    }

    override fun onEditClick() {
//        val intent = Intent(this, EditActivity::class.java)
//        startActivity(intent)
    }

    override fun onProfileClick() {
//        val intent = Intent(this, ProfileActivity::class.java)
//        startActivity(intent)
    }
}