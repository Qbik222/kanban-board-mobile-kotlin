package com.kanban.mobile.feature.boards

interface BoardRepository {

    suspend fun listBoards(): Result<List<BoardSummary>>

    suspend fun getBoard(id: String): Result<BoardDetails>

    suspend fun createBoard(
        title: String,
        teamId: String,
        projectIds: List<String>?,
    ): Result<BoardSummary>
}
