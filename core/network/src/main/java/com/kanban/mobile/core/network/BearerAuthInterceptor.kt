package com.kanban.mobile.core.network

import okhttp3.Interceptor
import okhttp3.Response

class BearerAuthInterceptor(
    private val accessTokenProvider: () -> String?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = accessTokenProvider() ?: return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
