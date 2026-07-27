package com.wolffsoft.blogapi.blogpost.dto

import jakarta.validation.constraints.Size

data class UpdateBlogPostRequest(
    @Size(max = 250)
    val title: String?,

    @Size(max = 250)
    val subtitle: String?,

    val body: String?,

    @Size(max = 250)
    val imgUrl: String?
)
