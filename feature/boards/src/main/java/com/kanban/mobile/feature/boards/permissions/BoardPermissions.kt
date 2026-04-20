package com.kanban.mobile.feature.boards.permissions

import com.kanban.mobile.feature.boards.BoardDetails
import com.kanban.mobile.feature.boards.BoardRole

enum class BoardPermission {
    VIEW_BOARD,
    CREATE_COLUMN,
    UPDATE_COLUMN,
    DELETE_COLUMN,
    REORDER_COLUMNS,
    CREATE_CARD,
    UPDATE_CARD,
    DELETE_CARD,
    MOVE_CARD,
    COMMENT_CREATE,
    COMMENT_DELETE_OWN,
    COMMENT_DELETE_ANY,
}

private val OWNER_OR_EDITOR: Set<BoardPermission> = setOf(
    BoardPermission.VIEW_BOARD,
    BoardPermission.CREATE_COLUMN,
    BoardPermission.UPDATE_COLUMN,
    BoardPermission.DELETE_COLUMN,
    BoardPermission.REORDER_COLUMNS,
    BoardPermission.CREATE_CARD,
    BoardPermission.UPDATE_CARD,
    BoardPermission.DELETE_CARD,
    BoardPermission.MOVE_CARD,
    BoardPermission.COMMENT_CREATE,
    BoardPermission.COMMENT_DELETE_OWN,
    BoardPermission.COMMENT_DELETE_ANY,
)

private val VIEWER: Set<BoardPermission> = setOf(BoardPermission.VIEW_BOARD)

private val ROLE_PERMISSIONS: Map<BoardRole, Set<BoardPermission>> = mapOf(
    BoardRole.OWNER to OWNER_OR_EDITOR,
    BoardRole.EDITOR to OWNER_OR_EDITOR,
    BoardRole.VIEWER to VIEWER,
)

fun roleHasPermission(role: BoardRole, permission: BoardPermission): Boolean =
    ROLE_PERMISSIONS[role]?.contains(permission) == true

fun resolveEffectiveBoardRole(
    board: BoardDetails,
    currentUserId: String?,
    isTeamAdmin: Boolean,
): BoardRole {
    if (currentUserId == null) return BoardRole.VIEWER
    if (currentUserId == board.ownerId) return BoardRole.OWNER
    board.members.firstOrNull { it.userId == currentUserId }?.role?.let {
        return it
    }
    if (isTeamAdmin) return BoardRole.OWNER
    return BoardRole.VIEWER
}

fun canDeleteComment(
    role: BoardRole,
    commentAuthorId: String?,
    currentUserId: String?,
): Boolean {
    if (roleHasPermission(role, BoardPermission.COMMENT_DELETE_ANY)) return true
    if (!roleHasPermission(role, BoardPermission.COMMENT_DELETE_OWN)) return false
    return commentAuthorId != null &&
        currentUserId != null &&
        commentAuthorId == currentUserId
}
