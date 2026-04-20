package com.kanban.mobile.feature.boards

data class BoardSummary(
    val id: String,
    val title: String,
    val teamId: String,
    val ownerId: String,
    val projectIds: List<String>,
)

data class BoardDetails(
    val id: String,
    val title: String,
    val teamId: String,
    val ownerId: String,
    val projectIds: List<String>,
    val columns: List<BoardColumn>,
)

data class BoardColumn(
    val id: String,
    val title: String,
    val cards: List<BoardCard>,
)

data class BoardCard(
    val id: String,
    val title: String,
    val description: String?,
    val priority: String?,
    val commentCount: Int,
    val deadlineDueAt: String?,
)
