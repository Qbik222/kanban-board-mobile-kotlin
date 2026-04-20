package com.kanban.mobile.data

import com.kanban.mobile.core.network.BoardApi
import com.kanban.mobile.core.network.dto.BoardDetailsDto
import com.kanban.mobile.core.network.dto.BoardSummaryDto
import com.kanban.mobile.core.network.dto.CardDeadlineDto
import com.kanban.mobile.core.network.dto.CardDto
import com.kanban.mobile.core.network.dto.ColumnDto
import com.kanban.mobile.core.network.dto.ColumnOrderEntryDto
import com.kanban.mobile.core.network.dto.CreateBoardRequestDto
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
                boardApi.createColumn(CreateColumnRequestDto(title = title, boardId = boardId)).toDomain()
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
        deadlineDueAt: String?,
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
                    deadline = deadlineDueAt?.let { CardDeadlineDto(dueAt = it) },
                ),
            ).toCard()
        }
    }

    override suspend fun patchCard(
        cardId: String,
        title: String?,
        description: String?,
        priority: String?,
        assigneeId: String?,
        projectIds: List<String>?,
        deadlineDueAt: String?,
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
                    deadline = deadlineDueAt?.let { CardDeadlineDto(dueAt = it) },
                ),
            ).toCard()
        }
    }

    override suspend fun moveCard(
        cardId: String,
        targetColumnId: String,
        newOrder: Int,
    ): Result<BoardCard> = withContext(Dispatchers.IO) {
        runCatching {
            boardApi.moveCard(cardId, MoveCardRequestDto(targetColumnId, newOrder)).toCard()
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
                val dto = boardApi.createComment(cardId, CreateCardCommentRequestDto(text = text))
                CardComment(id = dto.id, body = dto.body, userId = dto.userId)
            }
        }

    override suspend fun deleteComment(cardId: String, commentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                boardApi.deleteComment(cardId, commentId)
            }
        }

    private fun BoardSummaryDto.toDomain(): BoardSummary =
        BoardSummary(
            id = id,
            title = title,
            teamId = teamId,
            ownerId = ownerId,
            projectIds = projectIds,
        )

    private fun BoardDetailsDto.toDomain(): BoardDetails =
        BoardDetails(
            id = id,
            title = title,
            teamId = teamId,
            ownerId = ownerId,
            projectIds = projectIds,
            members = members.map {
                BoardMember(userId = it.userId, role = BoardRole.fromApi(it.role))
            },
            columns = columns.sortedBy { it.order }.map { it.toDomain() },
        )

    private fun ColumnDto.toDomain(): BoardColumn =
        BoardColumn(
            id = id,
            title = title,
            order = order,
            cards = cards.sortedBy { it.order }.map { it.toCard(columnId = id) },
        )

    private fun CardDto.toCard(columnId: String? = null): BoardCard =
        BoardCard(
            id = id,
            title = title,
            description = description,
            priority = priority,
            columnId = columnId ?: this.columnId ?: error("card ${id} missing columnId"),
            order = order,
            assigneeId = assigneeId,
            projectIds = projectIds,
            comments = comments.map { CardComment(id = it.id, body = it.body, userId = it.userId) },
            deadlineDueAt = deadline?.dueAt,
        )
}
