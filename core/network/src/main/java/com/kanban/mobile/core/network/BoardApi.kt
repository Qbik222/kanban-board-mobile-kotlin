package com.kanban.mobile.core.network

import com.kanban.mobile.core.network.dto.BoardDetailsDto
import com.kanban.mobile.core.network.dto.BoardSummaryDto
import com.kanban.mobile.core.network.dto.CreateBoardRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BoardApi {

    @GET("boards")
    suspend fun listBoards(): List<BoardSummaryDto>

    @POST("boards")
    suspend fun createBoard(@Body body: CreateBoardRequestDto): BoardSummaryDto

    @GET("boards/{id}")
    suspend fun getBoard(@Path("id") id: String): BoardDetailsDto
}
