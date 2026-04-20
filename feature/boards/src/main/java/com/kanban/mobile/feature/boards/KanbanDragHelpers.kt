package com.kanban.mobile.feature.boards

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Hit-tests which column the finger is over. [columnRects] maps column id to bounds in root coordinates.
 */
internal fun columnIdAtPoint(
    point: Offset,
    columnRects: Map<String, Rect>,
    sortedColumnIds: List<String>,
): String? {
    for (id in sortedColumnIds) {
        val r = columnRects[id] ?: continue
        if (r.contains(point)) return id
    }
    return null
}

/**
 * Resolves which column contains [fingerInRoot] and the insert-before slot (0..n) from card geometry.
 * Used for drag preview and for drop commit with the same finger position as [onDragEnd].
 */
internal data class DropTarget(
    val columnId: String,
    val insertBeforeSlot: Int,
)

internal fun resolveDropTarget(
    fingerInRoot: Offset,
    board: BoardDetails,
    columnBounds: Map<String, Rect>,
    cardBounds: Map<Pair<String, String>, Rect>,
): DropTarget? {
    if (columnBounds.isEmpty()) return null
    val sortedColumnIds = columnBounds.keys.sortedBy { columnBounds[it]?.left ?: 0f }
    val colId = columnIdAtPoint(fingerInRoot, columnBounds, sortedColumnIds) ?: return null
    val column = board.columns.firstOrNull { it.id == colId } ?: return null
    val rects = column.cards.mapNotNull { c -> cardBounds[colId to c.id] }.sortedBy { it.top }
    val slot = if (rects.isEmpty()) {
        0
    } else {
        insertSlotFromFingerY(fingerInRoot.y, rects)
    }
    return DropTarget(colId, slot)
}

/**
 * Insert slot in 0..cardBounds.size where slot i means "before card i"; size means after last card.
 * [cardBounds] must be ordered top-to-bottom; typically excludes the dragged row if it has zero size.
 */
internal fun insertSlotFromFingerY(fingerY: Float, cardBounds: List<Rect>): Int {
    if (cardBounds.isEmpty()) return 0
    var slot = 0
    for (i in cardBounds.indices) {
        val r = cardBounds[i]
        val midY = (r.top + r.bottom) / 2f
        if (fingerY < midY) return slot
        slot = i + 1
    }
    return cardBounds.size
}

/**
 * Converts a visual "insert before slot" index into [KanbanBoardReducer] / API [newOrderAfterRemoval].
 *
 * @param insertBeforeSlot index in the **current** target column list (0..n), where n is [targetCardsIncludingDragged].size
 */
internal fun newOrderAfterRemoval(
    sourceColumnId: String,
    sourceIndex: Int,
    targetColumnId: String,
    insertBeforeSlot: Int,
    targetCardsIncludingDragged: List<BoardCard>,
): Int {
    val n = targetCardsIncludingDragged.size
    val s = insertBeforeSlot.coerceIn(0, n)
    if (sourceColumnId != targetColumnId) {
        return s.coerceIn(0, n)
    }
    val o = sourceIndex
    val raw = if (s <= o) s else s - 1
    return raw.coerceIn(0, (n - 1).coerceAtLeast(0))
}
