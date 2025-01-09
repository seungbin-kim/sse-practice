package com.example.ssepractice.repository

import com.example.ssepractice.domain.EventHistoryEntity
import com.example.ssepractice.domain.EventType
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.LocalDateTime

@DataJpaTest
class EventHistoryRepositoryTest @Autowired constructor(
    private val eventHistoryRepository: EventHistoryRepository
) {

    @Test
    @DisplayName("lastId 이후 이벤트를 정상적으로 조회")
    fun get_EventHistories_GraterThan_LastId() {
        // given
        eventHistoryRepository.saveAll(listOf(
            EventHistoryEntity(
                type = EventType.ORDER_NOTIFICATION,
                target = "1",
                dateTime = LocalDateTime.now(),
                message = "test1",
            ),
            EventHistoryEntity(
                type = EventType.ORDER_NOTIFICATION,
                target = "1",
                dateTime = LocalDateTime.now(),
                message = "test2",
            ),
            EventHistoryEntity(
                type = EventType.ENTIRE_NOTIFICATION,
                target = "ALL",
                dateTime = LocalDateTime.now(),
                message = "test3",
            ),
            EventHistoryEntity(
                type = EventType.ORDER_NOTIFICATION,
                target = "2",
                dateTime = LocalDateTime.now(),
                message = "test4",
            ),
            EventHistoryEntity(
                type = EventType.ORDER_NOTIFICATION,
                target = "1",
                dateTime = LocalDateTime.now(),
                message = "test5",
            )
        ))

        // when
        val results = eventHistoryRepository.findByTargetAndTypeAndIdGreaterThan(
            target = "1",
            type = EventType.ORDER_NOTIFICATION,
            id = 1L,
        )

        // then
        assertThat(results).hasSize(2)
        assertThat(results).extracting("type")
            .containsExactlyInAnyOrder(EventType.ORDER_NOTIFICATION, EventType.ORDER_NOTIFICATION)
        assertThat(results).extracting("target")
            .containsExactlyInAnyOrder("1", "1")
        assertThat(results).extracting("id")
            .containsExactlyInAnyOrder(2L, 5L)
    }

}