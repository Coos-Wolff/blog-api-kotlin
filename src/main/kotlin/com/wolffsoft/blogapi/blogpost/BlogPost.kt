package com.wolffsoft.blogapi.blogpost

import com.wolffsoft.blogapi.user.User
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.jdbc.core.mapping.AggregateReference
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("blog_post")
data class BlogPost(
    @Id val id: UUID = UUID.randomUUID(),
    val title: String,
    val subtitle: String,
    val date: LocalDate,
    val body: String,
    val imgUrl: String,
    val author: AggregateReference<User, UUID>,
    @Version val version: Long? = null
) {
}
