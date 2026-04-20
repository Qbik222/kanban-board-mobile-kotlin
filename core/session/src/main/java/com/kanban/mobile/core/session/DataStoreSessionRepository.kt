package com.kanban.mobile.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.sessionDataStore by preferencesDataStore(name = "kanban_session")

class DataStoreSessionRepository(
    private val context: Context,
) : SessionRepository {

    private val scope = CoroutineScope(SupervisorJob())
    private val dataStore = context.sessionDataStore

    private val keyAccessToken = stringPreferencesKey("access_token")
    private val keyUserId = stringPreferencesKey("user_id")
    private val keyEmail = stringPreferencesKey("email")

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unknown)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _invalidated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionInvalidatedEvents: SharedFlow<Unit> = _invalidated.asSharedFlow()

    init {
        scope.launch {
            dataStore.data
                .map { prefs ->
                    val token = prefs[keyAccessToken]
                    val uid = prefs[keyUserId]
                    val email = prefs[keyEmail]
                    when {
                        token.isNullOrBlank() -> SessionState.Unauthenticated
                        else -> SessionState.Authenticated(userId = uid, email = email)
                    }
                }
                .distinctUntilChanged()
                .collect { state ->
                    _sessionState.value = state
                }
        }
    }

    override suspend fun getAccessToken(): String? =
        dataStore.data.first()[keyAccessToken]

    override suspend fun setAccessToken(token: String?) {
        dataStore.edit { prefs ->
            if (token.isNullOrBlank()) {
                prefs.remove(keyAccessToken)
            } else {
                prefs[keyAccessToken] = token
            }
        }
    }

    override suspend fun setUserProfile(userId: String?, email: String?) {
        dataStore.edit { prefs ->
            if (userId.isNullOrBlank()) prefs.remove(keyUserId) else prefs[keyUserId] = userId
            if (email.isNullOrBlank()) prefs.remove(keyEmail) else prefs[keyEmail] = email
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
        _sessionState.value = SessionState.Unauthenticated
    }

    override fun notifySessionInvalidated() {
        scope.launch {
            clearSession()
            _invalidated.emit(Unit)
        }
    }

    /** For OkHttp interceptors — blocking read of latest token. */
    fun accessTokenBlocking(): String? = runBlocking { getAccessToken() }
}
