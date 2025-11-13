package com.ndjinny.tagmoa.controller

import android.content.Context
import android.net.Uri

/**
 * Persists the user-selected home background URI so we can restore it across launches.
 */
object HomeBackgroundManager {
    private const val PREFS_NAME = "home_background_prefs"
    private const val KEY_BACKGROUND_URI = "key_background_uri"

    fun saveBackgroundUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BACKGROUND_URI, uri.toString())
            .apply()
    }

    fun getBackgroundUri(context: Context): Uri? {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BACKGROUND_URI, null)
        return saved?.let { Uri.parse(it) }
    }

    fun clearBackgroundUri(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_BACKGROUND_URI)
            .apply()
    }
}
