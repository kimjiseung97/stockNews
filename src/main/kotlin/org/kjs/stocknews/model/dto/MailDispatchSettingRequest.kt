package org.kjs.stocknews.model.dto

import java.time.LocalTime

data class MailDispatchSettingRequest(
    val dispatchTime: LocalTime,
)
