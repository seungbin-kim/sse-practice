package com.example.ssepractice.repository

import com.example.ssepractice.domain.EventHistoryEntity
import com.example.ssepractice.domain.EventType
import org.springframework.data.jpa.repository.JpaRepository

interface EventHistoryRepository : JpaRepository<EventHistoryEntity, Long> {

    fun findByTargetAndTypeAndIdGreaterThan(target: String, type: EventType, id: Long): List<EventHistoryEntity>

}