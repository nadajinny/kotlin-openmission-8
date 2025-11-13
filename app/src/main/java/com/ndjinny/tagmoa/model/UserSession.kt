package com.ndjinny.tagmoa.model

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
