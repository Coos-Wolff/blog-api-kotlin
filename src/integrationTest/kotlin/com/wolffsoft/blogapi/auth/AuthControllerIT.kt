package com.wolffsoft.blogapi.auth

import com.wolffsoft.blogapi.IntegrationTestBase
import com.wolffsoft.blogapi.IntegrationTestConstants.EMAIL
import com.wolffsoft.blogapi.IntegrationTestConstants.LOGIN_URI
import com.wolffsoft.blogapi.IntegrationTestConstants.NAME
import com.wolffsoft.blogapi.IntegrationTestConstants.PASSWORD_CORRECT
import com.wolffsoft.blogapi.IntegrationTestConstants.REFRESH_URI
import com.wolffsoft.blogapi.IntegrationTestConstants.REGISTER_URI
import com.wolffsoft.blogapi.auth.dto.LoginRequest
import com.wolffsoft.blogapi.auth.dto.RefreshRequest
import com.wolffsoft.blogapi.auth.dto.RegisterRequest
import com.wolffsoft.blogapi.auth.dto.TokenResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.expectBody

class AuthControllerIT: IntegrationTestBase() {

    @Test
    fun`Should test register successful`() {
        val registerRequest = RegisterRequest(EMAIL, NAME, PASSWORD_CORRECT)

        register(registerRequest)
    }

    @Test
    fun `Should test duplicate email on register`() {
        val registerRequest = RegisterRequest(EMAIL, NAME, PASSWORD_CORRECT)

        register(registerRequest)

        restTestClient.post()
            .uri(REGISTER_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerRequest)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `Should test too short password on register`() {
        val request = RegisterRequest(EMAIL, NAME, "too-short")

        restTestClient.post()
            .uri(REGISTER_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
    }

    @Test
    fun `Should test no name on register`() {
        val request = RegisterRequest(EMAIL, "", PASSWORD_CORRECT)

        restTestClient.post()
            .uri(REGISTER_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
    }

    @Test
    fun `Should test wrong email format on register`() {
        val request = RegisterRequest("email-wrong-format.nl", NAME, PASSWORD_CORRECT)

        restTestClient.post()
            .uri(REGISTER_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
    }

    @Test
    fun `Should test login success`() {
        val registerRequest = RegisterRequest(EMAIL, NAME, PASSWORD_CORRECT)
        val loginRequest = LoginRequest(EMAIL, PASSWORD_CORRECT)

        register(registerRequest)

        restTestClient.post()
            .uri(LOGIN_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(loginRequest)
            .exchange()
            .expectStatus()
            .isOk()
    }

    @Test
    fun `Should test wrong password on login`() {
        val registerRequest = RegisterRequest(EMAIL, NAME, PASSWORD_CORRECT)
        val loginRequest = LoginRequest(EMAIL, "some-other-password")

        register(registerRequest)

        restTestClient.post()
            .uri(LOGIN_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(loginRequest)
            .exchange()
            .expectStatus()
            .isUnauthorized()
    }

    @Test
    fun `Should test refresh success`() {
        val registerRequest = RegisterRequest(EMAIL, NAME, PASSWORD_CORRECT)
        val loginRequest = LoginRequest(EMAIL, PASSWORD_CORRECT)

        register(registerRequest)

        val tokenResponseFromLogin: TokenResponse = requireNotNull(restTestClient.post()
            .uri(LOGIN_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(loginRequest)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody<TokenResponse>()
            .returnResult()
            .getResponseBody()) { "Login should return a token response body" }

        val refreshRequest = RefreshRequest(tokenResponseFromLogin.refreshToken)

        val tokenResponseFromRefresh = requireNotNull(restTestClient.post()
            .uri(REFRESH_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(refreshRequest)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody<TokenResponse>()
            .returnResult()
            .getResponseBody()) { "Login should return a token response body" }

        assertThat(tokenResponseFromRefresh.accessToken).isNotNull()
        assertThat(tokenResponseFromRefresh.refreshToken).isNotNull()
        assertThat(tokenResponseFromLogin.refreshToken).isEqualTo(tokenResponseFromRefresh.refreshToken)
    }

    @Test
    fun `Should test unauthorized on refresh with an access token`() {
        val registerRequest = RegisterRequest(EMAIL, NAME, PASSWORD_CORRECT)
        val loginRequest = LoginRequest(EMAIL, PASSWORD_CORRECT)

        restTestClient.post()
            .uri(REGISTER_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerRequest)
            .exchange()
            .expectStatus()
            .isCreated()

        val tokenResponse: TokenResponse = requireNotNull( restTestClient.post()
            .uri(LOGIN_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(loginRequest)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody<TokenResponse>()
            .returnResult()
            .getResponseBody()) { "Login should return a token response body" }

        val refreshRequest = RefreshRequest(tokenResponse.accessToken)

        restTestClient.post()
            .uri(REFRESH_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(refreshRequest)
            .exchange()
            .expectStatus()
            .isUnauthorized()
    }

    @Test
    fun `Should test unauthorized on refresh with garbage token`() {
        val refreshRequest = RefreshRequest("garbage.token.now")

        restTestClient.post()
            .uri(REFRESH_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(refreshRequest)
            .exchange()
            .expectStatus()
            .isUnauthorized()
    }

    private fun register(request: RegisterRequest) =
        restTestClient.post().uri(REGISTER_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isCreated()
}