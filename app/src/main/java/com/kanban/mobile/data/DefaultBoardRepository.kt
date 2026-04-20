package com.kanban.mobile.data

import com.kanban.mobile.core.network.BoardApi
import com.kanban.mobile.core.network.dto.BoardDetailsDto
import com.kanban.mobile.core.network.dto.BoardSummaryDto
import com.kanban.mobile.core.network.dto.CardDeadlineDto
import com.kanban.mobile.core.network.dto.CardDto
import com.kanban.mobile.core.network.dto.ColumnDto
import com.kanban.mobile.core.network.dto.BoardMemberDto
import com.kanban.mobile.core.network.dto.ColumnOrderEntryDto
import com.kanban.mobile.core.network.dto.CreateBoardRequestDto
import com.kanban.mobile.core.network.dto.InviteBoardMemberRequestDto
import com.kanban.mobile.core.network.dto.PatchBoardMemberRoleRequestDto
import com.kanban.mobile.core.network.dto.PatchBoardRequestDto
import com.kanban.mobile.core.network.dto.CreateCardCommentRequestDto
import com.kanban.mobile.core.network.dto.CreateCardRequestDto
import com.kanban.mobile.core.network.dto.CreateColumnRequestDto
import com.kanban.mobile.core.network.dto.MoveCardRequestDto
import com.kanban.mobile.core.network.dto.PatchCardRequestDto
import com.kanban.mobile.core.network.dto.PatchColumnRequestDto
import com.kanban.mobile.core.network.dto.ReorderColumnsRequestDto
import com.kanban.mobile.feature.boards.BoardCard
import com.kanban.mobile.feature.boards.BoardColumn
import com.kanban.mobile.feature.boards.BoardDetails
import com.kanban.mobile.feature.boards.BoardMember
import com.kanban.mobile.feature.boards.BoardRepository
import com.kanban.mobile.feature.boards.BoardRole
import com.kanban.mobile.feature.boards.BoardSummary
import com.kanban.mobile.feature.boards.CardComment
import com.kanban.mobile.feature.boards.CardDeadline
import com.kanban.mobile.feature.boards.toBoardCard
import com.kanban.mobile.feature.boards.toBoardColumn
import com.kanban.mobile.feature.boards.toDomain
import com.kanban.mobile.feature.boards.toMember
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class DefaultBoardRepository @Inject constructor(
    private val boardApi: BoardApi,
) : BoardRepository {

    private val detailsCache = ConcurrentHashMap<String, BoardDetails>()

    override suspend fun listBoards(): Result<List<BoardSummary>> = withContext(Dispatchers.IO) {
        runCatching { boardApi.listBoards().map { it.toDomain() } }
    }

    override suspend fun getBoard(id: String): Result<BoardDetails> = withContext(Dispatchers.IO) {
        runCatching {
            val dto = boardApi.getBoard(id)
            val members = dto.members.takeIf { it.isNotEmpty() }
                ?: runCatching { boardApi.getBoardMembers(id) }.getOrElse { emptyList() }
            dto.copy(members = members).toDomain().also { detailsCache[id] = it }
        }
    }

    override suspend fun createBoard(
        title: String,
        teamId: String,
        projectIds: List<String>?,
    ): Result<BoardSummary> = withContext(Dispatchers.IO) {
        runCatching {
            detailsCache.clear()
            boardApi.createBoard(
                CreateBoardRequestDto(
                    title = title,
                    teamId = teamId,
                    projectIds = projectIds?.takeIf { it.isNotEmpty() },
                ),
            ).toDomain()
        }
    }

    override suspend fun createColumn(boardId: String, title: String): Result<BoardColumn> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.createColumn(CreateColumnRequestDto(title = title, boardId = boardId)).toBoardColumn()
            }
        }

    override suspend fun reorderColumns(columnOrders: List<Pair<String, Int>>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.reorderColumns(
                    ReorderColumnsRequestDto(
                        columnOrders.map { ColumnOrderEntryDto(it.first, it.second) },
                    ),
                )
            }
        }

    override suspend fun renameColumn(columnId: String, title: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.patchColumn(columnId, PatchColumnRequestDto(title = title))
                Unit
            }
        }

    override suspend fun deleteColumn(columnId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.deleteColumn(columnId)
            }
        }

    override suspend fun createCard(
        title: String,
        description: String?,
        columnId: String,
        assigneeId: String?,
        projectIds: List<String>?,
        priority: String?,
        deadline: CardDeadline?,
    ): Result<BoardCard> = withContext(Dispatchers.IO) {
        runCatching {
            boardApi.createCard(
                CreateCardRequestDto(
                    title = title,
                    description = description,
                    columnId = columnId,
                    assigneeId = assigneeId,
                    projectIds = projectIds?.takeIf { it.isNotEmpty() },
                    priority = priority,
                    deadline = deadline.toDto(),
                ),
            ).toBoardCard()
        }
    }

    override suspend fun patchCard(
        cardId: String,
        title: String?,
        description: String?,
        priority: String?,
        assigneeId: String?,
        projectIds: List<String>?,
        deadline: CardDeadline?,
    ): Result<BoardCard> = withContext(Dispatchers.IO) {
        runCatching {
            boardApi.patchCard(
                cardId,
                PatchCardRequestDto(
                    title = title,
                    description = description,
                    priority = priority,
                    assigneeId = assigneeId,
                    projectIds = projectIds,
                    deadline = deadline.toDto(),
                ),
            ).toBoardCard()
        }
    }

    override suspend fun moveCard(
        cardId: String,
        targetColumnId: String,
        newOrder: Int,
    ): Result<BoardCard> = withContext(Dispatchers.IO) {
        runCatching {
            boardApi.moveCard(cardId, MoveCardRequestDto(targetColumnId, newOrder)).toBoardCard()
        }
    }

    override suspend fun deleteCard(cardId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.deleteCard(cardId)
            }
        }

    override suspend fun addComment(cardId: String, text: String): Result<CardComment> =
        withContext(Dispatchers.IO) {
            runCatching {
                val card =
                    boardApi.createComment(cardId, CreateCardCommentRequestDto(text = text))
                val dto = card.comments.lastOrNull()
                    ?: error("Server did not return comments on card after createComment")
                CardComment(id = dto.id, body = dto.body, userId = dto.userId)
            }
        }

    override suspend fun deleteComment(cardId: String, commentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.deleteComment(cardId, commentId)
                Unit
            }
        }

    override suspend fun listBoardMembers(boardId: String): Result<List<BoardMember>> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.getBoardMembers(boardId).map { it.toMember() }
            }
        }

    override suspend fun inviteBoardMember(boardId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.inviteBoardMember(boardId, InviteBoardMemberRequestDto(userId))
                refreshMembersCache(boardId)
            }
        }

    override suspend fun updateBoardMemberRole(
        boardId: String,
        userId: String,
        role: BoardRole,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.patchBoardMemberRole(
                    boardId,
                    userId,
                    PatchBoardMemberRoleRequestDto(role = role.toApi()),
                )
                refreshMembersCache(boardId)
            }
        }

    override suspend fun removeBoardMember(boardId: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.deleteBoardMember(boardId, userId)
                refreshMembersCache(boardId)
            }
        }

    override suspend fun updateBoard(
        boardId: String,
        title: String,
        projectIds: List<String>,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.patchBoard(boardId, PatchBoardRequestDto(title = title, projectIds = projectIds))
                detailsCache[boardId]?.let { cached ->
                    detailsCache[boardId] = cached.copy(title = title, projectIds = projectIds)
                }
                Unit
            }
        }

    override suspend fun deleteBoard(boardId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.deleteBoard(boardId)
                detailsCache.remove(boardId)
                Unit
            }
        }

    private fun CardDeadline?.toDto(): CardDeadlineDto? {
        val d = this ?: return null
        val start = d.startDate?.takeIf { it.isNotBlank() }
        val end = d.endDate?.takeIf { it.isNotBlank() }
        val due = d.dueAt?.takeIf { it.isNotBlank() }
        if (start == null && end == null && due == null) return null
        return CardDeadlineDto(startDate = start, endDate = end, dueAt = due)
    }

    private suspend fun refreshMembersCache(boardId: String) {
        val members = boardApi.getBoardMembers(boardId).map { it.toMember() }
        detailsCache[boardId]?.let { cached ->
            detailsCache[boardId] = cached.copy(members = members)
        }
    }
}
