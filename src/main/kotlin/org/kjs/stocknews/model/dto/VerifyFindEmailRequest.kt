package org.kjs.stocknews.model.dto

data class VerifyFindEmailRequest(
    val recoveryEmail: String,
    val code: String,
)
