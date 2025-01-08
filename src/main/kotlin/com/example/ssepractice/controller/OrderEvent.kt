package com.example.ssepractice.controller

data class OrderEvent(
    val source: String,
    val target: String,
    var message: String,
    val lastEventId: String,
) {

    val type: String
        get() = "ORDER_NOTIFICATION"

    init {
        message = message.replace("@admin", target).replace("@user", source)
    }

}