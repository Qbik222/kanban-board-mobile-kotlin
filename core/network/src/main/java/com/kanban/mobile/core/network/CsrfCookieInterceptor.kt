package com.kanban.mobile.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Double-submit CSRF: copies value from cookie [NetworkConfig.csrfCookieName] into header [NetworkConfig.csrfHeaderName]
 * for mutating requests (POST, PUT, PATCH, DELETE).
 */
class CsrfCookieInterceptor(
    private val config: NetworkConfig,
    private val cookieJar: ClearableCookieJar,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val method = request.method
        if (method !in CSRF_METHODS) {
            return chain.proceed(request)
        }
        val url = request.url
        val cookies = cookieJar.loadForRequest(url)
        val token = cookies.find { it.name == config.csrfCookieName }?.value
        val builder = request.newBuilder()
        if (!token.isNullOrBlank()) {
            builder.header(config.csrfHeaderName, token)
        }
        return chain.proceed(builder.build())
    }

    companion object {
        private val CSRF_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}
