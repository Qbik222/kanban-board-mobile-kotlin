package com.kanban.mobile.feature.boards

/**
 * Pure board state transforms for optimistic UI and tests.
 */
object KanbanBoardReducer {

    /**
     * [newOrder] is the zero-based index within the target column **after** the card is removed
     * from its source column (same semantics as PATCH /cards/{id}/move).
     */
    fun applyCardMove(
        board: BoardDetails,
        cardId: String,
        targetColumnId: String,
        newOrder: Int,
    ): BoardDetails {
        val cardLists = board.columns.map { ArrayList(it.cards) }

        var moved: BoardCard? = null
        run findCard@{
            cardLists.forEach { cards ->
                val idx = cards.indexOfFirst { it.id == cardId }
                if (idx >= 0) {
                    moved = cards.removeAt(idx)
                    return@findCard
                }
            }
        }
        val card = moved ?: return board
        val targetColIndex = board.columns.indexOfFirst { it.id == targetColumnId }
        if (targetColIndex < 0) return board
        val list = cardLists[targetColIndex]
        val insertAt = newOrder.coerceIn(0, list.size)
        list.add(insertAt, card.copy(columnId = targetColumnId))

        return board.copy(
            columns = board.columns.indices.map { i ->
                val col = board.columns[i]
                col.copy(
                    cards = cardLists[i].mapIndexed { index, c ->
                        c.copy(order = index, columnId = col.id)
                    },
                )
            }.sortedBy { it.order },
        )
    }

    /** Applies server-returned card (e.g. after move) into the board graph. */
    fun mergeCard(board: BoardDetails, updated: BoardCard): BoardDetails {
        val cols = board.columns.map { col ->
            col.copy(cards = col.cards.filter { it.id != updated.id }.toMutableList())
        }.toMutableList()
        val targetIndex = cols.indexOfFirst { it.id == updated.columnId }
        if (targetIndex < 0) return board
        val targetCol = cols[targetIndex]
        val list = targetCol.cards.toMutableList()
        val pos = updated.order.coerceIn(0, list.size)
        list.add(pos, updated.copy(columnId = targetCol.id))
        cols[targetIndex] = targetCol.copy(cards = list.toList())
        return board.copy(
            columns = cols
                .map { col ->
                    col.copy(
                        cards = col.cards.mapIndexed { index, c ->
                            c.copy(order = index, columnId = col.id)
                        },
                    )
                }
                .sortedBy { it.order },
        )
    }

    fun reorderColumns(board: BoardDetails, orderedColumnIds: List<String>): BoardDetails {
        val map = board.columns.associateBy { it.id }
        val known = orderedColumnIds.mapNotNull { map[it] }
        val tail = board.columns.filter { it.id !in orderedColumnIds.toSet() }
        return board.copy(
            columns = (known + tail).mapIndexed { index, col -> col.copy(order = index) },
        )
    }

    fun removeColumn(board: BoardDetails, columnId: String): BoardDetails =
        board.copy(columns = board.columns.filter { it.id != columnId }.mapIndexed { i, c -> c.copy(order = i) })

    fun upsertColumn(board: BoardDetails, column: BoardColumn): BoardDetails {
        val without = board.columns.filter { it.id != column.id }
        val merged = (without + column).sortedBy { it.order }
        return board.copy(columns = merged.mapIndexed { index, col -> col.copy(order = index) })
    }

    fun renameColumn(board: BoardDetails, columnId: String, title: String): BoardDetails =
        board.copy(
            columns = board.columns.map {
                if (it.id == columnId) it.copy(title = title) else it
            },
        )

    fun addCard(board: BoardDetails, card: BoardCard): BoardDetails {
        val cols = board.columns.map { col ->
            if (col.id != card.columnId) col else {
                val next = col.cards + card.copy(
                    order = col.cards.size,
                    columnId = col.id,
                )
                col.copy(cards = next.mapIndexed { i, c -> c.copy(order = i, columnId = col.id) })
            }
        }
        return board.copy(columns = cols.sortedBy { it.order })
    }

    fun removeCard(board: BoardDetails, cardId: String): BoardDetails =
        board.copy(
            columns = board.columns.map { col ->
                val filtered = col.cards.filter { it.id != cardId }
                col.copy(cards = filtered.mapIndexed { i, c -> c.copy(order = i, columnId = col.id) })
            },
        )

    fun patchCard(board: BoardDetails, updated: BoardCard): BoardDetails =
        board.copy(
            columns = board.columns.map { col ->
                col.copy(cards = col.cards.map { c -> if (c.id == updated.id) updated else c })
            },
        )

    fun addComment(board: BoardDetails, cardId: String, comment: CardComment): BoardDetails =
        board.copy(
            columns = board.columns.map { col ->
                col.copy(
                    cards = col.cards.map { c ->
                        if (c.id != cardId) c
                        else c.copy(comments = c.comments + comment)
                    },
                )
            },
        )

    fun removeComment(board: BoardDetails, cardId: String, commentId: String): BoardDetails =
        board.copy(
            columns = board.columns.map { col ->
                col.copy(
                    cards = col.cards.map { c ->
                        if (c.id != cardId) c
                        else c.copy(comments = c.comments.filter { it.id != commentId })
                    },
                )
            },
        )
}
