package com.kanban.mobile.feature.boards

import com.kanban.mobile.feature.boards.permissions.BoardAdminPermission
import com.kanban.mobile.feature.boards.permissions.BoardPermission
import com.kanban.mobile.feature.boards.permissions.resolveEffectiveBoardRole
import com.kanban.mobile.feature.boards.permissions.roleHasAdminPermission
import com.kanban.mobile.feature.boards.permissions.roleHasPermission
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardPermissionsTest {

    private fun board(ownerId: String = "u1"): BoardDetails =
        BoardDetails(
            id = "b",
            title = "B",
            teamId = "t",
            ownerId = ownerId,
            projectIds = emptyList(),
            members = listOf(BoardMember("u2", BoardRole.EDITOR)),
            columns = emptyList(),
        )

    @Test
    fun owner_can_move_cards() {
        val role = resolveEffectiveBoardRole(board(ownerId = "me"), "me", isTeamAdmin = false)
        assertTrue(roleHasPermission(role, BoardPermission.MOVE_CARD))
    }

    @Test
    fun viewer_read_only() {
        val role = resolveEffectiveBoardRole(board(), currentUserId = "stranger", isTeamAdmin = false)
        assertTrue(roleHasPermission(role, BoardPermission.VIEW_BOARD))
        assertFalse(roleHasPermission(role, BoardPermission.MOVE_CARD))
    }

    @Test
    fun team_admin_elevated_to_owner_permissions() {
        val b = board(ownerId = "other")
        val role = resolveEffectiveBoardRole(b, currentUserId = "me", isTeamAdmin = true)
        assertTrue(roleHasPermission(role, BoardPermission.DELETE_COLUMN))
    }

    @Test
    fun owner_has_board_delete_admin_permission() {
        assertTrue(roleHasAdminPermission(BoardRole.OWNER, BoardAdminPermission.BOARD_DELETE))
    }

    @Test
    fun editor_cannot_delete_board_via_admin_matrix() {
        assertFalse(roleHasAdminPermission(BoardRole.EDITOR, BoardAdminPermission.BOARD_DELETE))
    }

    @Test
    fun editor_can_update_board_and_manage_members() {
        assertTrue(roleHasAdminPermission(BoardRole.EDITOR, BoardAdminPermission.BOARD_UPDATE))
        assertTrue(roleHasAdminPermission(BoardRole.EDITOR, BoardAdminPermission.MEMBER_INVITE))
        assertTrue(roleHasAdminPermission(BoardRole.EDITOR, BoardAdminPermission.MEMBER_UPDATE_ROLE))
        assertTrue(roleHasAdminPermission(BoardRole.EDITOR, BoardAdminPermission.MEMBER_REMOVE))
    }

    @Test
    fun viewer_has_no_admin_permissions() {
        BoardAdminPermission.entries.forEach { p ->
            assertFalse(roleHasAdminPermission(BoardRole.VIEWER, p))
        }
    }
}
