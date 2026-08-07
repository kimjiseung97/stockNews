package org.kjs.stocknews.model.dto

data class ResetPasswordCompleteRequest(
    val email: String,
    val newPassword: String,
)
