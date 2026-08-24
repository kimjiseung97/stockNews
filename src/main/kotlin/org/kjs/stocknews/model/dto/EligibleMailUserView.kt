package org.kjs.stocknews.model.dto

import java.time.LocalTime

data class EligibleMailUserView(
    val userId: Long,
    val email: String,
    val dispatchTime: LocalTime,
)
