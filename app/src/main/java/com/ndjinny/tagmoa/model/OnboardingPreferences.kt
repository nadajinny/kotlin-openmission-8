package com.ndjinny.tagmoa.model

import android.content.Context
import android.content.SharedPreferences

class OnboardingPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldShowOnboarding(): Boolean = !prefs.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "onboarding_preferences"
        private const val KEY_COMPLETED = "key_onboarding_completed"
    }
}
