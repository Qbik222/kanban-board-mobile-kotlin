package com.kanban.mobile.feature.boards

import com.kanban.mobile.core.realtime.BoardRealtimeEvent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardSocketReducerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun sampleBoard(): BoardDetails =
        BoardDetails(
            id = "b1",
            title = "Old",
            teamId = "t1",
            ownerId = "o1",
            projectIds = listOf("p1"),
            members = listOf(BoardMember("u1", BoardRole.OWNER, email = "a@b.c")),
            columns = listOf(
                BoardColumn(
                    id = "c1",
                    title = "Todo",
                    order = 0,
                    cards = listOf(
                        BoardCard(
                            id = "k1",
                            title = "A",
                            description = null,
                            priority = null,
                            columnId = "c1",
                            order = 0,
                            assigneeId = null,
                            projectIds = emptyList(),
                            comments = emptyList(),
                            deadlineDueAt = null,
                        ),
                    ),
                ),
            ),
        )

    @Test
    fun columnsUpdated_replacesColumns() {
        val current = sampleBoard()
        val payload =
            """{"columns":[{"id":"c2","title":"Done","order":0,"cards":[]}]}"""
        val r = BoardSocketReducer.reduce(
            current,
            BoardRealtimeEvent.ColumnsUpdated(payload),
            json,
        )
        assertEquals(1, r.nextBoard!!.columns.size)
        assertEquals("c2", r.nextBoard!!.columns[0].id)
        assertEquals("Done", r.nextBoard!!.columns[0].title)
    }

    @Test
    fun cardMoved_fullSnapshot_restoresOrder() {
        val current = sampleBoard()
        val payload =
            """{"id":"b1","title":"Old","teamId":"t1","ownerId":"o1","projectIds":["p1"],"members":[],"columns":[{"id":"c1","title":"Todo","order":0,"cards":[{"id":"k2","title":"Z","columnId":"c1","order":0},{"id":"k1","title":"A","columnId":"c1","order":1}]}]}"""
        val r = BoardSocketReducer.reduce(
            current,
            BoardRealtimeEvent.CardMoved(payload),
            json,
        )
        val ids = r.nextBoard!!.columns[0].cards.map { it.id }
        assertEquals(listOf("k2", "k1"), ids)
    }

    @Test
    fun boardDeleted_navigateAndSnackbar() {
        val r = BoardSocketReducer.reduce(
            sampleBoard(),
            BoardRealtimeEvent.BoardDeleted,
            json,
        )
        assertEquals(null, r.nextBoard)
        assertTrue(r.effects.any { it is BoardDetailEffect.NavigateToBoards })
        assertTrue(r.effects.any { it is BoardDetailEffect.Snackbar })
    }
}
