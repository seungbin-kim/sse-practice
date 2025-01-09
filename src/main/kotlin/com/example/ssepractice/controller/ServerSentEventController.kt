package com.example.ssepractice.controller

import com.example.ssepractice.MessagePublisher
import com.example.ssepractice.SseConnectionManager
import com.example.ssepractice.domain.EventType
import com.example.ssepractice.service.EventHistoryService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class ServerSentEventController(
    @Qualifier("NotificationMessagePublisher")
    private val messagePublisher: MessagePublisher,
    private val sseConnectionManager: SseConnectionManager,
    private val eventHistoryService: EventHistoryService,
    private val objectMapper: ObjectMapper,
) {

    private companion object {
        const val TIMEOUT_MILLIS: Long = 30L * 1000L
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    @CrossOrigin(origins = ["*"])
    @GetMapping("/sse", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun sse(
        @RequestParam("admin") admin: String,
        @RequestHeader("Last-Event-Id", required = false) lastEventId: String?,
    ): ResponseEntity<SseEmitter> {
        logger.info("admin: {}, lastEventId: {}", admin, lastEventId)

        val emitter = SseEmitter(TIMEOUT_MILLIS) // 해당 시간 이후 연결종료. 클라이언트에서 지속적으로 재연결
        emitter.onError {
            logger.info("onError 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
        }
        emitter.onTimeout {
            logger.info("onTimeout 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
            logger.info("onTimeout - emitters: {}", sseConnectionManager.emitterEntries)
        }
        emitter.onCompletion {
            logger.info("onCompletion 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
            sseConnectionManager.removeEmitter(name = admin)
            logger.info("onCompletion - emitters: {}", sseConnectionManager.emitterEntries)
        }
        sseConnectionManager.addEmitter(name = admin, emitter = emitter)

        // 마지막 수신 ID가 있다면 해당 ID+1 부터 다시 보내주어야 함.
        lastEventId?.let { lastId ->
            // lastId 이후 이벤트들 조회, 전송
            val histories = eventHistoryService.getEventHistoriesBy(
                target = admin,
                eventType = EventType.ORDER_NOTIFICATION,
                lastId = lastId.toLong()
            )
            // 0건이면 더미 보내게 해야함
            histories.ifEmpty { sseConnectionManager.sendToDummy(admin) }
            histories.forEach {
                sseConnectionManager.sendTo(
                    eventType = it.type.name,
                    target = it.target,
                    data = it.message,
                    lastEventId = it.id!!.toString(),
                )
            }
        }
        // 최초 연결시 timeout 시간 안에 첫 응답을 주어야 클라이언트가 지속적으로 자동 재연결 가능
        // 응답을 한번도 안주면 503. 클라이언트가 재연결 시도하지 않음
        ?: sseConnectionManager.sendToDummy(admin)

        return ResponseEntity
            .ok()
            .body(emitter)
    }

    @CrossOrigin(origins = ["*"])
    @PostMapping("/notify")
    fun notify(
        @RequestParam("user") user: String,
        @RequestParam("admin") admin: String,
    ) {
        logger.info("from user[{}] to admin [{}]", user, admin)

        val eventHistory = eventHistoryService.saveHistory(
            eventType = EventType.ORDER_NOTIFICATION,
            target = admin,
            message = "${admin}님, ${user} 유저의 주문이 도착하였습니다.",
        )

        val message = objectMapper.writeValueAsString(
            mapOf(
                "type" to eventHistory.type.name,
                "source" to user,
                "target" to eventHistory.target,
                "message" to eventHistory.message,
                "lastEventId" to eventHistory.id!!.toString()
            )
        )
        // redis pub
        messagePublisher.publish("notification", message)
    }

    @CrossOrigin(origins = ["*"])
    @PostMapping("/notify-all")
    fun notifyAllToAdmins() {
        val message = objectMapper.writeValueAsString(
            mapOf(
                "type" to EventType.ENTIRE_NOTIFICATION.name,
                "message" to "전체 알림! 수신자: [@key]"
            )
        )
        messagePublisher.publish("notification", message)
    }

}