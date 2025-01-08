package com.example.ssepractice.listener

import com.example.ssepractice.controller.EntireNotificationEvent
import com.example.ssepractice.controller.OrderEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

// 이벤트 핸들러는 각 서버 인스턴스에게 메세지를 보내(redis pub) 연결된 클라이언트에게 알림을 보내도록 한다.
@Component
class NotificationEventHandler(
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {

    private val logger = LoggerFactory.getLogger(NotificationEventHandler::class.java)

    @Async
    @EventListener
    fun handleEvent(orderEvent: OrderEvent) {
        logger.info("주문알림 이벤트 발생")
        stringRedisTemplate.convertAndSend("notification", objectMapper.writeValueAsString(orderEvent))
    }

    @Async
    @EventListener
    fun handleEvent(entireNotificationEvent: EntireNotificationEvent) {
        logger.info("전체알림 이벤트 발생")
        stringRedisTemplate.convertAndSend("notification", objectMapper.writeValueAsString(entireNotificationEvent))
    }

}