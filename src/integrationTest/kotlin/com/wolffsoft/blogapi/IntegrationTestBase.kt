package com.wolffsoft.blogapi

import com.wolffsoft.blogapi.blogpost.BlogPostRepository
import com.wolffsoft.blogapi.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.client.RestTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration::class)
abstract class IntegrationTestBase {

    @Autowired
    protected lateinit var restTestClient: RestTestClient

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var blogPostRepository: BlogPostRepository

    @BeforeEach
    fun cleanDatabase() {
        blogPostRepository.deleteAll()
        userRepository.deleteAll()
    }

}