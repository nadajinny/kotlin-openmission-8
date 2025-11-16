package com.ndjinny.tagmoa.controller

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ndjinny.tagmoa.R
import com.ndjinny.tagmoa.model.OnboardingPreferences

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var buttonNext: MaterialButton
    private lateinit var buttonSkip: View

    private val onboardingPreferences by lazy { OnboardingPreferences(this) }
    private var forceShow: Boolean = false
    private val pages by lazy {
        listOf(
            OnboardingPage(
                imageRes = R.drawable.img_usage_1
            ),
            OnboardingPage(
                imageRes = R.drawable.img_usage_2
            ),
            OnboardingPage(
                imageRes = R.drawable.img_usage_3
            ),
            OnboardingPage(
                imageRes = R.drawable.img_usage_4
            ),
            OnboardingPage(
                imageRes = R.drawable.img_usage_5
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        forceShow = intent?.getBooleanExtra(EXTRA_FORCE_SHOW, false) ?: false

        if (!forceShow && !onboardingPreferences.shouldShowOnboarding()) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_onboarding)

        val root = findViewById<View>(R.id.rootOnboarding)
        viewPager = findViewById(R.id.viewPagerOnboarding)
        tabLayout = findViewById(R.id.onboardingIndicator)
        buttonNext = findViewById(R.id.buttonOnboardingNext)
        buttonSkip = findViewById(R.id.buttonOnboardingSkip)

        applyWindowInsets(root)

        viewPager.adapter = OnboardingPagerAdapter(pages)

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        buttonNext.setOnClickListener {
            if (isLastPage()) {
                completeOnboarding()
            } else {
                viewPager.currentItem = viewPager.currentItem + 1
            }
        }

        buttonSkip.setOnClickListener { completeOnboarding() }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                buttonNext.setText(
                    if (position == pages.lastIndex) {
                        R.string.onboarding_get_started
                    } else {
                        R.string.onboarding_next
                    }
                )
            }
        })
    }

    private fun applyWindowInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun isLastPage(): Boolean = viewPager.currentItem == pages.lastIndex

    private fun completeOnboarding() {
        onboardingPreferences.markCompleted()
        if (forceShow) {
            finish()
        } else {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    companion object {
        const val EXTRA_FORCE_SHOW = "extra_force_show"
    }
}
