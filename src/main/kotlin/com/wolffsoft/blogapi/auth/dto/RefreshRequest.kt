package com.wolffsoft.blogapi.auth.dto

import jakarta.validation.constraints.NotBlank

data class RefreshRequest(
    @NotBlank
    val refreshToken: String,
)
