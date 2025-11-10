package com.example.tagmoa

enum class AuthProvider {
    GOOGLE,
    KAKAO,
    NAVER
}

data class UserSession(
    val provider: AuthProvider,
    val uid: String,
    val displayName: String? = null,
    val email: String? = null
)
