package com.wolffsoft.blogapi.blogpost

import com.wolffsoft.blogapi.TestcontainersConfiguration
import com.wolffsoft.blogapi.user.User
import com.wolffsoft.blogapi.user.UserRepository
import java.time.LocalDate
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.jdbc.core.mapping.AggregateReference

@DataJdbcTest
@Import(TestcontainersConfiguration::class)
data class BlogPostRepositoryTest(@Autowired val posts: BlogPostRepository, @Autowired val users: UserRepository) {

    private fun persistedAuthorId(): UUID =
        users.save(
            User(email = "author@example.com", name = "Author", password = "hashed", isAdmin = false),
        ).id

    private fun newPost(
        authorId: UUID,
        title: String = "First post",
        subtitle: String = "A subtitle",
        date: LocalDate = LocalDate.of(2026, 1, 1),
        body: String = "Body text",
        imgUrl: String = "https://example.com/img.png",
    ) = BlogPost(
        title = title,
        subtitle = subtitle,
        date = date,
        body = body,
        imgUrl = imgUrl,
        author = AggregateReference.to(authorId),
    )

    @Test
    fun `saving a new post assigns a version, marking it persisted`() {
        val authorId = persistedAuthorId()

        val saved = posts.save(newPost(authorId))

        assertThat(saved.version).isNotNull()
    }

    @Test
    fun `a saved post is retrievable by its constructor-assigned id`() {
        val authorId = persistedAuthorId()
        val saved = posts.save(newPost(authorId))

        val found = posts.findById(saved.id).orElse(null)

        assertThat(found).isNotNull
        assertThat(found?.title).isEqualTo("First post")
        assertThat(found?.author?.id).isEqualTo(authorId)
    }

    @Test
    fun `findAll with a pageable returns a populated page with correct totals`() {
        val authorId = persistedAuthorId()
        posts.save(newPost(authorId, title = "Post one"))
        posts.save(newPost(authorId, title = "Post two"))
        posts.save(newPost(authorId, title = "Post three"))

        val page = posts.findAll(PageRequest.of(0, 2))

        assertThat(page.content).hasSize(2)
        assertThat(page.totalElements).isEqualTo(3)
        assertThat(page.number).isZero()
    }
}
