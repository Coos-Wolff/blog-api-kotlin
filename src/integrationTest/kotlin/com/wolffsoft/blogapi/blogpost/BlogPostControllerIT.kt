package com.wolffsoft.blogapi.blogpost

import com.wolffsoft.blogapi.IntegrationTestBase
import com.wolffsoft.blogapi.IntegrationTestConstants.ADMIN_EMAIL
import com.wolffsoft.blogapi.IntegrationTestConstants.ADMIN_NAME
import com.wolffsoft.blogapi.IntegrationTestConstants.BLOGPOST_BODY
import com.wolffsoft.blogapi.IntegrationTestConstants.BLOGPOST_BY_ID_URI
import com.wolffsoft.blogapi.IntegrationTestConstants.BLOGPOST_IMG_URL
import com.wolffsoft.blogapi.IntegrationTestConstants.BLOGPOST_SUBTITLE
import com.wolffsoft.blogapi.IntegrationTestConstants.BLOGPOST_TITLE
import com.wolffsoft.blogapi.IntegrationTestConstants.BLOGPOST_URI
import com.wolffsoft.blogapi.IntegrationTestConstants.EMAIL
import com.wolffsoft.blogapi.IntegrationTestConstants.EMAIL_2
import com.wolffsoft.blogapi.IntegrationTestConstants.LOGIN_URI
import com.wolffsoft.blogapi.IntegrationTestConstants.NAME
import com.wolffsoft.blogapi.IntegrationTestConstants.NAME_2
import com.wolffsoft.blogapi.IntegrationTestConstants.PASSWORD_CORRECT
import com.wolffsoft.blogapi.IntegrationTestConstants.REGISTER_URI
import com.wolffsoft.blogapi.auth.dto.LoginRequest
import com.wolffsoft.blogapi.auth.dto.RegisterRequest
import com.wolffsoft.blogapi.auth.dto.TokenResponse
import com.wolffsoft.blogapi.blogpost.dto.BlogPostResponse
import com.wolffsoft.blogapi.blogpost.dto.CreateBlogPostRequest
import com.wolffsoft.blogapi.blogpost.dto.PagedResponse
import com.wolffsoft.blogapi.blogpost.dto.UpdateBlogPostRequest
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.expectBody

class BlogPostControllerIT : IntegrationTestBase() {


    val title251Characters: String = "a".repeat(251)

    val subtitle251Characters = "b".repeat(251)


    val imgUrl251Characters = "c".repeat(251)

    @Test
    fun `Should create a blog post successfully`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        val createBlogPostRequest = CreateBlogPostRequest(
            BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL
        )

        val response = requireNotNull(restTestClient.post()
            .uri(BLOGPOST_URI)
            .headers { it.addAll(headers) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(createBlogPostRequest)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody<BlogPostResponse>()
            .returnResult()
            .responseBody) {"Creation of blog post should return saved blog post"}

        assertThat(response.id).isNotNull()
        assertThat(response.title).isEqualTo(BLOGPOST_TITLE)
        assertThat(response.subtitle).isEqualTo(BLOGPOST_SUBTITLE)
        assertThat(response.body).isEqualTo(BLOGPOST_BODY)
        assertThat(response.imgUrl).isEqualTo(BLOGPOST_IMG_URL)
        assertThat(response.author.id).isNotNull()
        assertThat(response.author.name).isEqualTo(NAME)
    }

