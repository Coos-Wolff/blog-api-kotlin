package com.wolffsoft.blogapi.auth

import com.wolffsoft.blogapi.auth.TokenClaims.CLAIM_ROLES
import com.wolffsoft.blogapi.auth.TokenClaims.CLAIM_TOKEN_TYPE
import com.wolffsoft.blogapi.auth.TokenClaims.TYPE_ACCESS
import com.wolffsoft.blogapi.auth.TokenClaims.TYPE_REFRESH
import java.time.Duration
import java.time.Instant
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service

@Service
class TokenService(private val jwtEncoder: JwtEncoder, private val jwtProperties: JwtProperties) {

    private val jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build()

    fun issueAccessToken(user: AppUserDetails): String {
        return buildToken(TYPE_ACCESS, user, jwtProperties.accessTokenTtl)
    }

    fun issueRefreshToken(user: AppUserDetails): String {
        return buildToken(TYPE_REFRESH, user, jwtProperties.refreshTokenTtl)
    }

    private fun buildToken(tokenType: String, user: AppUserDetails, expiresIn: Duration): String {
        val jwtClaimsSet = buildJwtClaimsSet(tokenType, user, expiresIn)
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, jwtClaimsSet)).tokenValue
    }

    private fun buildJwtClaimsSet(tokenType: String, user: AppUserDetails, expiresIn: Duration): JwtClaimsSet {
        val now = Instant.now()
        val builder = JwtClaimsSet.builder()
            .subject(user.id.toString())
            .issuedAt(now)
            .expiresAt(now.plus(expiresIn))
            .claim(CLAIM_TOKEN_TYPE, tokenType)

        addRolesForAccessToken(tokenType, builder, user)

        return builder.build()
    }

    private fun extractAuthorities(authorities: Collection<GrantedAuthority>): List<String> = authorities.map {
        it.authority ?: error("Authority returned null") }

    private fun addRolesForAccessToken(tokenType: String, builder: JwtClaimsSet.Builder, user: AppUserDetails) {
        if (TYPE_ACCESS == tokenType) {
            val authorities = extractAuthorities(user.authorities)
            builder.claim(CLAIM_ROLES, authorities)
        }
    }
}