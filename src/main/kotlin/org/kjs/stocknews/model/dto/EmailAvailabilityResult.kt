package org.kjs.stocknews.model.dto

data class EmailAvailabilityResult(
    val duplicated: Boolean,
    val isMailSendSuccess: Boolean,
)
