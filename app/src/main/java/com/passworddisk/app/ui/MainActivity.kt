package com.passworddisk.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.passworddisk.app.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNavigationView.setupWithNavController(navController)

        // Hide bottom nav bar on auth, add password, totp, categories, and cloud sync fragments
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.authFragment, R.id.addPasswordFragment, R.id.totpFragment, R.id.editPasswordFragment, R.id.categoriesFragment, R.id.cloudSyncFragment -> 
                    bottomNavigationView.visibility = BottomNavigationView.GONE
                else -> bottomNavigationView.visibility = BottomNavigationView.VISIBLE
            }
        }
    }
}