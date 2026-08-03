package org.kjs.stocknews.model.dto

data class VerifyResetPasswordRequest(
    val email: String,
    val code: String,
)
