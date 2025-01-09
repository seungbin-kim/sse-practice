package com.example.ssepractice.publisher

interface MessagePublisher {

    fun publish(topic: String, message: String)

}