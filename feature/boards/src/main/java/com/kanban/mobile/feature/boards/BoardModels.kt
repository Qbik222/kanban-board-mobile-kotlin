package com.kanban.mobile.feature.boards

data class BoardSummary(
    val id: String,
    val title: String,
    val teamId: String,
    val ownerId: String,
    val projectIds: List<String>,
)

enum class BoardRole {
    OWNER,
    EDITOR,
    VIEWER,
    ;

    companion object {
        fun fromApi(value: String): BoardRole =
            when (value.uppercase()) {
                "OWNER" -> OWNER
                "EDITOR" -> EDITOR
                else -> VIEWER
            }
    }

    fun toApi(): String = name
}

data class BoardMember(
    val userId: String,
    val role: BoardRole,
    val email: String? = null,
    val name: String? = null,
)

data class BoardDetails(
    val id: String,
    val title: String,
    val teamId: String,
    val ownerId: String,
    val projectIds: List<String>,
    val members: List<BoardMember>,
    val columns: List<BoardColumn>,
)

data class BoardColumn(
    val id: String,
    val title: String,
    val order: Int,
    val cards: List<BoardCard>,
)

data class CardComment(
    val id: String,
    val body: String?,
    val userId: String?,
)

data class BoardCard(
    val id: String,
    val title: String,
    val description: String?,
    val priority: String?,
    val columnId: String,
    val order: Int,
    val assigneeId: String?,
    val projectIds: List<String>,
    val comments: List<CardComment>,
    val deadlineDueAt: String?,
) {
    val commentCount: Int get() = comments.size
}
