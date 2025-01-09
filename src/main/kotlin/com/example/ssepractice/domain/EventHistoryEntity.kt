package com.example.ssepractice.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "EVENT_HISTORY",
    indexes = [
        Index(name = "EVENT_HISTORY_IDX_1", columnList = "target, type, id")
    ]
)
class EventHistoryEntity(
    val dateTime: LocalDateTime, // 년-월 까지 파티션키로?

    @Enumerated(EnumType.STRING)
    val type: EventType,

    val target: String,

    @Column(length = 1000)
    val message: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)