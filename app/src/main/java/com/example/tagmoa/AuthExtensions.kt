package com.example.tagmoa

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

fun AppCompatActivity.requireUserIdOrRedirect(): String? {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser != null) {
        return currentUser.uid
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

fun Fragment.requireUserIdOrRedirect(): String? {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser != null) {
        return currentUser.uid
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
