package com.wolffsoft.blogapi.blogpost.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateBlogPostRequest(
    @NotBlank
    @Size(max = 250)
    val title: String,

    @NotBlank
    @Size(max = 250)
    val subtitle: String,

    @NotBlank
    @Size(max = 5000)
    val body: String,

    @NotBlank
    @Size(max = 250)
    val imgUrl: String
)
