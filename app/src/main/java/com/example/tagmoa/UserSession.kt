package com.example.tagmoa

enum class AuthProvider {
    GOOGLE,
    KAKAO
}

data class UserSession(
    val provider: AuthProvider,
    val uid: String,
    val displayName: String? = null,
    val email: String? = null
)
