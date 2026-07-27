package com.wolffsoft.blogapi.blogpost

import com.wolffsoft.blogapi.auth.CurrentUserProvider
import com.wolffsoft.blogapi.auth.exception.InvalidTokenException
import com.wolffsoft.blogapi.blogpost.dto.CreateBlogPostRequest
import com.wolffsoft.blogapi.blogpost.dto.UpdateBlogPostRequest
import com.wolffsoft.blogapi.blogpost.exception.BlogPostNotFoundException
import com.wolffsoft.blogapi.exception.ForbiddenException
import com.wolffsoft.blogapi.user.User
import com.wolffsoft.blogapi.user.UserRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jdbc.core.mapping.AggregateReference

@ExtendWith(MockKExtension::class)
class BlogPostServiceTest {

    companion object {
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val BODY = "body"
        const val IMG_URL = "imgUrl"
        const val AUTHOR_NAME = "authorName"
        const val EMAIL = "user@email.com"
        const val CORRECT_PASSWORD = "password.correct.123!"
    }

    private val currentUserId: UUID = UUID.randomUUID()
    private val user: User = User(
        id = currentUserId,
        name = AUTHOR_NAME,
        email = EMAIL,
        password = CORRECT_PASSWORD,
    )
    @MockK
    private lateinit var blogPostRepository: BlogPostRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var currentUserProvider: CurrentUserProvider

    @InjectMockKs
    private lateinit var blogPostService: BlogPostService

    private fun buildBlogPost(
        id: UUID = UUID.randomUUID(),
        authorId: UUID = currentUserId,
        version: Long? = 0L,
        title: String = TITLE,
        subtitle: String = SUBTITLE,
        body: String = BODY,
        imgUrl: String = IMG_URL,
        date: LocalDate = LocalDate.now(),
    ): BlogPost = BlogPost(
        id = id,
        title = title,
        subtitle = subtitle,
        date = date,
        body = body,
        imgUrl = imgUrl,
        author = AggregateReference.to(authorId),
        version = version,
    )

    private fun createRequest() = CreateBlogPostRequest(TITLE, SUBTITLE, BODY, IMG_URL)

    private fun update(
        title: String? = null,
        subtitle: String? = null,
        body: String? = null,
        imgUrl: String? = null,
    ) = UpdateBlogPostRequest(title = title, subtitle = subtitle, body = body, imgUrl = imgUrl)

    private fun buildUser(id: UUID = UUID.randomUUID(), name: String = AUTHOR_NAME) =
        User(id = id, name = name, email = "$name@email.com", password = CORRECT_PASSWORD)

