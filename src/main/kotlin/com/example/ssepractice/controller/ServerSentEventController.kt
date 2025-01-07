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

        const val TIMEOUT_MILLIS: Long = 10 * 1000
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    @CrossOrigin(origins = ["*"])
    @GetMapping("/sse", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun sse(
        @RequestParam("admin") admin: String,
        @RequestHeader("Last-Event-Id", required = false) lastEventId: String?,
    ): ResponseEntity<SseEmitter> {
        logger.info("admin: {}, lastEventId: {}", admin, lastEventId)

        val emitter = SseEmitter(TIMEOUT_MILLIS) // 해당 시간마다 지속적으로 재연결됨
        emitter.onError {
            logger.info("onError 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
            // 재연결 대비 이벤트 저장?
        }
        emitter.onTimeout {
            logger.info("onTimeout 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
            logger.info("onTimeout - emitters: {}", emitters)
        }
        emitter.onCompletion {
            logger.info("onCompletion 콜백, admin: {}, lastEventId: {}", admin, lastEventId)
            logger.info("onCompletion - emitters: {}", emitters)
            emitters.remove(admin)
        }

        // 최초 연결시 timeout 시간 안에 첫 응답을 주어야 클라이언트가 지속적으로 자동 재연결 가능
        // 응답을 한번도 안주면 503. 클라이언트가 재연결 시도하지 않음
        emitter.send(
            SseEmitter
                .event()
                .name("init")
                .data("Hello") // 최초 전송 더미데이터
                .reconnectTime(0L) // 연결이 끊겼을 때, 클라이언트가 다음 재시도까지의 대기시간
        )
        // name을 지정하지 않은경우는 "message"
//        emitter.send(
//            SseEmitter
//                .event()
//                .data("기본이벤트")
//        )

        // 마지막 수신 ID가 있다면 해당 ID+1 부터 다시 보내주어야 함.
        // 조회 및 전송은 비동기로 빼기.
        lastEventId?.let { lastId ->
            // lastId 이후 이벤트들 조회, 전송
        }

        emitters[admin] = emitter
        logger.info("emitters: {}", emitters)
        return ResponseEntity
            .ok()
            .header("Keep-Alive", "timeout=${TIMEOUT_MILLIS}")
            .body(emitter)
    }

    @CrossOrigin(origins = ["*"])
    @PostMapping("/notify")
    fun notify(
        @RequestParam("user") user: String,
        @RequestParam("admin") admin: String,
    ) {
        logger.info("[{}]from user[{}] to admin [{}]", tmpSeq, user, admin)

        tmpSeq++;

        emitters[admin]?.send( // 보내다가 에러가 나는 경우도 이벤트를 저장해야 함.. onError() 에서?
            SseEmitter
                .event()
                .id(tmpSeq.toString())
                .name(ORDER_NOTIFICATION_TYPE)
                .data("${admin}님, [${user}] 유저의 주문이 도착하였습니다.")
        ) ?: run {
            // 이벤트 Redis 저장
        }
    }

    private var tmpSeq = 0L

    @CrossOrigin(origins = ["*"])
    @PostMapping("/notify-all")
    fun notifyAllToAdmins() {
        logger.info("전체 알림")

        emitters.forEach { (admin, emitter) ->
            emitter.send(
                SseEmitter
                    .event()
                    .name(ENTIRE_NOTIFICATION_TYPE)
                    .data("전체 알림 [수신자: ${admin}]")
            )
        }
    }
}