package com.kanban.mobile.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val name: String? = null,
)

@Serializable
data class AuthResponseDto(
    @SerialName("accessToken") val accessToken: String? = null,
    @SerialName("token") val token: String? = null,
    val user: UserDto? = null,
) {
    fun accessTokenValue(): String? = accessToken ?: token
}

@Serializable
data class RefreshResponseDto(
    @SerialName("accessToken") val accessToken: String? = null,
    @SerialName("token") val token: String? = null,
) {
    fun accessTokenValue(): String? = accessToken ?: token
}

@Serializable
data class UserDto(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
)
