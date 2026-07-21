package com.wolffsoft.blogapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class BlogApiApplication

fun main(args: Array<String>) {
    runApplication<BlogApiApplication>(*args)
}
