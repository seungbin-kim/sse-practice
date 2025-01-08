package com.example.ssepractice.config

import com.example.ssepractice.NotificationReceiver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class RedisMessageConfig(
    private val notificationReceiver: NotificationReceiver,
) {

    @Bean
    fun redisContainer(
        connectionFactory: RedisConnectionFactory,
        listenerAdapter: MessageListenerAdapter,
    ): RedisMessageListenerContainer {
        return RedisMessageListenerContainer()
            .apply {
                setConnectionFactory(connectionFactory)
                addMessageListener(listenerAdapter, PatternTopic("notification"))
                setTaskExecutor(
                    ThreadPoolTaskExecutor().apply {
                        corePoolSize = 2
                        maxPoolSize = 2
                        queueCapacity = 500
                        setThreadNamePrefix("RedisMessageListenerContainer-")
                        initialize()
                    }
                )
            }
    }

    @Bean
    fun listenerAdapter(): MessageListenerAdapter {
        return MessageListenerAdapter(notificationReceiver)
    }

    @Bean
    fun stringRedisTemplate(
        connectionFactory: RedisConnectionFactory,
    ): StringRedisTemplate {
        return StringRedisTemplate(connectionFactory)
    }

}