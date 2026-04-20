package com.kanban.mobile.core.realtime

import kotlinx.coroutines.flow.SharedFlow

interface BoardRealtimeClient {

    val events: SharedFlow<BoardRealtimeEvent>

    fun connectIfNeeded()

    fun joinBoard(boardId: String)

    /** Screen disposed: stop treating server pushes as targeting this board (no global disconnect). */
    fun leaveBoard()

    /** Logout / teardown: disconnect socket and remove listeners. */
    fun disconnect()
}
