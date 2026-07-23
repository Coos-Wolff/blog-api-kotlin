package com.wolffsoft.blogapi.auth.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @NotBlank
    val email: String,

    @NotBlank
    val password: String,
)
