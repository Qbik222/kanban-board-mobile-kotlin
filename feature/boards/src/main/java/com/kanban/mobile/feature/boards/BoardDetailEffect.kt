package com.kanban.mobile.feature.boards

sealed interface BoardDetailEffect {
    data class Snackbar(val message: String) : BoardDetailEffect
    data object NavigateToBoards : BoardDetailEffect
}
