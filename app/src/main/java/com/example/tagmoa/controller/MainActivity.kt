package com.example.tagmoa.controller

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.tagmoa.R
import com.example.tagmoa.model.AuthProvider
import com.example.tagmoa.model.SessionManager
import com.example.tagmoa.view.CurvedBottomNavItem
import com.example.tagmoa.view.CurvedBottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var bottomNavigation: CurvedBottomNavigationView
    private lateinit var fabMenuContainer: View
    private lateinit var fabAddTag: FloatingActionButton
    private lateinit var fabAddMainTask: FloatingActionButton
    private lateinit var fabAddSubTask: FloatingActionButton

    private var lastContentItemId: Int = R.id.navigation_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAuthenticated()) {
            return
        }

        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)
        setupFabMenu()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            bottomNavigation.selectItem(R.id.navigation_home, animate = false)
            replaceFragment(HomeFragment())
        }
    }

    override fun onBackPressed() {
        if (this::fabMenuContainer.isInitialized && fabMenuContainer.isVisible) {
            hideFabMenu(shouldReselectContent = true)
            return
        }
        super.onBackPressed()
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

    private fun setupBottomNavigation() {
        val items = listOf(
            CurvedBottomNavItem(R.id.navigation_add, R.drawable.ic_nav_add, R.string.nav_add),
            CurvedBottomNavItem(R.id.navigation_tasks, R.drawable.ic_nav_tasks, R.string.nav_tasks),
            CurvedBottomNavItem(R.id.navigation_home, R.drawable.ic_nav_home, R.string.nav_home),
            CurvedBottomNavItem(R.id.navigation_calendar, R.drawable.ic_nav_calendar, R.string.nav_calendar),
            CurvedBottomNavItem(R.id.navigation_profile, R.drawable.ic_nav_profile, R.string.nav_profile)
        )

        bottomNavigation.setItems(items)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.id) {
                R.id.navigation_home -> navigateTo(HomeFragment(), item.id)
                R.id.navigation_tasks -> navigateTo(MainTaskListFragment(), item.id)
                R.id.navigation_add -> toggleFabMenu()
                R.id.navigation_calendar -> navigateTo(CalendarFragment(), item.id)
                R.id.navigation_profile -> navigateTo(UserProfileFragment(), item.id)
            }
        }
    }

    private fun navigateTo(fragment: Fragment, itemId: Int) {
        hideFabMenu(shouldReselectContent = false)
        lastContentItemId = itemId
        replaceFragment(fragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerMain, fragment)
            .commit()
    }

    private fun setupFabMenu() {
        fabMenuContainer = findViewById(R.id.addFabMenu)
        fabAddTag = findViewById(R.id.fabAddTag)
        fabAddMainTask = findViewById(R.id.fabAddMainTask)
        fabAddSubTask = findViewById(R.id.fabAddSubTask)

        fabAddTag.setOnClickListener {
            hideFabMenu(shouldReselectContent = true)
            startActivity(Intent(this, TagManagementActivity::class.java))
        }
        fabAddMainTask.setOnClickListener {
            hideFabMenu(shouldReselectContent = true)
            startActivity(Intent(this, AddEditMainTaskActivity::class.java))
        }
        fabAddSubTask.setOnClickListener {
            hideFabMenu(shouldReselectContent = true)
            startActivity(Intent(this, AddEditSubTaskActivity::class.java))
        }

        bottomNavigation.doOnLayout {
            if (fabMenuContainer.isVisible) {
                alignFabMenuWithAddButton()
            }
        }
    }

    private fun toggleFabMenu() {
        if (fabMenuContainer.isVisible) {
            hideFabMenu(shouldReselectContent = true)
        } else {
            showFabMenu()
        }
    }

    private fun showFabMenu() {
        if (fabMenuContainer.isVisible) return
        bottomNavigation.setHighlightOverlayColor(
            ContextCompat.getColor(this, R.color.cbn_add_active_color)
        )
        fabMenuContainer.animate().cancel()
        fabMenuContainer.alpha = 0f
        fabMenuContainer.visibility = View.VISIBLE
        fabMenuContainer.bringToFront()
        fabMenuContainer.post {
            alignFabMenuWithAddButton()
            fabMenuContainer.animate()
                .alpha(1f)
                .setDuration(180L)
                .start()
        }
    }

    private fun hideFabMenu(shouldReselectContent: Boolean) {
        if (!fabMenuContainer.isVisible) {
            bottomNavigation.setHighlightOverlayColor(null)
            if (shouldReselectContent) {
                bottomNavigation.selectItem(lastContentItemId, animate = true)
            }
            return
        }
        fabMenuContainer.animate().cancel()
        fabMenuContainer.animate()
            .alpha(0f)
            .setDuration(150L)
            .withEndAction {
                fabMenuContainer.visibility = View.GONE
                fabMenuContainer.alpha = 1f
                bottomNavigation.setHighlightOverlayColor(null)
                if (shouldReselectContent) {
                    bottomNavigation.selectItem(lastContentItemId, animate = true)
                }
            }
            .start()
    }

    private fun alignFabMenuWithAddButton() {
        val addCenterX = bottomNavigation.getItemCenterX(R.id.navigation_add)
        if (addCenterX <= 0f) return
        if (fabMenuContainer.width == 0) {
            fabMenuContainer.post { alignFabMenuWithAddButton() }
            return
        }
        val navLeft = bottomNavigation.left.toFloat()
        val horizontalOffset = resources.getDimension(R.dimen.add_fab_menu_horizontal_offset)
        val desiredLeft = navLeft + addCenterX - fabMenuContainer.width / 2f + horizontalOffset
        val currentLeft = fabMenuContainer.left.toFloat()
        fabMenuContainer.translationX = desiredLeft - currentLeft
    }
}
