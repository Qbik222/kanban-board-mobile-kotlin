package com.kanban.mobile.core.network.dto

import kotlinx.serialization.Serializable

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
data class BoardDetailsDto(
    val id: String,
    val title: String,
    val teamId: String,
    val ownerId: String,
    val projectIds: List<String> = emptyList(),
    val columns: List<ColumnDto> = emptyList(),
)

@Serializable
data class ColumnDto(
    val id: String,
    val title: String,
    val cards: List<CardDto> = emptyList(),
)

@Serializable
data class CardDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val priority: String? = null,
    val comments: List<CardCommentDto> = emptyList(),
    val deadline: CardDeadlineDto? = null,
)

@Serializable
data class CardCommentDto(
    val id: String,
    val body: String? = null,
)

@Serializable
data class CardDeadlineDto(
    val dueAt: String? = null,
)
