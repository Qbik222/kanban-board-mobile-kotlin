package com.kanban.mobile.feature.boards

import com.kanban.mobile.core.network.dto.BoardDetailsDto
import com.kanban.mobile.core.network.dto.BoardMemberDto
import com.kanban.mobile.core.network.dto.BoardSummaryDto
import com.kanban.mobile.core.network.dto.CardDto
import com.kanban.mobile.core.network.dto.ColumnDto

fun BoardSummaryDto.toDomain(): BoardSummary =
    BoardSummary(
        id = id,
        title = title,
        teamId = teamId,
        ownerId = ownerId,
        projectIds = projectIds,
    )

fun BoardDetailsDto.toDomain(): BoardDetails =
    BoardDetails(
        id = id,
        title = title,
        teamId = teamId,
        ownerId = ownerId,
        projectIds = projectIds,
        members = members.map { it.toMember() },
        columns = columns.sortedBy { it.order }.map { it.toBoardColumn() },
    )

fun ColumnDto.toBoardColumn(): BoardColumn =
    BoardColumn(
        id = id,
        title = title,
        order = order,
        cards = cards.sortedBy { it.order }.map { it.toBoardCard(columnId = id) },
    )

fun BoardMemberDto.toMember(): BoardMember =
    BoardMember(
        userId = userId,
        role = BoardRole.fromApi(role),
        email = email,
        name = name,
    )

fun CardDto.toBoardCard(columnId: String? = null): BoardCard =
    BoardCard(
        id = id,
        title = title,
        description = description,
        priority = priority,
        columnId = columnId ?: this.columnId ?: error("card $id missing columnId"),
        order = order,
        assigneeId = assigneeId,
        projectIds = projectIds,
        comments = comments.map { CardComment(id = it.id, body = it.body, userId = it.userId) },
        deadlineDueAt = deadline?.endDate ?: deadline?.startDate ?: deadline?.dueAt,
    )
