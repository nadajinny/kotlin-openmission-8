package com.example.tagmoa

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

object UserDatabase {
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    private fun userRoot(uid: String): DatabaseReference {
        return database.getReference("users").child(uid)
    }

    fun tasksRef(uid: String): DatabaseReference = userRoot(uid).child("mainTasks")

    fun tagsRef(uid: String): DatabaseReference = userRoot(uid).child("tags")

    fun subTasksRef(uid: String): DatabaseReference = userRoot(uid).child("subTasks")

    fun upsertUserProfile(user: FirebaseUser) {
        val profile = mapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "lastLoginAt" to ServerValue.TIMESTAMP
        )
        userRoot(user.uid).child("profile").updateChildren(profile)
    }
}
