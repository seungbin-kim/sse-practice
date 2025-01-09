package com.example.ssepractice

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component("NotificationMessagePublisher")
class RedisMessagePublisher(
    private val stringRedisTemplate: StringRedisTemplate,
) : MessagePublisher {

    override fun publish(
        topic: String,
        message: String
    ) {
        stringRedisTemplate.convertAndSend(topic, message)
    }

}