package com.wolffsoft.blogapi.auth.dto

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long
)
