package com.kanban.mobile.data

import com.kanban.mobile.core.network.BoardApi
import com.kanban.mobile.core.network.dto.BoardDetailsDto
import com.kanban.mobile.core.network.dto.BoardSummaryDto
import com.kanban.mobile.core.network.dto.CardDto
import com.kanban.mobile.core.network.dto.ColumnDto
import com.kanban.mobile.core.network.dto.CreateBoardRequestDto
import com.kanban.mobile.feature.boards.BoardCard
import com.kanban.mobile.feature.boards.BoardColumn
import com.kanban.mobile.feature.boards.BoardDetails
import com.kanban.mobile.feature.boards.BoardRepository
import com.kanban.mobile.feature.boards.BoardSummary
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
            boardApi.getBoard(id).toDomain().also { detailsCache[id] = it }
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
            columns = columns.map { it.toDomain() },
        )

    private fun ColumnDto.toDomain(): BoardColumn =
        BoardColumn(
            id = id,
            title = title,
            cards = cards.map { it.toDomain() },
        )

    private fun CardDto.toDomain(): BoardCard =
        BoardCard(
            id = id,
            title = title,
            description = description,
            priority = priority,
            commentCount = comments.size,
            deadlineDueAt = deadline?.dueAt,
        )
}
