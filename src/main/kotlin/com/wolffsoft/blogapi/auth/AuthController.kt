package com.wolffsoft.blogapi.auth

import com.wolffsoft.blogapi.auth.dto.LoginRequest
import com.wolffsoft.blogapi.auth.dto.RefreshRequest
import com.wolffsoft.blogapi.auth.dto.RegisterRequest
import com.wolffsoft.blogapi.auth.dto.TokenResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody registerRequest: RegisterRequest): ResponseEntity<Void> {
        authService.register(registerRequest)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<TokenResponse> {
        val tokenResponse = authService.login(loginRequest)
        return ResponseEntity.ok(tokenResponse)
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody refreshRequest: RefreshRequest): ResponseEntity<TokenResponse> {
        val tokenResponse = authService.refresh(refreshRequest)
        return ResponseEntity.ok(tokenResponse)
    }
}