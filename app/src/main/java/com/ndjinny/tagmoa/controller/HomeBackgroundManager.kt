package com.ndjinny.tagmoa.controller

import android.content.Context
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.ndjinny.tagmoa.R

/**
 * Persists the user-selected home background URI so we can restore it across launches.
 */
object HomeBackgroundManager {
    private const val PREFS_NAME = "home_background_prefs"
    private const val KEY_BACKGROUND_URI = "key_background_uri"
    private const val KEY_DATE_TEXT_COLOR = "key_date_text_color"

    fun saveBackgroundUri(context: Context, uri: Uri) {
        getPrefs(context)
            .edit()
            .putString(KEY_BACKGROUND_URI, uri.toString())
            .apply()
    }

    fun getBackgroundUri(context: Context): Uri? {
        val saved = getPrefs(context)
            .getString(KEY_BACKGROUND_URI, null)
        return saved?.let { Uri.parse(it) }
    }

    fun clearBackgroundUri(context: Context) {
        getPrefs(context)
            .edit()
            .remove(KEY_BACKGROUND_URI)
            .apply()
    }

    fun saveDateTextColor(context: Context, color: HomeDateTextColor) {
        getPrefs(context)
            .edit()
            .putString(KEY_DATE_TEXT_COLOR, color.prefValue)
            .apply()
    }

    fun getDateTextColor(context: Context): HomeDateTextColor {
        val saved = getPrefs(context).getString(KEY_DATE_TEXT_COLOR, null)
        return HomeDateTextColor.fromPref(saved)
    }

    fun getDateTextColorInt(context: Context): Int {
        val colorRes = getDateTextColor(context).colorRes
        return ContextCompat.getColor(context, colorRes)
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

enum class HomeDateTextColor(val prefValue: String, @ColorRes val colorRes: Int) {
    DARK("dark", android.R.color.black),
    LIGHT("light", android.R.color.white);

    companion object {
        fun fromPref(value: String?): HomeDateTextColor {
            return values().firstOrNull { it.prefValue == value } ?: DARK
        }
    }

    fun isLight(): Boolean = this == LIGHT
    fun isDark(): Boolean = this == DARK
}
