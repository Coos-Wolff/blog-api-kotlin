package com.wolffsoft.blogapi.auth

import com.wolffsoft.blogapi.auth.TokenClaims.CLAIM_TOKEN_TYPE
import com.wolffsoft.blogapi.auth.TokenClaims.TYPE_REFRESH
import com.wolffsoft.blogapi.auth.dto.LoginRequest
import com.wolffsoft.blogapi.auth.dto.RefreshRequest
import com.wolffsoft.blogapi.auth.dto.RegisterRequest
import com.wolffsoft.blogapi.auth.dto.TokenResponse
import com.wolffsoft.blogapi.auth.exception.EmailAlreadyExistsException
import com.wolffsoft.blogapi.auth.exception.InvalidTokenException
import com.wolffsoft.blogapi.user.User
import com.wolffsoft.blogapi.user.UserRepository
import java.util.Objects
import java.util.Optional
import java.util.UUID
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val jwtProperties: JwtProperties,
    private val jwtDecoder: JwtDecoder,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository
) {
    companion object {
        const val TOKEN_TYPE_BEARER = "Bearer"
        const val INVALID_TOKEN_MESSAGE = "Invalid token"
    }

    fun register(request: RegisterRequest) {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException("Email already exists!")
        }
        val hashedPassword = passwordEncoder.encode(request.password)
            ?: error("PasswordEncoder returned null for a non-null password")
        val user = User.create(request.email, request.name, hashedPassword)
        userRepository.save(user)
    }

    fun login(request: LoginRequest): TokenResponse {
        val authentication: Authentication =
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email, request.password))
        val principal = authentication.principal as AppUserDetails
        val accessToken = tokenService.issueAccessToken(principal)
        val refreshToken = tokenService.issueRefreshToken(principal)
        return buildTokenResponse(accessToken, refreshToken)
    }

    fun refresh(request: RefreshRequest): TokenResponse {
        val jwt = decodeRefreshToken(request.refreshToken)
        val userId = UUID.fromString(jwt.subject)
        val user = userRepository.findById(userId).orElseThrow { throw InvalidTokenException(INVALID_TOKEN_MESSAGE) }
        val userDetails = AppUserDetails.from(user)
        val accessToken = tokenService.issueAccessToken(userDetails)
        return buildTokenResponse(accessToken, request.refreshToken)
    }

    private fun decodeRefreshToken(token: String): Jwt {
        val jwt = try {
            jwtDecoder.decode(token)
        } catch (ex: JwtException) {
            throw InvalidTokenException(INVALID_TOKEN_MESSAGE)
        }
        if (TYPE_REFRESH != jwt.getClaimAsString(CLAIM_TOKEN_TYPE)) {
            throw InvalidTokenException(INVALID_TOKEN_MESSAGE)
        }
        return jwt
    }

    private fun buildTokenResponse(accessToken: String, refreshToken: String): TokenResponse =
        TokenResponse(accessToken, refreshToken, TOKEN_TYPE_BEARER, jwtProperties.accessTokenTtl.seconds)
}