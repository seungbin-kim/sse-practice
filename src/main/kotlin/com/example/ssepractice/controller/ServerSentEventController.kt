package com.example.ssepractice.controller

import com.example.ssepractice.store.SseConnectionStore
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
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
    private val eventPublisher: ApplicationEventPublisher,
    private val sseConnectionStore: SseConnectionStore,
) {

    private companion object {
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
        sseConnectionStore.storeEmitter(name = admin, emitter = emitter)

        emitter.onError {
            logger.info("onError 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
        }
        emitter.onTimeout {
            logger.info("onTimeout 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
            logger.info("onTimeout - emitters: {}", sseConnectionStore.emitterEntries)
        }
        emitter.onCompletion {
            logger.info("onCompletion 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
            sseConnectionStore.removeEmitter(name = admin)
            logger.info("onCompletion - emitters: {}", sseConnectionStore.emitterEntries)
        }

        // 최초 연결시 timeout 시간 안에 첫 응답을 주어야 클라이언트가 지속적으로 자동 재연결 가능
        // 응답을 한번도 안주면 503. 클라이언트가 재연결 시도하지 않음
        sseConnectionStore.sendTo(
            target = admin,
            comment = "server hello",
            reconnectTime = RECONNECT_TIME_MILLIS,
        )

        // 마지막 수신 ID가 있다면 해당 ID+1 부터 다시 보내주어야 함.
        // 조회 및 전송은 비동기로?
        lastEventId?.let { lastId ->
            // lastId 이후 이벤트들 조회, 전송
        }

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

        // 애플리케이션 이벤트를 발행한다. -> 이벤트 핸들러가 이벤트에 대한 처리(redis pub)
        eventPublisher.publishEvent(
            OrderEvent(
                source = user,
                target = admin,
                message = "@admin님, @user 유저의 주문이 도착하였습니다.",
                lastEventId = tmpSeq.toString(),
            )
        )
    }
    private var tmpSeq = 0L

    @CrossOrigin(origins = ["*"])
    @PostMapping("/notify-all")
    fun notifyAllToAdmins() {
        eventPublisher.publishEvent(EntireNotificationEvent("전체 알림! 수신자: [@key]"))
    }

}