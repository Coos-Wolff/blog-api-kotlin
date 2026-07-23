package com.wolffsoft.blogapi.user

import java.util.UUID
import org.springframework.data.repository.ListCrudRepository

interface UserRepository : ListCrudRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}