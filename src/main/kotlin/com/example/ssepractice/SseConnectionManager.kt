package com.example.ssepractice

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
class SseConnectionManager{
    private val emitters = ConcurrentHashMap<String, SseEmitter>()
    private val logger = LoggerFactory.getLogger(SseConnectionManager::class.java)


    fun addEmitter(
        name: String,
        emitter: SseEmitter,
    ) {
        emitter.onError {
            logger.info("onError 콜백, admin: {}", name)
        }
        emitter.onTimeout {
            logger.info("onTimeout 콜백, admin: {}", name)
            logger.info("onTimeout - emitters: {}", emitters)
        }
        emitter.onCompletion {
            logger.info("onCompletion 콜백, admin: {}", name)
            emitters.remove(name)
            logger.info("onCompletion - emitters: {}", emitters)
        }
        emitters[name] = emitter
    }

    fun sendToDummy(
        target: String,
    ) {
        sendTo(
            target = target,
            comment = "dummy",
            reconnectTime = 1_000L,
        )
    }

    fun sendTo(
        target: String,
        eventType: String? = null,
        data: String? = null,
        comment: String? = null,
        lastEventId: String? = null,
        reconnectTime: Long? = null,
    ) {
        try {
            emitters[target]?.send(
                SseEmitter
                    .event()
                    .apply {
                        eventType?.let { name(eventType) }
                        data?.let { data(data) }
                        comment?.let { comment(comment) }
                        lastEventId?.let { id(lastEventId) }
                        reconnectTime?.let { reconnectTime(reconnectTime) }
                    }
            )
        } catch (e: Exception) {
            emitters.remove(target)
        }
    }

    fun sendToAll(
        eventType: String? = null,
        data: String? = null,
        comment: String? = null,
        lastEventId: String? = null,
        reconnectTime: Long? = null,
    ) {
        emitters.forEach { (key, _) ->
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

}