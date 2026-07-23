package com.wolffsoft.blogapi.user

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("users")
data class User(
    @Id val id: UUID = UUID.randomUUID(),
    val email: String,
    val name: String,
    val password: String,
    val isAdmin: Boolean = false,
    @Version val version: Long? = null
) {
    companion object {
        fun create(email: String, name: String, hashedPassword: String): User {
            return User(email = email, name = name, password = hashedPassword)
        }
    }
}
