package com.wolffsoft.blogapi.blogpost

import java.util.UUID
import org.springframework.data.repository.ListCrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface BlogPostRepository : ListCrudRepository<BlogPost, UUID>, PagingAndSortingRepository<BlogPost, UUID> {
}