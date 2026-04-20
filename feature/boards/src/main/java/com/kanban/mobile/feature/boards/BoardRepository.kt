package com.kanban.mobile.feature.boards

interface BoardRepository {

    suspend fun listBoards(): Result<List<BoardSummary>>

    suspend fun getBoard(id: String): Result<BoardDetails>

    suspend fun createBoard(
        title: String,
        teamId: String,
        projectIds: List<String>?,
    ): Result<BoardSummary>

    suspend fun createColumn(boardId: String, title: String): Result<BoardColumn>

    suspend fun reorderColumns(columnOrders: List<Pair<String, Int>>): Result<Unit>

    suspend fun renameColumn(columnId: String, title: String): Result<Unit>

    suspend fun deleteColumn(columnId: String): Result<Unit>

    suspend fun createCard(
        title: String,
        description: String?,
        columnId: String,
        assigneeId: String?,
        projectIds: List<String>?,
        priority: String?,
        deadlineDueAt: String?,
    ): Result<BoardCard>

    suspend fun patchCard(
        cardId: String,
        title: String?,
        description: String?,
        priority: String?,
        assigneeId: String?,
        projectIds: List<String>?,
        deadlineDueAt: String?,
    ): Result<BoardCard>

    suspend fun moveCard(cardId: String, targetColumnId: String, newOrder: Int): Result<BoardCard>

    suspend fun deleteCard(cardId: String): Result<Unit>

    suspend fun addComment(cardId: String, text: String): Result<CardComment>

    suspend fun deleteComment(cardId: String, commentId: String): Result<Unit>

    suspend fun listBoardMembers(boardId: String): Result<List<BoardMember>>

    suspend fun inviteBoardMember(boardId: String, userId: String): Result<Unit>

    suspend fun updateBoardMemberRole(
        boardId: String,
        userId: String,
        role: BoardRole,
    ): Result<Unit>

    suspend fun removeBoardMember(boardId: String, userId: String): Result<Unit>

    suspend fun updateBoard(
        boardId: String,
        title: String,
        projectIds: List<String>,
    ): Result<Unit>

    suspend fun deleteBoard(boardId: String): Result<Unit>
}
