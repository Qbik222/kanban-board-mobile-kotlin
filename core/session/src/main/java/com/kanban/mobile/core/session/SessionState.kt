package com.kanban.mobile.core.session

sealed interface SessionState {
    data object Unknown : SessionState
    data object Unauthenticated : SessionState
    data class Authenticated(val userId: String?, val email: String?) : SessionState
}
