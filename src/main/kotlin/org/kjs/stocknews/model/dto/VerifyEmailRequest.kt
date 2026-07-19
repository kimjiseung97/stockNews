package org.kjs.stocknews.model.dto

data class VerifyEmailRequest(
    val email: String,
    val code: String,
)
