package com.ndjinny.tagmoa.controller

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.AuthProvider
import com.ndjinny.tagmoa.model.SessionManager
import com.ndjinny.tagmoa.model.TaskCompletionSyncManager
import com.ndjinny.tagmoa.model.UserDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var rootContainer: View
    private lateinit var bottomNavigation: BottomNavigationView
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

        SessionManager.currentSession?.uid?.let { uid ->
            TaskCompletionSyncManager.flushPending(uid, UserDatabase.tasksRef(uid))
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        rootContainer = findViewById(R.id.rootMain)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        applyEdgeToEdgeInsets()

        setupFabMenu()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.navigation_home
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
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navigateTo(HomeFragment(), item.itemId)
                    true
                }
                R.id.navigation_tasks -> {
                    navigateTo(MainTaskListFragment(), item.itemId)
                    true
                }
                R.id.navigation_add -> {
                    toggleFabMenu()
                    false
                }
                R.id.navigation_calendar -> {
                    navigateTo(CalendarFragment(), item.itemId)
                    true
                }
                R.id.navigation_profile -> {
                    navigateTo(UserProfileFragment(), item.itemId)
                    true
                }
                else -> false
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

    private fun applyEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top)
            bottomNavigation.updatePadding(bottom = systemBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(rootContainer)
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
            if (shouldReselectContent) {
                bottomNavigation.selectedItemId = lastContentItemId
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
                if (shouldReselectContent) {
                    bottomNavigation.selectedItemId = lastContentItemId
                }
            }
            .start()
    }

    private fun alignFabMenuWithAddButton() {
        val addItemView = bottomNavigation.findViewById<View>(R.id.navigation_add) ?: return
        val addCenterX = addItemView.left + addItemView.width / 2f
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
