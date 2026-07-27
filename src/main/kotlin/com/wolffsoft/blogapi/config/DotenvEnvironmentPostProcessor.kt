package com.wolffsoft.blogapi.config

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class DotenvEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val entries = Dotenv.configure()
            .ignoreIfMissing()
            .ignoreIfMalformed()
            .load()
            .entries()

        if (entries.isEmpty()) {
            return
        }

        val values = entries.associate { it.key to it.value }
        // addLast: a real OS/CI environment variable for the same key still wins over the .env fallback.
        environment.propertySources.addLast(MapPropertySource("dotenv", values))
    }
}
