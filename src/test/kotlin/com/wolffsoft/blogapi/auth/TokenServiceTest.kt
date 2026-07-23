package com.wolffsoft.blogapi.auth

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.wolffsoft.blogapi.auth.TokenClaims.CLAIM_ROLES
import com.wolffsoft.blogapi.auth.TokenClaims.CLAIM_TOKEN_TYPE
import com.wolffsoft.blogapi.auth.TokenClaims.TYPE_ACCESS
import com.wolffsoft.blogapi.auth.TokenClaims.TYPE_REFRESH
import java.time.Duration
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder

class TokenServiceTest {

    private val secret = "test-secret-at-least-32-characters-long-padding"
    private val jwtProperties = JwtProperties(
        secret = secret,
        accessTokenTtl = Duration.ofMinutes(15),
        refreshTokenTtl = Duration.ofDays(7)
    )

    private val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
    private val encoder = NimbusJwtEncoder(ImmutableSecret(secretKey))
    private val decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build()

    private val tokenService = TokenService(encoder, jwtProperties)

    private fun user(isAdmin: Boolean = false) =
        AppUserDetails(id = UUID.randomUUID(), email = "a@b.com", password = "x", isAdmin = isAdmin)

    @Test
    fun `access token carries token type and the user's roles`() {
        val user = user(isAdmin = false)
        val jwt = decoder.decode(tokenService.issueAccessToken(user))

        assertThat(jwt.getClaimAsString(CLAIM_TOKEN_TYPE)).isEqualTo(TYPE_ACCESS)
        assertThat(jwt.subject).isEqualTo(user.id.toString())
        assertThat(jwt.getClaimAsStringList(CLAIM_ROLES)).containsExactly("ROLE_USER")
    }

    @Test
    fun `refresh token carries token_type refresh and no roles`() {
        val jwt = decoder.decode(tokenService.issueRefreshToken(user()))
        assertThat(jwt.getClaimAsString(CLAIM_TOKEN_TYPE)).isEqualTo(TYPE_REFRESH)
        assertThat(jwt.getClaimAsStringList(CLAIM_ROLES)).isNull()
        assertThat(jwt.claims).doesNotContainKey(CLAIM_ROLES)
    }

    @Test
    fun `admin access token carries ROLE_ADMIN`() {
        val user = user(isAdmin = true)
        val jwt = decoder.decode(tokenService.issueAccessToken(user))
        assertThat(jwt.getClaimAsStringList(CLAIM_ROLES)).containsExactly("ROLE_ADMIN")
    }
}