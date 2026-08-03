package org.kjs.stocknews.model.dto

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)
