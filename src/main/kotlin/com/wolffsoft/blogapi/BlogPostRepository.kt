package com.wolffsoft.blogapi

import com.wolffsoft.blogapi.blogpost.BlogPost
import java.util.*
import org.springframework.data.repository.ListCrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface BlogPostRepository : ListCrudRepository<BlogPost, UUID>, PagingAndSortingRepository<BlogPost, UUID> {
}