package com.example.ssepractice

import com.example.ssepractice.store.SseConnectionManager
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NotificationReceiver(
    private val sseConnectionManager: SseConnectionManager,
    private val objectMapper: ObjectMapper,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    // 각 서버 인스턴스는 redis 에서 메세지를 받아 연결된 클라이언트에 SSE 알람을 보낸다.
    fun handleMessage(message: String) { // 기본적으론 handleMessage 이름으로 구현해야 함. 커스텀 가능
        logger.info("Received message: {}", message)

        val event = objectMapper.readValue(message, Map::class.java)

        when (val type = event["type"] as String) {
            "ORDER_NOTIFICATION" -> {
                logger.info("주문알림 전송")
                sseConnectionManager.sendTo(
                    eventType = type,
                    target = event["target"] as String,
                    data = event["message"] as String,
                    lastEventId = event["lastEventId"] as String,
                )
            }

            "ENTIRE_NOTIFICATION" -> {
                logger.info("전체알림 전송")
                sseConnectionManager.sendToAll(
                    eventType = type,
                    data = event["message"] as String,
                )
            }
        }
    }

}