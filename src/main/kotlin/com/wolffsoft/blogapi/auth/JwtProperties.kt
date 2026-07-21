package com.wolffsoft.blogapi.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "jwt")
@Validated
data class JwtProperties(
    @NotBlank
    @Size(min = 32, message = "jwt.secret must be at least 32 characters for HS256")
    val secret: String,
    val accessTokenTtl: Duration,
    val refreshTokenTtl: Duration
)
