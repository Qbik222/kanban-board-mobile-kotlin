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

    /** Backend expects lowercase: `owner`, `editor`, `viewer`. */
    fun toApi(): String = name.lowercase()
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

/** Aligns with [com.kanban.mobile.core.network.dto.CardDeadlineDto] (ISO date strings). */
data class CardDeadline(
    val startDate: String? = null,
    val endDate: String? = null,
    val dueAt: String? = null,
)

/** Fixed set for dropdown parity with the web client (extend if backend adds values). */
object CardPriorityOptions {
    val LABELS = listOf(
        "" to "(none)",
        "LOW" to "Low",
        "MEDIUM" to "Medium",
        "HIGH" to "High",
        "URGENT" to "Urgent",
    )
}

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
    val deadline: CardDeadline?,
) {
    val commentCount: Int get() = comments.size
}
