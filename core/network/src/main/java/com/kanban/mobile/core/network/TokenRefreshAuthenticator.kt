package com.kanban.mobile.core.network

import com.kanban.mobile.core.network.dto.RefreshResponseDto
import com.kanban.mobile.core.session.SessionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Route

/**
 * On 401, calls [refreshClient] POST auth/refresh (cookie + CSRF only), saves new access token, retries once.
 */
class TokenRefreshAuthenticator(
    private val apiBaseUrl: String,
    private val json: Json,
    private val refreshClient: OkHttpClient,
    private val sessionRepository: SessionRepository,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        val path = request.url.encodedPath
        if (path.contains("auth/refresh") || path.contains("auth/login") || path.contains("auth/register")) {
            sessionRepository.notifySessionInvalidated()
            return null
        }
        if (request.header(HEADER_RETRY) != null) {
            sessionRepository.notifySessionInvalidated()
            return null
        }
        val newToken = refreshAccessToken() ?: run {
            sessionRepository.notifySessionInvalidated()
            return null
        }
        runBlocking {
            sessionRepository.setAccessToken(newToken)
        }
        return request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .header(HEADER_RETRY, "1")
            .build()
    }

    private fun refreshAccessToken(): String? {
        val url = try {
            "${apiBaseUrl.trimEnd('/')}/auth/refresh".toHttpUrl()
        } catch (_: IllegalArgumentException) {
            return null
        }
        val body = "".toRequestBody(JSON_MEDIA)
        val req = Request.Builder().url(url).post(body).build()
        return try {
            refreshClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return null
                val text = res.body?.string() ?: return null
                json.decodeFromString<RefreshResponseDto>(text).accessTokenValue()
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private const val HEADER_RETRY = "X-Auth-Retry"
    }
}
