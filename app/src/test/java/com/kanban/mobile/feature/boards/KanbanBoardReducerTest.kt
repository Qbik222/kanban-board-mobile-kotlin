package com.kanban.mobile.feature.boards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KanbanBoardReducerTest {

    private fun sampleBoard(): BoardDetails =
        BoardDetails(
            id = "b",
            title = "B",
            teamId = "t",
            ownerId = "o",
            projectIds = emptyList(),
            members = emptyList(),
            columns = listOf(
                BoardColumn(
                    id = "c1",
                    title = "Todo",
                    order = 0,
                    cards = listOf(
                        boardCard("a", "c1", 0),
                        boardCard("b", "c1", 1),
                    ),
                ),
                BoardColumn(
                    id = "c2",
                    title = "Done",
                    order = 1,
                    cards = listOf(boardCard("x", "c2", 0)),
                ),
            ),
        )

    private fun boardCard(id: String, columnId: String, order: Int): BoardCard =
        BoardCard(
            id = id,
            title = id,
            description = null,
            priority = null,
            columnId = columnId,
            order = order,
            assigneeId = null,
            projectIds = emptyList(),
            comments = emptyList(),
            deadline = null,
        )

    @Test
    fun applyCardMove_crossColumn() {
        val board = sampleBoard()
        val moved = KanbanBoardReducer.applyCardMove(board, "a", "c2", newOrder = 1)
        val c2 = moved.columns.first { it.id == "c2" }
        assertEquals(listOf("x", "a"), c2.cards.map { it.id })
        val c1 = moved.columns.first { it.id == "c1" }
        assertEquals(listOf("b"), c1.cards.map { it.id })
        assertEquals(0, c1.cards[0].order)
        assertEquals(1, c2.cards[1].order)
    }

    @Test
    fun mergeCard_insertsReturnedCard() {
        val board = sampleBoard()
        val updated = boardCard("a", "c2", 0).copy(title = "A!")
        val merged = KanbanBoardReducer.mergeCard(board, updated)
        assertNull(merged.columns.flatMap { it.cards }.find { it.id == "a" && it.columnId == "c1" })
        assertEquals("A!", merged.columns.first { it.id == "c2" }.cards.first().title)
    }

    @Test
    fun reorderColumns_newOrder() {
        val board = sampleBoard()
        val r = KanbanBoardReducer.reorderColumns(board, listOf("c2", "c1"))
        assertEquals(listOf("c2", "c1"), r.columns.map { it.id })
        assertEquals(0, r.columns[0].order)
        assertEquals(1, r.columns[1].order)
    }
}
