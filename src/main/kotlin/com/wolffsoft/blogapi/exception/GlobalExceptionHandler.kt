package com.wolffsoft.blogapi.exception

import com.wolffsoft.blogapi.auth.exception.EmailAlreadyExistsException
import com.wolffsoft.blogapi.auth.exception.InvalidTokenException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler: ResponseEntityExceptionHandler() {

    companion object {
        const val INVALID_CREDENTIALS_MESSAGE = "Invalid credentials"
        const val INVALID_TOKEN_MESSAGE = "Invalid token"
        const val CONFLICTING_VALUES_MESSAGE = "A conflicting value already exists"
        const val UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred"
        const val EMAIL_EXISTS_MESSAGE = "The email already exists"
    }

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailAlreadyExistsException(ex: EmailAlreadyExistsException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, EMAIL_EXISTS_MESSAGE)

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(ex: BadCredentialsException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE)

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidTokenException(ex: InvalidTokenException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_MESSAGE)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(ex: DataIntegrityViolationException): ProblemDetail {
        log.error("DataIntegrityViolationException: {}", ex.message)
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, CONFLICTING_VALUES_MESSAGE)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ProblemDetail {
        log.error("Unhandled exception", ex)
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_MESSAGE)
    }
}