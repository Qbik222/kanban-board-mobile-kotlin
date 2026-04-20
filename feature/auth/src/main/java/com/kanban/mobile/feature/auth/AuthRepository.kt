package com.kanban.mobile.feature.auth

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun register(email: String, password: String, name: String?): Result<Unit>

    /** Try cookie refresh if no access token, then load profile via [com.kanban.mobile.core.network.AuthApi.me]. */
    suspend fun bootstrapSession(): Result<Unit>

    suspend fun logout(): Result<Unit>
}
