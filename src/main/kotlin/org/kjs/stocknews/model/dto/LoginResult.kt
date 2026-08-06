package org.kjs.stocknews.model.dto

data class LoginResult(
    val userId: Long,
    val email: String,
    val requiresPasswordChange: Boolean,
)
