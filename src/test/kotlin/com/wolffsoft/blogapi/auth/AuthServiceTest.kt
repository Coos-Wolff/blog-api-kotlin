package com.wolffsoft.blogapi.auth

import com.wolffsoft.blogapi.auth.dto.LoginRequest
import com.wolffsoft.blogapi.auth.dto.RefreshRequest
import com.wolffsoft.blogapi.auth.dto.RegisterRequest
import com.wolffsoft.blogapi.auth.exception.EmailAlreadyExistsException
import com.wolffsoft.blogapi.auth.exception.InvalidTokenException
import com.wolffsoft.blogapi.user.User
import com.wolffsoft.blogapi.user.UserRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import java.time.Duration
import java.time.Instant
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException

@ExtendWith(MockKExtension::class)
class AuthServiceTest {

    companion object {
        const val USER_EMAIL = "user@example.com"
    }

    @MockK
    private lateinit var jwtProperties: JwtProperties

    @MockK
    private lateinit var jwtDecoder: JwtDecoder

    @MockK
    private lateinit var passwordEncoder: PasswordEncoder

    @MockK
    private lateinit var tokenService: TokenService

    @MockK
    private lateinit var authenticationManager: AuthenticationManager

    @MockK
    private lateinit var userRepository: UserRepository

    @InjectMockKs
    private lateinit var authService: AuthService

    private fun jwtWith(token: String, tokenType: String, subject: UUID): Jwt =
        Jwt.withTokenValue(token)
            .header("alg", "HS256")
            .claim("token_type", tokenType)
            .subject(subject.toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build()

    @Test
    fun `Should test existsByEmail throws EmailAlreadyExistsException`() {
        val registerRequest = RegisterRequest("existing@email.com", "Existing user", "plaintext123!!!")
        every { userRepository.existsByEmail(registerRequest.email) } returns true

        assertThatThrownBy {
            authService.register(registerRequest)
        }
            .isInstanceOf(EmailAlreadyExistsException::class.java)
            .hasMessageContaining("Email already exists!")
        verify(exactly = 0) { userRepository.save(any<User>()) }
    }

    @Test
    fun `register saves the user with the password-encoder's output, not the raw password`() {
        val request = RegisterRequest("new@example.com", "New user", "passwordplaintext1234")
        every { userRepository.existsByEmail(request.email) } returns false
        every { passwordEncoder.encode(request.password) } returns "bycrypt%encoded%hash"

        val savedSlot = slot<User>()
        every { userRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        authService.register(request)

        verify { userRepository.save(any<User>()) }
        assertThat(savedSlot.captured.password).isEqualTo("bycrypt%encoded%hash")
        assertThat(savedSlot.captured.email).isEqualTo(request.email)
    }

    @Test
    fun `login propagates BadCredentialsException when authentication fails`() {
        val loginRequest = LoginRequest(USER_EMAIL, "wrongPassword")
        every { authenticationManager.authenticate(any()) } throws BadCredentialsException("Wrong password")

        assertThatThrownBy {
            authService.login(loginRequest)
        }
            .isInstanceOf(BadCredentialsException::class.java)
            .hasMessageContaining("Wrong password")
    }

    @Test
    fun `login returns a TokenResponse with the issued access or refresh tokens, Bearer type, and access-token TTL in seconds`() {
        val loginRequest = LoginRequest(USER_EMAIL, "correctPassword")
        val principal = AppUserDetails(
            id = UUID.randomUUID(),
            email = loginRequest.email,
            password = loginRequest.password,
            isAdmin = false
        )
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        every { authenticationManager.authenticate(any()) } returns auth
        every { tokenService.issueAccessToken(principal) } returns "access-token-value"
        every { tokenService.issueRefreshToken(principal) } returns "refresh-token-value"
        every { jwtProperties.accessTokenTtl } returns Duration.ofSeconds(900)

        val response = authService.login(loginRequest)

        // then
        assertThat(response.accessToken).isEqualTo("access-token-value")
        assertThat(response.refreshToken).isEqualTo("refresh-token-value")
        assertThat(response.tokenType).isEqualTo("Bearer")
        assertThat(response.expiresIn).isEqualTo(900L)
    }

    @Test
    fun `refresh returns a new access token while echoing back the same refresh token when the refresh token is valid`() {
        val userId = UUID.randomUUID()
        val refreshToken = "valid-refresh-token-value"
        val newAccessToken = "new-access-token"
        val refreshRequest = RefreshRequest(refreshToken)
        val decodeJwt = jwtWith(refreshToken, "refresh", userId)
        val user = User(userId, USER_EMAIL, "name", "hashedPassword123!!")

        every { jwtDecoder.decode(refreshToken) } returns decodeJwt
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { tokenService.issueAccessToken(any(AppUserDetails::class)) } returns newAccessToken
        every { jwtProperties.accessTokenTtl } returns Duration.ofSeconds(900)

        val response = authService.refresh(refreshRequest)

        assertThat(response.accessToken).isEqualTo(newAccessToken)
        assertThat(response.refreshToken).isEqualTo(refreshToken)
    }

    @Test
    fun `refresh throws InvalidTokenException when the decoded token's token_type is not refresh (eg an access token)`() {
        val accessTokenValue = "access-token-used-as-refresh"
        val request = RefreshRequest(accessTokenValue)
        val decodedJwt = jwtWith(accessTokenValue, "access", UUID.randomUUID())

        every { jwtDecoder.decode(accessTokenValue) } returns decodedJwt

        assertThatThrownBy {
            authService.refresh(request)
        }
            .isInstanceOf(InvalidTokenException::class.java)
    }

    @Test
    fun `refresh throws InvalidTokenException when the token fails to decode (malformed or expired)`() {
        val malformedToken = "not.a.valid.token";
        val request = RefreshRequest(malformedToken)

        every { jwtDecoder.decode(malformedToken) } throws JwtException("malformed token")

        assertThatThrownBy {
            authService.refresh(request)
        }
        .isInstanceOf(InvalidTokenException::class.java)
    }

    @Test
    fun `refresh throws InvalidTokenException when the valid token's subject does not match any existing user`() {
        val userId = UUID.randomUUID()
        val refreshTokenValue = "valid-token-unknown-user"
        val request = RefreshRequest(refreshTokenValue)
        val decodedJwt = jwtWith(refreshTokenValue, "refresh", userId)

        every { jwtDecoder.decode(refreshTokenValue) } returns decodedJwt
        every { userRepository.findById(userId) } returns Optional.empty()

        assertThatThrownBy {
            authService.refresh(request)
        }
        .isInstanceOf(InvalidTokenException::class.java)
    }
}