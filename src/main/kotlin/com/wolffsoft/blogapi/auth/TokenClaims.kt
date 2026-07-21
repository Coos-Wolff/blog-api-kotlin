package com.wolffsoft.blogapi.auth

object TokenClaims {
    const val TYPE_ACCESS = "access"
    const val TYPE_REFRESH = "refresh"
    const val CLAIM_TOKEN_TYPE = "token_type"
    const val CLAIM_ROLES = "roles"
}