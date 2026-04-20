@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.kanban.mobile.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * REST shapes aligned with the Kanban backend contract (stage 5).
 * [CardCommentDto.body] accepts JSON keys `text` or `body` via [JsonNames].
 */
@Serializable
data class BoardSummaryDto(
    val id: String,
    val title: String,
    val teamId: String,
    val ownerId: String,
    val projectIds: List<String> = emptyList(),
)

@Serializable
data class CreateBoardRequestDto(
    val title: String,
    val teamId: String,
    val projectIds: List<String>? = null,
)

@Serializable
data class BoardMemberDto(
    val userId: String,
    val role: String,
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
)

@Serializable
data class PatchBoardRequestDto(
    val title: String? = null,
    val projectIds: List<String>? = null,
)

@Serializable
data class InviteBoardMemberRequestDto(
    val userId: String,
)

@Serializable
data class PatchBoardMemberRoleRequestDto(
    val role: String,
)

@Serializable
data class BoardDetailsDto(
    val id: String,
    val title: String,
    val teamId: String,
    val ownerId: String,
    val projectIds: List<String> = emptyList(),
    val columns: List<ColumnDto> = emptyList(),
    val members: List<BoardMemberDto> = emptyList(),
)

@Serializable
data class ColumnDto(
    val id: String,
    val title: String,
    val order: Int = 0,
    val cards: List<CardDto> = emptyList(),
)

@Serializable
data class CardDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val priority: String? = null,
    val columnId: String? = null,
    val order: Int = 0,
    val assigneeId: String? = null,
    val projectIds: List<String> = emptyList(),
    val comments: List<CardCommentDto> = emptyList(),
    val deadline: CardDeadlineDto? = null,
)

@Serializable
data class CardCommentDto(
    val id: String,
    @JsonNames("text", "body")
    val body: String? = null,
    val userId: String? = null,
)

@Serializable
data class CardDeadlineDto(
    val dueAt: String? = null,
)

@Serializable
data class CreateColumnRequestDto(
    val title: String,
    val boardId: String,
)

@Serializable
data class PatchColumnRequestDto(
    val title: String? = null,
)

@Serializable
data class ColumnOrderEntryDto(
    val id: String,
    val order: Int,
)

@Serializable
data class ReorderColumnsRequestDto(
    val columns: List<ColumnOrderEntryDto>,
)

@Serializable
data class CreateCardRequestDto(
    val title: String,
    val description: String? = null,
    val columnId: String,
    val assigneeId: String? = null,
    val projectIds: List<String>? = null,
    val priority: String? = null,
    val deadline: CardDeadlineDto? = null,
)

@Serializable
data class PatchCardRequestDto(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val assigneeId: String? = null,
    val projectIds: List<String>? = null,
    val deadline: CardDeadlineDto? = null,
)

@Serializable
data class MoveCardRequestDto(
    val targetColumnId: String,
    val newOrder: Int,
)

@Serializable
data class CreateCardCommentRequestDto(
    val text: String,
)
