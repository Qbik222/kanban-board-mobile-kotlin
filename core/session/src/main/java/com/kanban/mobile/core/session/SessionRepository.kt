package com.kanban.mobile.core.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val sessionState: StateFlow<SessionState>

    /** Raw access token from DataStore; updates when refresh writes a new token. */
    val accessTokenFlow: Flow<String?>

    /** In-memory + DataStore; use from coroutines. */
    suspend fun getAccessToken(): String?

    suspend fun setAccessToken(token: String?)

    suspend fun setUserProfile(userId: String?, email: String?)

    suspend fun clearSession()

    /** After refresh fails or explicit 401 handling — forces UI to auth. */
    fun notifySessionInvalidated()

    val sessionInvalidatedEvents: SharedFlow<Unit>
}
