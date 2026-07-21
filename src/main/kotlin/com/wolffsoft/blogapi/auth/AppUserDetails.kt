package com.wolffsoft.blogapi.auth

import com.wolffsoft.blogapi.auth.Roles.ROLE_ADMIN
import com.wolffsoft.blogapi.auth.Roles.ROLE_USER
import com.wolffsoft.blogapi.user.User
import java.util.UUID
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class AppUserDetails(
    val id: UUID,
    val email: String,
    private val password: String,
    val isAdmin: Boolean) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority(if (isAdmin) ROLE_ADMIN else ROLE_USER))

    override fun getPassword(): String = password
    override fun getUsername(): String = email
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    companion object {
        fun from(user: User): AppUserDetails {
            return AppUserDetails(
                id = user.id,
                email = user.email,
                password = user.password,
                isAdmin = user.isAdmin
            )
        }
    }
}
