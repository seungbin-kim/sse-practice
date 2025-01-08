package com.example.ssepractice.controller

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@RestController
class ServerSentEventController(
    private val emitters: ConcurrentHashMap<String, SseEmitter> = ConcurrentHashMap()
) {

    private companion object {
        const val ORDER_NOTIFICATION_TYPE = "ORDER_NOTIFICATION"
        const val ENTIRE_NOTIFICATION_TYPE = "ENTIRE_NOTIFICATION"

        const val TIMEOUT_MILLIS: Long = 30L * 1000L
        const val RECONNECT_TIME_MILLIS: Long = 1L * 1000L
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
        emitters[admin] = emitter

        emitter.onError {
            logger.info("onError 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
        }
        emitter.onTimeout {
            emitter.complete()
            logger.info("onTimeout 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
            logger.info("onTimeout - emitters: {}", emitters)
        }
        emitter.onCompletion {
            logger.info("onCompletion 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
            emitters.remove(admin)
            logger.info("onCompletion - emitters: {}", emitters)
        }

        // 최초 연결시 timeout 시간 안에 첫 응답을 주어야 클라이언트가 지속적으로 자동 재연결 가능
        // 응답을 한번도 안주면 503. 클라이언트가 재연결 시도하지 않음
        emitters.sendTo(
            target = admin,
            comment = "server hello",
            reconnectTime = RECONNECT_TIME_MILLIS,
        )

        // 마지막 수신 ID가 있다면 해당 ID+1 부터 다시 보내주어야 함.
        // 조회 및 전송은 비동기로?
        lastEventId?.let { lastId ->
            // lastId 이후 이벤트들 조회, 전송
        }

        logger.info("emitters: {}", emitters)
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
        logger.info("[{}]from user[{}] to admin [{}]", tmpSeq, user, admin)

        tmpSeq++

        emitters.sendTo(
            target = admin,
            data = "${admin}님, [${user}] 유저의 주문이 도착하였습니다.",
            eventType = ORDER_NOTIFICATION_TYPE,
            lastEventId = tmpSeq.toString(),
        )
    }
    private var tmpSeq = 0L

    @CrossOrigin(origins = ["*"])
    @PostMapping("/notify-all")
    fun notifyAllToAdmins() {
        logger.info("전체 알림")

        emitters.sendToAll(
            eventType = ENTIRE_NOTIFICATION_TYPE,
            data = "전체 알림! 수신자: [@key]"
        )
    }

}

private fun ConcurrentHashMap<String, SseEmitter>.sendTo(
    target: String,
    eventType: String? = null,
    data: String? = null,
    comment: String? = null,
    lastEventId: String? = null,
    reconnectTime: Long? = null,
) {
    try {
        this[target]?.send(
            SseEmitter
                .event()
                .apply { eventType?.run { name(eventType) } }
                .apply { data?.run { data(data) } }
                .apply { comment?.run { comment(comment) } }
                .apply { lastEventId?.run { id(lastEventId) } }
                .apply { reconnectTime?.run { reconnectTime(reconnectTime) } }
        )
    } catch (e: Exception) {
        this.remove(target)
    }
}

private fun ConcurrentHashMap<String, SseEmitter>.sendToAll(
    eventType: String? = null,
    data: String? = null,
    comment: String? = null,
    lastEventId: String? = null,
    reconnectTime: Long? = null,
) {
    this.forEach { (key, _) ->
        sendTo(
            target = key,
            eventType = eventType,
            data = data?.replace("@key", key),
            comment = comment,
            lastEventId = lastEventId,
            reconnectTime = reconnectTime,
        )
    }
}