package com.example.ssepractice.config

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import redis.embedded.RedisServer
import java.io.IOException

// 임베디드 레디스 맥에서 안됨... 보안문제? 윈도우는 가능
@Configuration
@Profile("local-windows")
class EmbeddedRedisConfig(
    @Value("\${spring.redis.port:6379}")
    private var redisPort: String,
) {

    private var redisServer: RedisServer? = null

    @PostConstruct
    @Throws(IOException::class)
    fun startRedis() {
        redisServer = RedisServer(redisPort.toInt())
        redisServer?.start()
    }

    @PreDestroy
    fun stopRedis() {
        redisServer?.stop()
    }
}
