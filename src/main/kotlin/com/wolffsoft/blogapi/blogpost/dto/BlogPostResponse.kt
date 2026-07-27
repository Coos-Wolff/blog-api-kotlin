package com.wolffsoft.blogapi.blogpost.dto

import java.time.LocalDate
import java.util.UUID

data class BlogPostResponse(
    val id: UUID,
    val title: String,
    val subtitle: String,
    val date: LocalDate,
    val body: String,
    val imgUrl: String,
    val author: AuthorResponse,
)