    @Test
    fun `Should return 401 when creating a post without authentication`() {
        restTestClient.post()
            .uri(BLOGPOST_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(CreateBlogPostRequest(BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL))
            .exchange()
            .expectStatus()
            .isUnauthorized()
    }

    @Test
    fun `Should return 401 when updating a post without authentication`() {
        restTestClient.patch()
            .uri(BLOGPOST_BY_ID_URI, UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .body(update(title = "New Title"))
            .exchange()
            .expectStatus()
            .isUnauthorized()
    }

    @Test
    fun `Should return 401 when deleting a post without authentication`() {
        restTestClient.delete()
            .uri(BLOGPOST_BY_ID_URI, UUID.randomUUID())
            .exchange()
            .expectStatus()
            .isUnauthorized()
    }

    @Test
    fun `Should return an empty page when no posts exist`() {
        val response = requireNotNull(
            restTestClient.get()
                .uri(BLOGPOST_URI)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<PagedResponse<BlogPostResponse>>()
                .returnResult()
                .responseBody
        ) { "GET posts should return a paged response body" }

        assertThat(response.content).isEmpty()
    }

    @Test
    fun `Should return 404 when getting a non-existent post`() {
        restTestClient.get()
            .uri(BLOGPOST_BY_ID_URI, UUID.randomUUID())
            .exchange()
            .expectStatus()
            .isNotFound()
    }

    @Test
    fun `Should allow anonymous read of a single post after it is created`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        val created = createPost(headers)

        val response = requireNotNull(
            restTestClient.get()
                .uri(BLOGPOST_BY_ID_URI, created.id)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<BlogPostResponse>()
                .returnResult()
                .responseBody
        ) { "GET post by id should return a blog post response body" }

        assertThat(response.title).isEqualTo(BLOGPOST_TITLE)
        assertThat(response.subtitle).isEqualTo(BLOGPOST_SUBTITLE)
        assertThat(response.body).isEqualTo(BLOGPOST_BODY)
        assertThat(response.imgUrl).isEqualTo(BLOGPOST_IMG_URL)
        assertThat(response.author.name).isEqualTo(NAME)
    }

    @Test
    fun `Should allow anonymous read of all posts after one is created`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        val created = createPost(headers)

        val response = requireNotNull(
            restTestClient.get()
                .uri(BLOGPOST_URI)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<PagedResponse<BlogPostResponse>>()
                .returnResult()
                .responseBody
        ) { "GET posts should return a paged response body" }

        assertThat(response.content).hasSize(1)
        assertThat(response.content[0].id).isEqualTo(created.id)
    }

