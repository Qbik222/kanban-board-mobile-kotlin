package com.kanban.mobile.feature.boards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanban.mobile.core.network.toUserMessage
import com.kanban.mobile.core.session.SessionRepository
import com.kanban.mobile.core.session.SessionState
import com.kanban.mobile.feature.boards.permissions.BoardAdminPermission
import com.kanban.mobile.feature.boards.permissions.resolveEffectiveBoardRole
import com.kanban.mobile.feature.boards.permissions.roleHasAdminPermission
import com.kanban.mobile.feature.teams.TeamMemberRole
import com.kanban.mobile.feature.teams.TeamsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardSettingsUiState(
    val loading: Boolean = true,
    val titleDraft: String = "",
    val projectIds: List<String> = emptyList(),
    val newProjectIdInput: String = "",
    val inviteUserIdDraft: String = "",
    val members: List<BoardMember> = emptyList(),
    val ownerId: String = "",
    val effectiveRole: BoardRole = BoardRole.VIEWER,
    val error: String? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val memberActionInProgressUserId: String? = null,
)

sealed interface BoardSettingsUiEffect {
    data class Snackbar(val message: String) : BoardSettingsUiEffect
    data object NavigateToBoardsAfterDelete : BoardSettingsUiEffect
}

@HiltViewModel
class BoardSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val boardRepository: BoardRepository,
    private val sessionRepository: SessionRepository,
    private val teamsRepository: TeamsRepository,
) : ViewModel() {

    private val boardId: String = savedStateHandle.get<String>("boardId")!!

    private val _uiState = MutableStateFlow(BoardSettingsUiState())
    val uiState: StateFlow<BoardSettingsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<BoardSettingsUiEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<BoardSettingsUiEffect> = _effects.asSharedFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val session = sessionRepository.sessionState.first()
            val userId = (session as? SessionState.Authenticated)?.userId
            boardRepository.getBoard(boardId).fold(
                onSuccess = { board ->
                    val teamAdmin = userId?.let { uid ->
                        teamsRepository.listMembers(board.teamId).getOrNull()
                            ?.any { it.userId == uid && it.role == TeamMemberRole.ADMIN }
                    } == true
                    val role = resolveEffectiveBoardRole(board, userId, teamAdmin)
                    _uiState.update {
                        it.copy(
                            loading = false,
                            titleDraft = board.title,
                            projectIds = board.projectIds,
                            members = board.members,
                            ownerId = board.ownerId,
                            effectiveRole = role,
                            error = null,
                        )
                    }
                },
                onFailure = { e ->
                    val msg = e.toUserMessage()
                    _uiState.update {
                        it.copy(loading = false, error = msg)
                    }
                    _effects.tryEmit(BoardSettingsUiEffect.Snackbar(msg))
                },
            )
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(titleDraft = value) }
    }

    fun onNewProjectIdChange(value: String) {
        _uiState.update { it.copy(newProjectIdInput = value) }
    }

    fun addProjectIdChip() {
        val trimmed = _uiState.value.newProjectIdInput.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { st ->
            if (st.projectIds.contains(trimmed)) {
                st.copy(newProjectIdInput = "")
            } else {
                st.copy(
                    projectIds = st.projectIds + trimmed,
                    newProjectIdInput = "",
                )
            }
        }
    }

    fun removeProjectId(id: String) {
        _uiState.update { st ->
            st.copy(projectIds = st.projectIds.filter { it != id })
        }
    }

    fun onInviteUserIdChange(value: String) {
        _uiState.update { it.copy(inviteUserIdDraft = value) }
    }

    fun saveBoard() {
        if (!can(BoardAdminPermission.BOARD_UPDATE)) {
            _effects.tryEmit(BoardSettingsUiEffect.Snackbar("Немає прав"))
            return
        }
        val title = _uiState.value.titleDraft.trim()
        if (title.isEmpty()) {
            _effects.tryEmit(BoardSettingsUiEffect.Snackbar("Введіть назву дошки"))
            return
        }
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            boardRepository.updateBoard(boardId, title, _uiState.value.projectIds).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false) }
                    _effects.tryEmit(BoardSettingsUiEffect.Snackbar("Збережено"))
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false) }
                    _effects.tryEmit(BoardSettingsUiEffect.Snackbar(e.toUserMessage()))
                },
            )
        }
    }

    fun inviteMember() {
        if (!can(BoardAdminPermission.MEMBER_INVITE)) {
            _effects.tryEmit(BoardSettingsUiEffect.Snackbar("Немає прав"))
            return
        }
        val uid = _uiState.value.inviteUserIdDraft.trim()
        if (uid.isEmpty()) {
            _effects.tryEmit(BoardSettingsUiEffect.Snackbar("Введіть user id"))
            return
        }
        viewModelScope.launch {
            boardRepository.inviteBoardMember(boardId, uid).fold(
                onSuccess = {
                    _uiState.update { it.copy(inviteUserIdDraft = "") }
                    refreshMembersOnly()
                    _effects.tryEmit(BoardSettingsUiEffect.Snackbar("Запрошено"))
                },
                onFailure = { e ->
                    _effects.tryEmit(BoardSettingsUiEffect.Snackbar(e.toUserMessage()))
                },
            )
        }
    }

    fun updateMemberRole(memberUserId: String, newRole: BoardRole) {
        if (!can(BoardAdminPermission.MEMBER_UPDATE_ROLE)) {
            _effects.tryEmit(BoardSettingsUiEffect.Snackbar("Немає прав"))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(memberActionInProgressUserId = memberUserId) }
            boardRepository.updateBoardMemberRole(boardId, memberUserId, newRole).fold(
                onSuccess = {
                    refreshMembersOnly()
                    _effects.tryEmit(BoardSettingsUiEffect.Snackbar("Роль оновлено"))
                },
                onFailure = { e ->
                    _effects.tryEmit(BoardSettingsUiEffect.Snackbar(e.toUserMessage()))
                },
            )
            _uiState.update { it.copy(memberActionInProgressUserId = null) }
        }
    }

    fun removeMember(memberUserId: String) {
        if (!can(BoardAdminPermission.MEMBER_REMOVE)) {
            _effects.tryEmit(BoardSettingsUiEffect.Snackbar("Немає прав"))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(memberActionInProgressUserId = memberUserId) }
            boardRepository.removeBoardMember(boardId, memberUserId).fold(
                onSuccess = {
                    refreshMembersOnly()
                    _effects.tryEmit(BoardSettingsUiEffect.Snackbar("Учасника видалено"))
                },
                onFailure = { e ->
                    _effects.tryEmit(BoardSettingsUiEffect.Snackbar(e.toUserMessage()))
                },
            )
            _uiState.update { it.copy(memberActionInProgressUserId = null) }
        }
    }

    fun deleteBoard() {
        if (!can(BoardAdminPermission.BOARD_DELETE)) {
            _effects.tryEmit(BoardSettingsUiEffect.Snackbar("Немає прав"))
            return
        }
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            boardRepository.deleteBoard(boardId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isDeleting = false) }
                    _effects.tryEmit(BoardSettingsUiEffect.NavigateToBoardsAfterDelete)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isDeleting = false) }
                    _effects.tryEmit(BoardSettingsUiEffect.Snackbar(e.toUserMessage()))
                },
            )
        }
    }

    fun can(permission: BoardAdminPermission): Boolean =
        roleHasAdminPermission(_uiState.value.effectiveRole, permission)

    fun currentUserId(): String? =
        (sessionRepository.sessionState.value as? SessionState.Authenticated)?.userId

    fun canEditMember(member: BoardMember): Boolean {
        val self = currentUserId()
        if (self != null && member.userId == self) return false
        return can(BoardAdminPermission.MEMBER_UPDATE_ROLE)
    }

    fun canRemoveMember(member: BoardMember): Boolean {
        val self = currentUserId()
        if (self != null && member.userId == self) return false
        if (member.userId == _uiState.value.ownerId) return false
        return can(BoardAdminPermission.MEMBER_REMOVE)
    }

    private suspend fun refreshMembersOnly() {
        boardRepository.listBoardMembers(boardId).fold(
            onSuccess = { members ->
                _uiState.update { it.copy(members = members) }
            },
            onFailure = { },
        )
    }
}