    @Test
    fun `Should test create blog post successfully`() {
        every { currentUserProvider.currentUserId() } returns currentUserId
        every { userRepository.findById(currentUserId)} returns Optional.of(user)

        val request = createRequest()

        val savedSlot = slot<BlogPost>()
        every { blogPostRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        blogPostService.create(request)

        verify(exactly = 1) { blogPostRepository.save(any<BlogPost>()) }

        assertThat(savedSlot.captured.title).isEqualTo(TITLE)
        assertThat(savedSlot.captured.subtitle).isEqualTo(SUBTITLE)
        assertThat(savedSlot.captured.body).isEqualTo(BODY)
        assertThat(savedSlot.captured.imgUrl).isEqualTo(IMG_URL)
        assertThat(savedSlot.captured.author.id).isEqualTo(currentUserId)
    }

    @Test
    fun `Should throw InvalidTokenException when creating a post as a deleted user`() {
        every { currentUserProvider.currentUserId() } returns currentUserId
        every { userRepository.findById(currentUserId) } returns Optional.empty()

        val request = createRequest()

        assertThatThrownBy { blogPostService.create(request) }
            .isInstanceOf(InvalidTokenException::class.java)
    }

    @Test
    fun `Should return blog post with resolved author when found`() {
        val postId = UUID.randomUUID()
        val post = buildBlogPost(id = postId, authorId = currentUserId)
        every { blogPostRepository.findById(postId) } returns Optional.of(post)
        every { userRepository.findById(currentUserId) } returns Optional.of(user)

        val response = blogPostService.getById(postId)

        verify(exactly = 1) { userRepository.findById(currentUserId) }
        assertThat(response.id).isEqualTo(post.id)
        assertThat(response.title).isEqualTo(post.title)
        assertThat(response.subtitle).isEqualTo(post.subtitle)
        assertThat(response.body).isEqualTo(post.body)
        assertThat(response.imgUrl).isEqualTo(post.imgUrl)
        assertThat(response.date).isEqualTo(post.date)
        assertThat(response.author.name).isEqualTo(user.name)
    }

    @Test
    fun `Should throw BlogPostNotFoundException when getting a post that does not exist`() {
        val postId = UUID.randomUUID()
        every { blogPostRepository.findById(postId) } returns Optional.empty()

        assertThatThrownBy { blogPostService.getById(postId) }
            .isInstanceOf(BlogPostNotFoundException::class.java)
    }

    @Test
    fun `Should batch fetch authors once for getAll instead of one lookup per post`() {
        val otherAuthorId = UUID.randomUUID()
        val otherAuthor = buildUser(otherAuthorId, "Other Author")

        val post1 = buildBlogPost(authorId = currentUserId)
        val post2 = buildBlogPost(authorId = otherAuthorId)

        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(listOf(post1, post2), pageable, 2)
        every { blogPostRepository.findAll(pageable) } returns page
        every { userRepository.findAllById(any()) } returns listOf(user, otherAuthor)

        val result = blogPostService.findAll(pageable)

        verify(exactly = 1) { userRepository.findAllById(any()) }
        verify(exactly = 0) { userRepository.findById(any()) }
        assertThat(result.content).hasSize(2)
        assertThat(result.content[0].author.name).isEqualTo(user.name)
        assertThat(result.content[1].author.name).isEqualTo(otherAuthor.name)
    }

    @Test
    fun `Should preserve page ordering when mapping blog posts to responses`() {
        val secondAuthorId = UUID.randomUUID()
        val secondAuthor = buildUser(secondAuthorId, "Second Author")
        val thirdAuthorId = UUID.randomUUID()
        val thirdAuthor = buildUser(thirdAuthorId, "Third Author")

        val post1 = buildBlogPost(title = "First", authorId = currentUserId)
        val post2 = buildBlogPost(title = "Second", authorId = secondAuthorId)
        val post3 = buildBlogPost(title = "Third", authorId = thirdAuthorId)

        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(listOf(post1, post2, post3), pageable, 3)
        every { blogPostRepository.findAll(pageable) } returns page
        every { userRepository.findAllById(any()) } returns listOf(thirdAuthor, user, secondAuthor)

        val result = blogPostService.findAll(pageable)

        assertThat(result.content.map { it.title }).containsExactly("First", "Second", "Third")
    }

    @Test
    fun `Should patch only provided fields while preserving id and version`() {
        val postId = UUID.randomUUID()
        val original = buildBlogPost(id = postId, authorId = currentUserId, version = 0L)
        every { blogPostRepository.findById(postId) } returns Optional.of(original)
        every { currentUserProvider.currentUserId() } returns currentUserId
        every { userRepository.findById(currentUserId) } returns Optional.of(user)

        val savedSlot = slot<BlogPost>()
        every { blogPostRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val request = update(title = "New Title")

        blogPostService.update(postId, request)

        val saved = savedSlot.captured
        assertThat(saved.title).isEqualTo("New Title")
        assertThat(saved.subtitle).isEqualTo(original.subtitle)
        assertThat(saved.body).isEqualTo(original.body)
        assertThat(saved.imgUrl).isEqualTo(original.imgUrl)
        assertThat(saved.id).isEqualTo(original.id)
        assertThat(saved.version).isEqualTo(original.version)
    }

    @Test
    fun `Should patch only the subtitle when only subtitle is provided`() {
        val postId = UUID.randomUUID()
        val original = buildBlogPost(id = postId, authorId = currentUserId, version = 0L)
        every { blogPostRepository.findById(postId) } returns Optional.of(original)
        every { currentUserProvider.currentUserId() } returns currentUserId
        every { userRepository.findById(currentUserId) } returns Optional.of(user)

        val savedSlot = slot<BlogPost>()
        every { blogPostRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val request = update(subtitle = "New Subtitle")

        blogPostService.update(postId, request)

        val saved = savedSlot.captured
        assertThat(saved.subtitle).isEqualTo("New Subtitle")
        assertThat(saved.title).isEqualTo(original.title)
        assertThat(saved.body).isEqualTo(original.body)
        assertThat(saved.imgUrl).isEqualTo(original.imgUrl)
        assertThat(saved.id).isEqualTo(original.id)
        assertThat(saved.version).isEqualTo(original.version)
    }

    @Test
    fun `Should patch only the body when only body is provided`() {
        val postId = UUID.randomUUID()
        val original = buildBlogPost(id = postId, authorId = currentUserId, version = 0L)
        every { blogPostRepository.findById(postId) } returns Optional.of(original)
        every { currentUserProvider.currentUserId() } returns currentUserId
        every { userRepository.findById(currentUserId) } returns Optional.of(user)

        val savedSlot = slot<BlogPost>()
        every { blogPostRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val request = update(body = "New Body")

        blogPostService.update(postId, request)

        val saved = savedSlot.captured
        assertThat(saved.body).isEqualTo("New Body")
        assertThat(saved.title).isEqualTo(original.title)
        assertThat(saved.subtitle).isEqualTo(original.subtitle)
        assertThat(saved.imgUrl).isEqualTo(original.imgUrl)
        assertThat(saved.id).isEqualTo(original.id)
        assertThat(saved.version).isEqualTo(original.version)
    }

    @Test
    fun `Should patch only the imgUrl when only imgUrl is provided`() {
        val postId = UUID.randomUUID()
        val original = buildBlogPost(id = postId, authorId = currentUserId, version = 0L)
        every { blogPostRepository.findById(postId) } returns Optional.of(original)
        every { currentUserProvider.currentUserId() } returns currentUserId
        every { userRepository.findById(currentUserId) } returns Optional.of(user)

        val savedSlot = slot<BlogPost>()
        every { blogPostRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val request = update(imgUrl = "new-img-url")

        blogPostService.update(postId, request)

        val saved = savedSlot.captured
        assertThat(saved.imgUrl).isEqualTo("new-img-url")
        assertThat(saved.title).isEqualTo(original.title)
        assertThat(saved.subtitle).isEqualTo(original.subtitle)
        assertThat(saved.body).isEqualTo(original.body)
        assertThat(saved.id).isEqualTo(original.id)
        assertThat(saved.version).isEqualTo(original.version)
    }

    @Test
    fun `Should throw BlogPostNotFoundException on update before checking ownership`() {
        val postId = UUID.randomUUID()
        every { blogPostRepository.findById(postId) } returns Optional.empty()

        val request = update(title = "New Title")

        assertThatThrownBy { blogPostService.update(postId, request) }
            .isInstanceOf(BlogPostNotFoundException::class.java)

        verify(exactly = 0) { currentUserProvider.isCurrentUserAdmin() }
    }

    @Test
    fun `Should throw ForbiddenException when updating a post owned by another non-admin user`() {
        val postId = UUID.randomUUID()
        val otherAuthorId = UUID.randomUUID()
        val post = buildBlogPost(id = postId, authorId = otherAuthorId)
        every { blogPostRepository.findById(postId) } returns Optional.of(post)
        every { currentUserProvider.currentUserId() } returns currentUserId
        every { currentUserProvider.isCurrentUserAdmin() } returns false

        val request = update(title = "New Title")

        assertThatThrownBy { blogPostService.update(postId, request) }
            .isInstanceOf(ForbiddenException::class.java)

        verify(exactly = 0) { blogPostRepository.save(any()) }
    }

    @Test
    fun `Should allow an admin to update a post they do not own`() {
        val postId = UUID.randomUUID()
        val otherAuthorId = UUID.randomUUID()
        val otherAuthor = buildUser(otherAuthorId, "Other Author")
        val post = buildBlogPost(id = postId, authorId = otherAuthorId)
        every { blogPostRepository.findById(postId) } returns Optional.of(post)
        every { currentUserProvider.currentUserId() } returns currentUserId
        every { currentUserProvider.isCurrentUserAdmin() } returns true
        every { userRepository.findById(otherAuthorId) } returns Optional.of(otherAuthor)

        val savedSlot = slot<BlogPost>()
        every { blogPostRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val request = update(title = "New Title")

        blogPostService.update(postId, request)

        verify(exactly = 1) { blogPostRepository.save(any()) }
        val saved = savedSlot.captured
        assertThat(saved.title).isEqualTo("New Title")
        assertThat(saved.author.id).isEqualTo(otherAuthorId)
    }

    @Test
    fun `Should delete a post when requested by its owner`() {
        val postId = UUID.randomUUID()
        val post = buildBlogPost(id = postId, authorId = currentUserId)
        every { blogPostRepository.findById(postId) } returns Optional.of(post)
        every { currentUserProvider.currentUserId() } returns currentUserId
        every { blogPostRepository.delete(post) } just Runs

        blogPostService.delete(postId)

        verify(exactly = 1) { blogPostRepository.delete(post) }
    }

    @Test
    fun `Should throw ForbiddenException when deleting a post owned by another non-admin user`() {
        val postId = UUID.randomUUID()
        val otherAuthorId = UUID.randomUUID()
        val post = buildBlogPost(id = postId, authorId = otherAuthorId)
        every { blogPostRepository.findById(postId) } returns Optional.of(post)
        every { currentUserProvider.currentUserId() } returns currentUserId
        every { currentUserProvider.isCurrentUserAdmin() } returns false

        assertThatThrownBy { blogPostService.delete(postId) }
            .isInstanceOf(ForbiddenException::class.java)

        verify(exactly = 0) { blogPostRepository.delete(any()) }
    }

    @Test
    fun `Should throw BlogPostNotFoundException when deleting a post that does not exist`() {
        val postId = UUID.randomUUID()
        every { blogPostRepository.findById(postId) } returns Optional.empty()

        assertThatThrownBy { blogPostService.delete(postId) }
            .isInstanceOf(BlogPostNotFoundException::class.java)
    }
}
