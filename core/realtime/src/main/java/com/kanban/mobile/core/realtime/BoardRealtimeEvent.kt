package com.kanban.mobile.core.realtime

/**
 * Normalized realtime signals for the board room.
 * Domain decoding happens in `feature:boards` using the same DTOs as REST.
 */
sealed interface BoardRealtimeEvent {

    data object SocketConnected : BoardRealtimeEvent

    data object SocketDisconnected : BoardRealtimeEvent

    data class SocketConnectError(val message: String) : BoardRealtimeEvent

    /** Server accepted join; [rawPayload] is optional first argument JSON/string. */
    data class BoardJoined(val rawPayload: String?) : BoardRealtimeEvent

    data class BoardJoinError(val message: String) : BoardRealtimeEvent

    data object BoardDeleted : BoardRealtimeEvent

    data class BoardUpdated(val payloadJson: String) : BoardRealtimeEvent

    data class ColumnsUpdated(val payloadJson: String) : BoardRealtimeEvent

    data class CardCreated(val payloadJson: String) : BoardRealtimeEvent

    data class CardUpdated(val payloadJson: String) : BoardRealtimeEvent

    data class CardMoved(val payloadJson: String) : BoardRealtimeEvent

    data class CommentAdded(val payloadJson: String) : BoardRealtimeEvent
}
