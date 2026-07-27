package com.wolffsoft.blogapi.blogpost.dto

import java.util.UUID

data class AuthorResponse(
    val id: UUID,
    val name: String
)
