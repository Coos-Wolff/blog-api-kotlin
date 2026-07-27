package com.wolffsoft.blogapi.auth

import com.wolffsoft.blogapi.auth.Roles.ROLE_ADMIN
import java.util.UUID
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class CurrentUserProvider {

    fun currentUserId(): UUID = UUID.fromString(requireJwtAuthentication().name)

    fun isCurrentUserAdmin(): Boolean =
        requireJwtAuthentication().authorities.any { it.authority == ROLE_ADMIN }

    private fun requireJwtAuthentication(): JwtAuthenticationToken {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication is JwtAuthenticationToken) {
            return authentication
        }
        throw IllegalStateException("No authenticated JWT principal present")
    }
}