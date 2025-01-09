package com.example.ssepractice

interface MessagePublisher {

    fun publish(topic: String, message: String)

}