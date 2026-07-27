package com.wolffsoft.blogapi

internal object IntegrationTestConstants {
    const val REGISTER_URI: String = "/api/auth/register"
    const val LOGIN_URI: String = "/api/auth/login"
    const val REFRESH_URI: String = "/api/auth/refresh"
    const val BLOGPOST_URI: String = "/api/posts"
    val BLOGPOST_BY_ID_URI: String = BLOGPOST_URI + "/{id}"
    const val EMAIL: String = "nvt@nvt.nl"
    const val NAME: String = "name"
    const val EMAIL_2: String = "second@example.com"
    const val NAME_2: String = "second"
    const val ADMIN_EMAIL: String = "admin@example.com"
    const val ADMIN_NAME: String = "admin"
    const val PASSWORD_CORRECT: String = "password-of-minimal-12"
    const val AUTHORISATION_HEADER: String = "Authorization"
    const val AUTHENTICATION_SCHEME_BEARER: String = "Bearer %s"
    const val BLOGPOST_TITLE: String = "title"
    const val BLOGPOST_SUBTITLE: String = "subtitle"
    const val BLOGPOST_BODY: String = "body"
    const val BLOGPOST_IMG_URL: String = "imgUrl"
}