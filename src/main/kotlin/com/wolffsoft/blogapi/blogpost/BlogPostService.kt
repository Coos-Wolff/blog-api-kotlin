package com.wolffsoft.blogapi.blogpost

import com.wolffsoft.blogapi.auth.CurrentUserProvider
import com.wolffsoft.blogapi.auth.exception.InvalidTokenException
import com.wolffsoft.blogapi.blogpost.dto.AuthorResponse
import com.wolffsoft.blogapi.blogpost.dto.BlogPostResponse
import com.wolffsoft.blogapi.blogpost.dto.CreateBlogPostRequest
import com.wolffsoft.blogapi.blogpost.dto.PagedResponse
import com.wolffsoft.blogapi.blogpost.dto.UpdateBlogPostRequest
import com.wolffsoft.blogapi.blogpost.exception.BlogPostNotFoundException
import com.wolffsoft.blogapi.exception.ForbiddenException
import com.wolffsoft.blogapi.user.User
import com.wolffsoft.blogapi.user.UserRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import org.springframework.data.domain.Pageable
import org.springframework.data.jdbc.core.mapping.AggregateReference
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BlogPostService(
    private val blogPostRepository: BlogPostRepository,
    private val userRepository: UserRepository,
    private val currentUserProvider: CurrentUserProvider
) {

    companion object {
        const val BLOG_POST_NOT_FOUND_MESSAGE = "No blog post found for id: [%s]"
        const val AUTHOR_NOT_FOUND_MESSAGE = "Author [%s] not found for post [%s]"
    }
    @Transactional
    fun create(request: CreateBlogPostRequest): BlogPostResponse {
        val userId = currentUserProvider.currentUserId()
        val author = userRepository.findById(userId).getOrNull()
            ?: throw InvalidTokenException("Authenticated user no longer exists")
        val blogPost = request.toBlogPost(author.id)
        val saved = blogPostRepository.save(blogPost)
        return toResponse(saved, author)
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): BlogPostResponse {
        val blogPost = blogPostRepository.findById(id).getOrNull()
            ?: throw BlogPostNotFoundException(BLOG_POST_NOT_FOUND_MESSAGE.format(id))
        val author = findAuthorOrThrow(blogPost)
        return toResponse(blogPost, author)
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): PagedResponse<BlogPostResponse> {
        val page = blogPostRepository.findAll(pageable)
        val blogPosts = page.content
        val authorIds = blogPosts.map { it.author.id }.toSet()
        val users = userRepository.findAllById(authorIds).associateBy { it.id }
        val responses = blogPosts.map { blogPost ->
            toResponse(blogPost, users[blogPost.author.id]
                ?: error(AUTHOR_NOT_FOUND_MESSAGE.format(blogPost.author.id, blogPost.id)))}
        return PagedResponse.of(responses, page)
    }

    @Transactional
    fun update(id: UUID, request: UpdateBlogPostRequest): BlogPostResponse {
        val blogPost = fetchOwnedBlogPostOrThrow(id)
        val updated = blogPost.copy(
            title = request.title ?: blogPost.title,
            subtitle = request.subtitle ?: blogPost.subtitle,
            body = request.body ?: blogPost.body,
            imgUrl = request.imgUrl ?: blogPost.imgUrl,
        )
        val patched = blogPostRepository.save(updated)
        val author = findAuthorOrThrow(patched)
        return toResponse(patched, author)
    }

    @Transactional
    fun delete(id: UUID) {
        val blogPost = fetchOwnedBlogPostOrThrow(id)
        blogPostRepository.delete(blogPost)
    }

    private fun CreateBlogPostRequest.toBlogPost(authorId: UUID): BlogPost {
        return BlogPost(
            title = title,
            subtitle = subtitle,
            date = LocalDate.now(),
            body = body,
            imgUrl = imgUrl,
            author = AggregateReference.to(authorId)
        )
    }

    private fun toResponse(blogPost: BlogPost, author: User): BlogPostResponse =
        BlogPostResponse(
            id = blogPost.id,
            title = blogPost.title,
            subtitle = blogPost.subtitle,
            date = blogPost.date,
            body = blogPost.body,
            imgUrl = blogPost.imgUrl,
            author = AuthorResponse(author.id, author.name))

    private fun fetchOwnedBlogPostOrThrow(id: UUID): BlogPost {
        val blogPost = blogPostRepository.findById(id).getOrNull()
        ?: throw BlogPostNotFoundException(BLOG_POST_NOT_FOUND_MESSAGE.format(id))
        if (!isOwnerOrAdmin(blogPost)) {
            throw ForbiddenException("Forbidden user")
        }
        return blogPost
    }

    private fun isOwnerOrAdmin(blogPost: BlogPost): Boolean =
        blogPost.author.id == currentUserProvider.currentUserId() || currentUserProvider.isCurrentUserAdmin()

    private fun findAuthorOrThrow(blogPost: BlogPost): User {
        val authorId = blogPost.author.id
        return userRepository.findById(authorId).getOrNull()
            ?: error(AUTHOR_NOT_FOUND_MESSAGE.format(authorId, blogPost.id))
    }
}