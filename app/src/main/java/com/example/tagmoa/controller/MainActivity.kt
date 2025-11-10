package com.example.tagmoa.controller

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tagmoa.R
import com.example.tagmoa.model.AuthProvider
import com.example.tagmoa.model.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAuthenticated()) {
            return
        }

        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navigation_tasks -> {
                    replaceFragment(MainTaskListFragment())
                    true
                }
                R.id.navigation_calendar -> {
                    replaceFragment(CalendarFragment())
                    true
                }
                R.id.navigation_profile -> {
                    replaceFragment(UserProfileFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navigation_home
            replaceFragment(HomeFragment())
        }
    }
    

    override fun onStart() {
        super.onStart()
        ensureAuthenticated()
    }

    private fun ensureAuthenticated(): Boolean {
        val session = SessionManager.currentSession ?: run {
            redirectToLogin()
            return false
        }

        if (session.provider == AuthProvider.GOOGLE && firebaseAuth.currentUser == null) {
            SessionManager.clearSession()
            redirectToLogin()
            return false
        }
        return true
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerMain, fragment)
            .commit()
    }
}
