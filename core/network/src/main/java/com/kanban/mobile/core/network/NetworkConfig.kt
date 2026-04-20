package com.kanban.mobile.core.network

data class NetworkConfig(
    val apiBaseUrl: String,
    val csrfCookieName: String,
    val csrfHeaderName: String,
)
