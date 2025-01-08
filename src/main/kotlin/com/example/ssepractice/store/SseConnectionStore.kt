package com.example.ssepractice.store

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
class SseConnectionStore(
    private val emitters: ConcurrentHashMap<String, SseEmitter> = ConcurrentHashMap(),
) {

    val emitterEntries: String
        get() = emitters.entries.joinToString("\n")

    fun storeEmitter(
        name: String,
        emitter: SseEmitter,
    ) {
        emitters[name] = emitter
    }

    fun removeEmitter(name: String) {
        emitters.remove(name)
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
                        eventType?.run { name(eventType) }
                        data?.run { data(data) }
                        comment?.run { comment(comment) }
                        lastEventId?.run { id(lastEventId) }
                        reconnectTime?.run { reconnectTime(reconnectTime) }
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