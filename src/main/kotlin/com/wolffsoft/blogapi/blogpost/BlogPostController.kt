package com.wolffsoft.blogapi.blogpost

import com.wolffsoft.blogapi.blogpost.dto.BlogPostResponse
import com.wolffsoft.blogapi.blogpost.dto.CreateBlogPostRequest
import com.wolffsoft.blogapi.blogpost.dto.PagedResponse
import com.wolffsoft.blogapi.blogpost.dto.UpdateBlogPostRequest
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/api/posts")
class BlogPostController(private val blogPostService: BlogPostService) {

    @GetMapping
    fun findAll(@PageableDefault(size = 20) pageable: Pageable): ResponseEntity<PagedResponse<BlogPostResponse>> {
        val response = blogPostService.findAll(pageable)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<BlogPostResponse> {
        val response = blogPostService.getById(id)
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun create(@Valid @RequestBody request: CreateBlogPostRequest): ResponseEntity<BlogPostResponse> {
        val response = blogPostService.create(request)
        val location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id)
            .toUri()
        return ResponseEntity.created(location).body(response)
    }

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateBlogPostRequest
    ): ResponseEntity<BlogPostResponse> {
        val response = blogPostService.update(id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        blogPostService.delete(id)
        return ResponseEntity.noContent().build()
    }
}