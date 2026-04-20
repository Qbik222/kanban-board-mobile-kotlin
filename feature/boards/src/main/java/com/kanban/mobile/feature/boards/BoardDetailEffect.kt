package com.kanban.mobile.feature.boards

sealed interface BoardDetailEffect {
    data class Snackbar(val message: String) : BoardDetailEffect
    data object NavigateToBoards : BoardDetailEffect
    /** Emitted after a column is successfully created on the server (clear local title field). */
    data object ColumnCreated : BoardDetailEffect
}
