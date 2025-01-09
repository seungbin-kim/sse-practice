package com.example.ssepractice.service

import com.example.ssepractice.domain.EventHistoryEntity
import com.example.ssepractice.domain.EventType
import com.example.ssepractice.repository.EventHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class EventHistoryService(
    private val eventHistoryRepository: EventHistoryRepository,
) {

    fun getEventHistoriesBy(
        eventType: EventType,
        lastId: Long,
        target: String,
    ): List<EventHistoryEntity> {

        return eventHistoryRepository.findByTargetAndTypeAndIdGreaterThan(target, eventType, lastId)
    }

    fun saveHistory(
        eventType: EventType,
        target: String,
        message: String,
    ): EventHistoryEntity {

        return eventHistoryRepository.save(
            EventHistoryEntity(
                type = eventType,
                target = target,
                message = message,
                dateTime = LocalDateTime.now(),
            ))
    }

}