package com.kanban.mobile.core.network

import com.kanban.mobile.core.network.dto.AuthResponseDto
import com.kanban.mobile.core.network.dto.LoginRequestDto
import com.kanban.mobile.core.network.dto.RefreshResponseDto
import com.kanban.mobile.core.network.dto.RegisterRequestDto
import com.kanban.mobile.core.network.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthResponseDto

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): AuthResponseDto

    @POST("auth/refresh")
    suspend fun refresh(): RefreshResponseDto

    @POST("auth/logout")
    suspend fun logout()

    @GET("auth/me")
    suspend fun me(): UserDto
}
