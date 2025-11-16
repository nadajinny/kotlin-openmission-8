package com.ndjinny.tagmoa.model

import android.content.Context

object AlarmPreferences {
    private const val PREFS_NAME = "alarm_preferences"
    private const val KEY_MAJOR_ENABLED = "major_alarm_enabled"
    private const val KEY_MAJOR_TIME = "major_alarm_time"
    private const val KEY_SUB_ENABLED = "sub_alarm_enabled"
    private const val KEY_SUB_TIME = "sub_alarm_time"

    private const val DEFAULT_MAJOR_TIME = "08:00"
    private const val DEFAULT_SUB_TIME = "13:00"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isMajorAlarmEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MAJOR_ENABLED, true)

    fun setMajorAlarmEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MAJOR_ENABLED, enabled).apply()
    }

    fun isSubAlarmEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SUB_ENABLED, false)

    fun setSubAlarmEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SUB_ENABLED, enabled).apply()
    }

    fun getMajorAlarmTime(context: Context): String =
        prefs(context).getString(KEY_MAJOR_TIME, DEFAULT_MAJOR_TIME) ?: DEFAULT_MAJOR_TIME

    fun setMajorAlarmTime(context: Context, time: String) {
        prefs(context).edit().putString(KEY_MAJOR_TIME, time).apply()
    }

    fun getSubAlarmTime(context: Context): String =
        prefs(context).getString(KEY_SUB_TIME, DEFAULT_SUB_TIME) ?: DEFAULT_SUB_TIME

    fun setSubAlarmTime(context: Context, time: String) {
        prefs(context).edit().putString(KEY_SUB_TIME, time).apply()
    }
}
