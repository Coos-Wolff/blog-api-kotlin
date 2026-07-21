package com.wolffsoft.blogapi.user

import com.wolffsoft.blogapi.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.context.annotation.Import

@DataJdbcTest
@Import(TestcontainersConfiguration::class)
class UserRepositoryTest(@Autowired val users: UserRepository) {

    private fun newUser(
        email: String = "coos@example.com",
        name: String = "Coos",
        password: String = "hashed",
        isAdmin: Boolean = false,
    ) = User(
        email = email,
        name = name,
        password = password,
        isAdmin = isAdmin,
    )

    @Test
    fun `findByEmail returns the user when one exists with that email`() {
        val saved = users.save(newUser())
        val found = users.findByEmail("coos@example.com")

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(saved.id)
    }

    @Test
    fun `findByEmail returns null when no user has that email`() {
        val found = users.findByEmail("nobody@example.com")

        assertThat(found).isNull()
    }

    @Test
    fun `existsByEmail is true when a user with that email exists`() {
        users.save(newUser())

        assertThat(users.existsByEmail("coos@example.com")).isTrue()
    }

    @Test
    fun `existsByEmail is false when no user has that email`() {
        assertThat(users.existsByEmail("nobody@example.com")).isFalse()
    }
}