package com.example.ssepractice.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@EnableAsync
@Configuration
class AsyncConfig: AsyncConfigurer {

    override fun getAsyncExecutor(): Executor? {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 2
            queueCapacity = 500
            setThreadNamePrefix("sse-async")
            initialize()
        }
    }

}