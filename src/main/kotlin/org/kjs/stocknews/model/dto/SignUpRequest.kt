package org.kjs.stocknews.model.dto

data class SignUpRequest(
    val email: String,
    val password: String,
    val recoveryEmail: String,
)
