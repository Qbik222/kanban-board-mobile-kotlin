@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.kanban.mobile.feature.boards

import com.kanban.mobile.core.network.dto.BoardDetailsDto
import com.kanban.mobile.core.network.dto.CardDto
import com.kanban.mobile.core.network.dto.ColumnDto
import com.kanban.mobile.core.realtime.BoardRealtimeEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class BoardSocketReduceResult(
    val nextBoard: BoardDetails?,
    val effects: List<BoardDetailEffect> = emptyList(),
)

@Serializable
private data class ColumnsEnvelopeDto(val columns: List<ColumnDto> = emptyList())

/**
 * Pure merge of socket payloads into [current] board graph.
 * Full snapshots reuse [BoardDetailsDto] + [toDomain] with the same rules as REST (empty members keep previous).
 */
object BoardSocketReducer {

    fun reduce(current: BoardDetails?, event: BoardRealtimeEvent, json: Json): BoardSocketReduceResult =
        when (event) {
            BoardRealtimeEvent.BoardDeleted ->
                BoardSocketReduceResult(
                    nextBoard = null,
                    effects = listOf(
                        BoardDetailEffect.NavigateToBoards,
                        BoardDetailEffect.Snackbar("Дошку видалено"),
                    ),
                )

            is BoardRealtimeEvent.BoardUpdated ->
                reduceBoardUpdated(current, event.payloadJson, json)

            is BoardRealtimeEvent.ColumnsUpdated ->
                reduceColumnsUpdated(current, event.payloadJson, json)

            is BoardRealtimeEvent.CardCreated ->
                reduceCardPayload(current, event.payloadJson, json, isCreate = true)

            is BoardRealtimeEvent.CardUpdated ->
                reduceCardPayload(current, event.payloadJson, json, isCreate = false)

            is BoardRealtimeEvent.CardMoved ->
                reduceCardPayload(current, event.payloadJson, json, isCreate = false)

            is BoardRealtimeEvent.CommentAdded ->
                reduceCardPayload(current, event.payloadJson, json, isCreate = false)

            is BoardRealtimeEvent.BoardJoined,
            is BoardRealtimeEvent.BoardJoinError,
            is BoardRealtimeEvent.SocketConnectError,
            BoardRealtimeEvent.SocketConnected,
            BoardRealtimeEvent.SocketDisconnected,
            -> BoardSocketReduceResult(current)
        }

    private fun reduceBoardUpdated(
        current: BoardDetails?,
        payloadJson: String,
        json: Json,
    ): BoardSocketReduceResult {
        val full = runCatching { json.decodeFromString<BoardDetailsDto>(payloadJson) }.getOrNull()
        if (full != null) {
            return BoardSocketReduceResult(mergeBoardSnapshot(full, current))
        }
        val root = runCatching { json.parseToJsonElement(payloadJson).jsonObject }.getOrNull()
            ?: return BoardSocketReduceResult(current)
        val next = current?.let { b ->
            val title = root.stringOrNull("title") ?: b.title
            val teamId = root.stringOrNull("teamId") ?: b.teamId
            val ownerId = root.stringOrNull("ownerId") ?: b.ownerId
            val projectIds = root.arrayOfStrings("projectIds") ?: b.projectIds
            b.copy(title = title, teamId = teamId, ownerId = ownerId, projectIds = projectIds)
        }
        return BoardSocketReduceResult(next)
    }

    private fun reduceColumnsUpdated(
        current: BoardDetails?,
        payloadJson: String,
        json: Json,
    ): BoardSocketReduceResult {
        if (current == null) return BoardSocketReduceResult(null)
        val cols = runCatching { json.decodeFromString<ColumnsEnvelopeDto>(payloadJson).columns }
            .getOrElse {
                runCatching { json.decodeFromString<List<ColumnDto>>(payloadJson) }.getOrElse { emptyList() }
            }
        if (cols.isEmpty()) return BoardSocketReduceResult(current)
        val mapped = cols.sortedBy { it.order }.map { it.toBoardColumn() }
        return BoardSocketReduceResult(current.copy(columns = mapped.mapIndexed { i, c -> c.copy(order = i) }))
    }

    private fun reduceCardPayload(
        current: BoardDetails?,
        payloadJson: String,
        json: Json,
        isCreate: Boolean,
    ): BoardSocketReduceResult {
        val full = runCatching { json.decodeFromString<BoardDetailsDto>(payloadJson) }.getOrNull()
        if (full != null) {
            return BoardSocketReduceResult(mergeBoardSnapshot(full, current))
        }
        val card = runCatching { json.decodeFromString<CardDto>(payloadJson) }.getOrNull()
            ?: return BoardSocketReduceResult(current)
        val board = current ?: return BoardSocketReduceResult(null)
        val updated = if (isCreate) {
            KanbanBoardReducer.addCard(board, card.toBoardCard())
        } else {
            KanbanBoardReducer.patchCard(board, card.toBoardCard())
        }
        return BoardSocketReduceResult(updated)
    }

    private fun mergeBoardSnapshot(dto: BoardDetailsDto, previous: BoardDetails?): BoardDetails {
        val domain = dto.toDomain()
        val members = domain.members.takeIf { it.isNotEmpty() } ?: previous?.members.orEmpty()
        return domain.copy(members = members)
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.arrayOfStrings(key: String): List<String>? {
        val arr = this[key]?.jsonArray ?: return null
        return arr.mapNotNull { it.jsonPrimitive.contentOrNull }
    }
}
