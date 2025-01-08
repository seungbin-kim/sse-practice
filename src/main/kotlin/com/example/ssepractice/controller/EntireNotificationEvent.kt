package com.example.ssepractice.controller

data class EntireNotificationEvent(
    val message: String
) {
    val type: String
        get() = "ENTIRE_NOTIFICATION"
}