package com.wolffsoft.blogapi

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<BlogApiApplication>().with(TestcontainersConfiguration::class).run(*args)
}
