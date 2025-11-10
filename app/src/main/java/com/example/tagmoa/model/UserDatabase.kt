package com.example.tagmoa.model

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

    fun upsertUserProfile(session: UserSession) {
        val profile = mapOf(
            "uid" to session.uid,
            "email" to (session.email ?: ""),
            "displayName" to (session.displayName ?: ""),
            "provider" to session.provider.name,
            "lastLoginAt" to ServerValue.TIMESTAMP
        )
        userRoot(session.uid).child("profile").updateChildren(profile)
    }
}
