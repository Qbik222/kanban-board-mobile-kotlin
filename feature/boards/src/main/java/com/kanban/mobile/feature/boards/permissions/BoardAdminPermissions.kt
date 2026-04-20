package com.kanban.mobile.feature.boards.permissions

import com.kanban.mobile.feature.boards.BoardRole

enum class BoardAdminPermission {
    BOARD_UPDATE,
    BOARD_DELETE,
    MEMBER_INVITE,
    MEMBER_UPDATE_ROLE,
    MEMBER_REMOVE,
}

private val OWNER_ALL: Set<BoardAdminPermission> =
    BoardAdminPermission.entries.toSet()

private val EDITOR_MANAGE_MEMBERS_AND_META: Set<BoardAdminPermission> = setOf(
    BoardAdminPermission.BOARD_UPDATE,
    BoardAdminPermission.MEMBER_INVITE,
    BoardAdminPermission.MEMBER_UPDATE_ROLE,
    BoardAdminPermission.MEMBER_REMOVE,
)

private val ROLE_ADMIN_PERMISSIONS: Map<BoardRole, Set<BoardAdminPermission>> = mapOf(
    BoardRole.OWNER to OWNER_ALL,
    BoardRole.EDITOR to EDITOR_MANAGE_MEMBERS_AND_META,
    BoardRole.VIEWER to emptySet(),
)

fun roleHasAdminPermission(role: BoardRole, permission: BoardAdminPermission): Boolean =
    ROLE_ADMIN_PERMISSIONS[role]?.contains(permission) == true
