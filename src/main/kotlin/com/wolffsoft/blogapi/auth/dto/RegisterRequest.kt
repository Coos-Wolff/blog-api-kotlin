package com.wolffsoft.blogapi.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @NotBlank
    @Email
    @Size(max = 254)
    val email: String,

    @NotBlank
    @Size(max = 20)
    val name: String,

    @NotBlank
    @Size(min = 12, max = 72)
    val password: String,
)
