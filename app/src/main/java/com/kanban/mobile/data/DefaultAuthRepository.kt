package com.kanban.mobile.data

import com.kanban.mobile.core.network.AuthApi
import com.kanban.mobile.core.network.ClearableCookieJar
import com.kanban.mobile.core.realtime.BoardRealtimeClient
import com.kanban.mobile.core.network.dto.LoginRequestDto
import com.kanban.mobile.core.network.dto.RegisterRequestDto
import com.kanban.mobile.core.session.SessionRepository
import com.kanban.mobile.feature.auth.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionRepository: SessionRepository,
    private val cookieJar: ClearableCookieJar,
    private val boardRealtimeClient: BoardRealtimeClient,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val body = LoginRequestDto(email = email, password = password)
        val res = authApi.login(body)
        val token = res.accessTokenValue() ?: error("Missing access token")
        sessionRepository.setAccessToken(token)
        val user = res.user
        sessionRepository.setUserProfile(userId = user?.id, email = user?.email)
    }

    override suspend fun register(email: String, password: String, name: String?): Result<Unit> = runCatching {
        val body = RegisterRequestDto(email = email, password = password, name = name)
        val res = authApi.register(body)
        val token = res.accessTokenValue() ?: error("Missing access token")
        sessionRepository.setAccessToken(token)
        val user = res.user
        sessionRepository.setUserProfile(userId = user?.id, email = user?.email)
    }

    override suspend fun bootstrapSession(): Result<Unit> = runCatching {
        val existing = sessionRepository.getAccessToken()
        if (existing.isNullOrBlank()) {
            val refresh = authApi.refresh()
            sessionRepository.setAccessToken(
                refresh.accessTokenValue() ?: error("Missing access token from refresh"),
            )
        }
        val me = authApi.me()
        sessionRepository.setUserProfile(userId = me.id, email = me.email)
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        try {
            authApi.logout()
        } catch (_: Exception) {
            // still clear local session
        }
        sessionRepository.clearSession()
        boardRealtimeClient.disconnect()
        cookieJar.clear()
    }
}
