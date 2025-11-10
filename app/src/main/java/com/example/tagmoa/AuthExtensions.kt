package com.example.tagmoa

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

fun AppCompatActivity.requireUserIdOrRedirect(): String? {
    val session = currentSessionOrRedirect() ?: return null
    if (session.provider == AuthProvider.GOOGLE && FirebaseAuth.getInstance().currentUser == null) {
        SessionManager.clearSession()
        return currentSessionOrRedirect()?.uid
    }
    return session.uid
}

fun Fragment.requireUserIdOrRedirect(): String? {
    val session = SessionManager.currentSession
    if (session != null) {
        if (session.provider == AuthProvider.GOOGLE && FirebaseAuth.getInstance().currentUser == null) {
            SessionManager.clearSession()
        } else {
            return session.uid
        }
    }
    val activity = activity ?: return null
    val intent = Intent(activity, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    startActivity(intent)
    activity.finish()
    return null
}

private fun AppCompatActivity.currentSessionOrRedirect(): UserSession? {
    val session = SessionManager.currentSession
    if (session != null) {
        return session
    }
    val intent = Intent(this, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    startActivity(intent)
    finish()
    return null
}
