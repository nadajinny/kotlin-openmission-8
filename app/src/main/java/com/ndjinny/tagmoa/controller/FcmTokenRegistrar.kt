package com.ndjinny.tagmoa.controller

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.ndjinny.tagmoa.model.SessionManager
import com.ndjinny.tagmoa.model.UserDatabase

object FcmTokenRegistrar {

    private const val PREFS_NAME = "fcm_token_prefs"
    private const val KEY_PENDING_TOKEN = "pending_token"

    fun ensureCurrentTokenSynced(context: Context) {
        flushPendingToken(context)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FcmTokenRegistrar", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result ?: return@addOnCompleteListener
            registerToken(context, token)
        }
    }

    fun registerToken(context: Context, token: String) {
        val session = SessionManager.currentSession
        if (session == null) {
            storePendingToken(context, token)
            return
        }
        UserDatabase.fcmTokensRef(session.uid).child(token).setValue(true)
            .addOnSuccessListener { clearPendingToken(context, token) }
            .addOnFailureListener { exception ->
                Log.w("FcmTokenRegistrar", "Failed to save token", exception)
                storePendingToken(context, token)
            }
    }

    private fun flushPendingToken(context: Context) {
        val prefs = prefs(context)
        val token = prefs.getString(KEY_PENDING_TOKEN, null) ?: return
        if (SessionManager.currentSession == null) return
        registerToken(context, token)
    }

    private fun storePendingToken(context: Context, token: String) {
        prefs(context).edit().putString(KEY_PENDING_TOKEN, token).apply()
    }

    private fun clearPendingToken(context: Context, token: String) {
        val prefs = prefs(context)
        if (prefs.getString(KEY_PENDING_TOKEN, null) == token) {
            prefs.edit().remove(KEY_PENDING_TOKEN).apply()
        }
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
