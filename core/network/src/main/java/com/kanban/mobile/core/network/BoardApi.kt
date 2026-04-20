package com.kanban.mobile.core.network

import com.kanban.mobile.core.network.dto.BoardDetailsDto
import com.kanban.mobile.core.network.dto.BoardMemberDto
import com.kanban.mobile.core.network.dto.BoardSummaryDto
import com.kanban.mobile.core.network.dto.CardCommentDto
import com.kanban.mobile.core.network.dto.CardDto
import com.kanban.mobile.core.network.dto.ColumnDto
import com.kanban.mobile.core.network.dto.CreateBoardRequestDto
import com.kanban.mobile.core.network.dto.CreateCardCommentRequestDto
import com.kanban.mobile.core.network.dto.CreateCardRequestDto
import com.kanban.mobile.core.network.dto.CreateColumnRequestDto
import com.kanban.mobile.core.network.dto.MoveCardRequestDto
import com.kanban.mobile.core.network.dto.PatchCardRequestDto
import com.kanban.mobile.core.network.dto.PatchColumnRequestDto
import com.kanban.mobile.core.network.dto.ReorderColumnsRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface BoardApi {

    @GET("boards")
    suspend fun listBoards(): List<BoardSummaryDto>

    @POST("boards")
    suspend fun createBoard(@Body body: CreateBoardRequestDto): BoardSummaryDto

    @GET("boards/{id}")
    suspend fun getBoard(@Path("id") id: String): BoardDetailsDto

    @GET("boards/{boardId}/members")
    suspend fun getBoardMembers(@Path("boardId") boardId: String): List<BoardMemberDto>

    @POST("columns")
    suspend fun createColumn(@Body body: CreateColumnRequestDto): ColumnDto

    @PATCH("columns/reorder")
    suspend fun reorderColumns(@Body body: ReorderColumnsRequestDto)

    @PATCH("columns/{id}")
    suspend fun patchColumn(
        @Path("id") id: String,
        @Body body: PatchColumnRequestDto,
    ): ColumnDto

    @DELETE("columns/{id}")
    suspend fun deleteColumn(@Path("id") id: String)

    @POST("cards")
    suspend fun createCard(@Body body: CreateCardRequestDto): CardDto

    @PATCH("cards/{id}")
    suspend fun patchCard(
        @Path("id") id: String,
        @Body body: PatchCardRequestDto,
    ): CardDto

    /** Server returns updated card snapshot; refresh board if the payload is incomplete. */
    @PATCH("cards/{id}/move")
    suspend fun moveCard(
        @Path("id") id: String,
        @Body body: MoveCardRequestDto,
    ): CardDto

    @DELETE("cards/{id}")
    suspend fun deleteCard(@Path("id") id: String)

    @POST("cards/{cardId}/comments")
    suspend fun createComment(
        @Path("cardId") cardId: String,
        @Body body: CreateCardCommentRequestDto,
    ): CardCommentDto

    @DELETE("cards/{cardId}/comments/{commentId}")
    suspend fun deleteComment(
        @Path("cardId") cardId: String,
        @Path("commentId") commentId: String,
    )
}
