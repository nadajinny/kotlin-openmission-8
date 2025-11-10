package com.example.tagmoa

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREFS_NAME = "user_session"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_UID = "uid"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private lateinit var prefs: SharedPreferences

    var currentSession: UserSession? = null
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentSession = loadFromPrefs()
    }

    fun setSession(session: UserSession) {
        currentSession = session
        prefs.edit()
            .putString(KEY_PROVIDER, session.provider.name)
            .putString(KEY_UID, session.uid)
            .putString(KEY_NAME, session.displayName)
            .putString(KEY_EMAIL, session.email)
            .apply()
    }

    fun clearSession() {
        currentSession = null
        if (::prefs.isInitialized) {
            prefs.edit().clear().apply()
        }
    }

    private fun loadFromPrefs(): UserSession? {
        if (!::prefs.isInitialized) return null
        val providerName = prefs.getString(KEY_PROVIDER, null) ?: return null
        val uid = prefs.getString(KEY_UID, null) ?: return null
        val provider = runCatching { AuthProvider.valueOf(providerName) }.getOrNull() ?: return null
        val name = prefs.getString(KEY_NAME, null)
        val email = prefs.getString(KEY_EMAIL, null)
        return UserSession(provider, uid, name, email)
    }
}