    @Test
    fun `Should return 400 when title exceeds the max length`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)

        restTestClient.post()
            .uri(BLOGPOST_URI)
            .headers { it.addAll(headers) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(CreateBlogPostRequest(title251Characters, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL))
            .exchange()
            .expectStatus()
            .isBadRequest()
    }

    @Test
    fun `Should return 400 when subtitle exceeds the max length`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)

        restTestClient.post()
            .uri(BLOGPOST_URI)
            .headers { it.addAll(headers) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(CreateBlogPostRequest(BLOGPOST_TITLE, subtitle251Characters, BLOGPOST_BODY, BLOGPOST_IMG_URL))
            .exchange()
            .expectStatus()
            .isBadRequest()
    }

    @Test
    fun `Should return 400 when imgUrl exceeds the max length`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)

        restTestClient.post()
            .uri(BLOGPOST_URI)
            .headers { it.addAll(headers) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(CreateBlogPostRequest(BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, imgUrl251Characters))
            .exchange()
            .expectStatus()
            .isBadRequest()
    }

    @Test
    fun `Should return 400 when title is blank`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)

        restTestClient.post()
            .uri(BLOGPOST_URI)
            .headers { it.addAll(headers) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(CreateBlogPostRequest("", BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL))
            .exchange()
            .expectStatus()
            .isBadRequest()
    }

    @Test
    fun `Should update only the title when patched by the owner`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        val created = createPost(headers)

        val response = requireNotNull(
            restTestClient.patch()
                .uri(BLOGPOST_BY_ID_URI, created.id)
                .headers { it.addAll(headers) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(update(title = "New Title"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<BlogPostResponse>()
                .returnResult()
                .responseBody
        ) { "PATCH post should return the updated blog post response body" }

        assertThat(response.title).isEqualTo("New Title")
        assertThat(response.subtitle).isEqualTo(BLOGPOST_SUBTITLE)
        assertThat(response.body).isEqualTo(BLOGPOST_BODY)
        assertThat(response.imgUrl).isEqualTo(BLOGPOST_IMG_URL)
        assertThat(response.author.name).isEqualTo(NAME)
    }

    @Test
    fun `Should return 403 and leave the post unchanged when a non-owner patches it`() {
        val ownerHeaders = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        val created = createPost(ownerHeaders)

        val otherHeaders = addHeaderFor(EMAIL_2, NAME_2, PASSWORD_CORRECT)

        restTestClient.patch()
            .uri(BLOGPOST_BY_ID_URI, created.id)
            .headers { it.addAll(otherHeaders) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(update(title = "New Title"))
            .exchange()
            .expectStatus()
            .isForbidden()

        val response = requireNotNull(
            restTestClient.get()
                .uri(BLOGPOST_BY_ID_URI, created.id)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<BlogPostResponse>()
                .returnResult()
                .responseBody
        ) { "GET post by id should return a blog post response body" }

        assertThat(response.title).isEqualTo(BLOGPOST_TITLE)
    }

    @Test
    fun `Should return 403 and leave the post intact when a non-owner deletes it`() {
        val ownerHeaders = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        val created = createPost(ownerHeaders)

        val otherHeaders = addHeaderFor(EMAIL_2, NAME_2, PASSWORD_CORRECT)

        restTestClient.delete()
            .uri(BLOGPOST_BY_ID_URI, created.id)
            .headers { it.addAll(otherHeaders) }
            .exchange()
            .expectStatus()
            .isForbidden()

        restTestClient.get()
            .uri(BLOGPOST_BY_ID_URI, created.id)
            .exchange()
            .expectStatus()
            .isOk()
    }

    @Test
    fun `Should return 404 when the owner patches a non-existent post`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)

        restTestClient.patch()
            .uri(BLOGPOST_BY_ID_URI, UUID.randomUUID())
            .headers { it.addAll(headers) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(update(title = "New Title"))
            .exchange()
            .expectStatus()
            .isNotFound()
    }

    @Test
    fun `Should return 404 when the owner deletes a non-existent post`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)

        restTestClient.delete()
            .uri(BLOGPOST_BY_ID_URI, UUID.randomUUID())
            .headers { it.addAll(headers) }
            .exchange()
            .expectStatus()
            .isNotFound()
    }

    @Test
    fun `Should delete a post and make it unavailable afterward`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        val created = createPost(headers)

        restTestClient.delete()
            .uri(BLOGPOST_BY_ID_URI, created.id)
            .headers { it.addAll(headers) }
            .exchange()
            .expectStatus()
            .isNoContent()

        restTestClient.get()
            .uri(BLOGPOST_BY_ID_URI, created.id)
            .exchange()
            .expectStatus()
            .isNotFound()
    }

    @Test
    fun `Should allow an admin to patch a post they do not own`() {
        val ownerHeaders = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        val created = createPost(ownerHeaders)

        registerUser(ADMIN_EMAIL, ADMIN_NAME, PASSWORD_CORRECT)
        promoteToAdmin(ADMIN_EMAIL)
        // Roles are snapshotted into the JWT at login time, so the login MUST happen after
        // promoteToAdmin - logging in first would mint a token still carrying only ROLE_USER.
        val adminHeaders = loginHeaderFor(ADMIN_EMAIL, PASSWORD_CORRECT)

        val response = requireNotNull(
            restTestClient.patch()
                .uri(BLOGPOST_BY_ID_URI, created.id)
                .headers { it.addAll(adminHeaders) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(update(title = "New Title"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<BlogPostResponse>()
                .returnResult()
                .responseBody
        ) { "PATCH post should return the updated blog post response body" }

        assertThat(response.title).isEqualTo("New Title")
    }

    @Test
    fun `Should allow an admin to delete a post they do not own`() {
        val ownerHeaders = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        val created = createPost(ownerHeaders)

        registerUser(ADMIN_EMAIL, ADMIN_NAME, PASSWORD_CORRECT)
        promoteToAdmin(ADMIN_EMAIL)
        // Roles are snapshotted into the JWT at login time, so the login MUST happen after
        // promoteToAdmin - logging in first would mint a token still carrying only ROLE_USER.
        val adminHeaders = loginHeaderFor(ADMIN_EMAIL, PASSWORD_CORRECT)

        restTestClient.delete()
            .uri(BLOGPOST_BY_ID_URI, created.id)
            .headers { it.addAll(adminHeaders) }
            .exchange()
            .expectStatus()
            .isNoContent()

        restTestClient.get()
            .uri(BLOGPOST_BY_ID_URI, created.id)
            .exchange()
            .expectStatus()
            .isNotFound()
    }

    @Test
    fun `Should return the first page with correct pagination metadata`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        (1..3).forEach { createPost(headers, uniqueTitleRequest(it)) }

        val response = requireNotNull(
            restTestClient.get()
                .uri { it.path(BLOGPOST_URI).queryParam("page", 0).queryParam("size", 2).build() }
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<PagedResponse<BlogPostResponse>>()
                .returnResult()
                .responseBody
        ) { "GET posts should return a paged response body" }

        assertThat(response.content).hasSize(2)
        assertThat(response.totalElements).isEqualTo(3)
        assertThat(response.totalPages).isEqualTo(2)
        assertThat(response.hasNext).isTrue()
        assertThat(response.hasPrevious).isFalse()
        assertThat(response.page).isEqualTo(0)
        assertThat(response.size).isEqualTo(2)
    }

    @Test
    fun `Should return the second page with correct pagination metadata`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        (1..3).forEach { createPost(headers, uniqueTitleRequest(it)) }

        val response = requireNotNull(
            restTestClient.get()
                .uri { it.path(BLOGPOST_URI).queryParam("page", 1).queryParam("size", 2).build() }
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<PagedResponse<BlogPostResponse>>()
                .returnResult()
                .responseBody
        ) { "GET posts should return a paged response body" }

        assertThat(response.content).hasSize(1)
        assertThat(response.page).isEqualTo(1)
        assertThat(response.hasNext).isFalse()
        assertThat(response.hasPrevious).isTrue()
    }

    @Test
    fun `Should return 400 and leave the post unchanged when the owner patches with an over-length title`() {
        val headers = addHeaderFor(EMAIL, NAME, PASSWORD_CORRECT)
        val created = createPost(headers)

        restTestClient.patch()
            .uri(BLOGPOST_BY_ID_URI, created.id)
            .headers { it.addAll(headers) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(update(title = title251Characters))
            .exchange()
            .expectStatus()
            .isBadRequest()

        val response = requireNotNull(
            restTestClient.get()
                .uri(BLOGPOST_BY_ID_URI, created.id)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<BlogPostResponse>()
                .returnResult()
                .responseBody
        ) { "GET post by id should return a blog post response body" }

        assertThat(response.title).isEqualTo(BLOGPOST_TITLE)
    }

    private fun createPost(
        headers: HttpHeaders,
        request: CreateBlogPostRequest = CreateBlogPostRequest(
            BLOGPOST_TITLE, BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL
        ),
    ): BlogPostResponse = requireNotNull(
        restTestClient.post()
            .uri(BLOGPOST_URI)
            .headers { it.addAll(headers) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody<BlogPostResponse>()
            .returnResult()
            .responseBody
    ) { "Creation of blog post should return saved blog post" }

    private fun uniqueTitleRequest(index: Int) = CreateBlogPostRequest(
        "$BLOGPOST_TITLE $index", BLOGPOST_SUBTITLE, BLOGPOST_BODY, BLOGPOST_IMG_URL
    )

    private fun update(
        title: String? = null,
        subtitle: String? = null,
        body: String? = null,
        imgUrl: String? = null,
    ) = UpdateBlogPostRequest(title = title, subtitle = subtitle, body = body, imgUrl = imgUrl)

    private fun registerUser(email: String, name: String, password: String) {
        val registerRequest = RegisterRequest(email, name, password)
        restTestClient.post()
            .uri(REGISTER_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerRequest)
            .exchange()
            .expectStatus()
            .isCreated()
    }

    private fun loginHeaderFor(email: String, password: String): HttpHeaders {
        val loginRequest = LoginRequest(email, password)
        val responseToken = requireNotNull(
            restTestClient.post()
                .uri(LOGIN_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody<TokenResponse>()
                .returnResult()
                .getResponseBody()
        ) { "Login should return a token response body" }

        return HttpHeaders().apply { setBearerAuth(responseToken.accessToken) }
    }

    private fun addHeaderFor(email: String, name: String, password: String): HttpHeaders {
        registerUser(email, name, password)
        return loginHeaderFor(email, password)
    }

    private fun promoteToAdmin(email: String) {
        val user = userRepository.findByEmail(email) ?: error("User with email [$email] not found")
        userRepository.save(user.copy(isAdmin = true))
    }
}
