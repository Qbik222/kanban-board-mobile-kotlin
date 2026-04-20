package com.kanban.mobile.feature.boards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanban.mobile.core.session.SessionRepository
import com.kanban.mobile.core.session.SessionState
import com.kanban.mobile.feature.boards.permissions.BoardPermission
import com.kanban.mobile.feature.boards.permissions.canDeleteComment as permissionCanDeleteComment
import com.kanban.mobile.feature.boards.permissions.resolveEffectiveBoardRole
import com.kanban.mobile.feature.boards.permissions.roleHasPermission
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

data class CardDraft(
    val title: String,
    val description: String,
    val priority: String,
    val assigneeId: String,
    val projectIdsText: String,
    val deadlineDueAt: String,
)

data class BoardDetailUiState(
    val loading: Boolean = true,
    val board: BoardDetails? = null,
    val effectiveRole: BoardRole = BoardRole.VIEWER,
    val error: String? = null,
    val selectedCardId: String? = null,
    val cardDraft: CardDraft? = null,
    val newCommentText: String = "",
)

@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val boardRepository: BoardRepository,
    private val sessionRepository: SessionRepository,
    private val teamsRepository: TeamsRepository,
) : ViewModel() {

    private val boardId: String = savedStateHandle.get<String>("boardId")!!
    private val initialCardId: String? = savedStateHandle.get<String>("cardId")

    private val _uiState = MutableStateFlow(BoardDetailUiState())
    val uiState: StateFlow<BoardDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val effects: SharedFlow<String> = _effects.asSharedFlow()

    init {
        load()
    }

    fun refresh() {
        load()
    }

    fun selectCard(cardId: String?) {
        val board = _uiState.value.board
        _uiState.update { st ->
            val draft = cardId?.let { id ->
                board?.findCard(id)?.toDraft()
            }
            st.copy(selectedCardId = cardId, cardDraft = draft, newCommentText = "")
        }
    }

    fun dismissCardDetail() {
        selectCard(null)
    }

    fun updateDraft(draft: CardDraft) {
        _uiState.update { it.copy(cardDraft = draft) }
    }

    fun updateNewCommentText(text: String) {
        _uiState.update { it.copy(newCommentText = text) }
    }

    fun can(permission: BoardPermission): Boolean =
        roleHasPermission(_uiState.value.effectiveRole, permission)

    fun canRemoveComment(comment: CardComment): Boolean {
        val userId = (sessionRepository.sessionState.value as? SessionState.Authenticated)?.userId
        return permissionCanDeleteComment(_uiState.value.effectiveRole, comment.userId, userId)
    }

    fun saveCardDraft() {
        val draft = _uiState.value.cardDraft ?: return
        val cardId = _uiState.value.selectedCardId ?: return
        if (!can(BoardPermission.UPDATE_CARD)) {
            _effects.tryEmit("No permission to edit cards")
            return
        }
        if (draft.title.trim().isEmpty()) {
            _effects.tryEmit("Title is required")
            return
        }
        viewModelScope.launch {
            val projectIds = draft.projectIdsText.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .takeIf { it.isNotEmpty() }
            val deadline = draft.deadlineDueAt.trim().takeIf { it.isNotEmpty() }
            val assignee = draft.assigneeId.trim().takeIf { it.isNotEmpty() }
            val priority = draft.priority.trim().takeIf { it.isNotEmpty() }
            boardRepository.patchCard(
                cardId = cardId,
                title = draft.title.trim(),
                description = draft.description.trim().takeIf { it.isNotEmpty() },
                priority = priority,
                assigneeId = assignee,
                projectIds = projectIds,
                deadlineDueAt = deadline,
            ).fold(
                onSuccess = { updated ->
                    _uiState.update { st ->
                        val b = st.board ?: return@update st
                        st.copy(board = KanbanBoardReducer.patchCard(b, updated))
                    }
                    _effects.tryEmit("Saved")
                },
                onFailure = { e ->
                    _effects.tryEmit(e.message ?: "Save failed")
                },
            )
        }
    }

    fun deleteSelectedCard() {
        val cardId = _uiState.value.selectedCardId ?: return
        if (!can(BoardPermission.DELETE_CARD)) {
            _effects.tryEmit("No permission to delete cards")
            return
        }
        viewModelScope.launch {
            val snapshot = _uiState.value.board
            _uiState.update { st ->
                val b = st.board ?: return@update st
                st.copy(board = KanbanBoardReducer.removeCard(b, cardId), selectedCardId = null, cardDraft = null)
            }
            boardRepository.deleteCard(cardId).fold(
                onSuccess = { },
                onFailure = {
                    _uiState.update { it.copy(board = snapshot) }
                    _effects.tryEmit(it.message ?: "Delete failed")
                },
            )
        }
    }

    fun createCard(columnId: String) {
        if (!can(BoardPermission.CREATE_CARD)) {
            _effects.tryEmit("No permission to create cards")
            return
        }
        viewModelScope.launch {
            boardRepository.createCard(
                title = "New card",
                description = null,
                columnId = columnId,
                assigneeId = null,
                projectIds = null,
                priority = null,
                deadlineDueAt = null,
            ).fold(
                onSuccess = { card ->
                    _uiState.update { st ->
                        val b = st.board ?: return@update st
                        st.copy(board = KanbanBoardReducer.addCard(b, card))
                    }
                },
                onFailure = { _effects.tryEmit(it.message ?: "Create failed") },
            )
        }
    }

    fun createColumn(title: String) {
        val t = title.trim()
        if (t.isEmpty()) return
        if (!can(BoardPermission.CREATE_COLUMN)) {
            _effects.tryEmit("No permission to create columns")
            return
        }
        viewModelScope.launch {
            boardRepository.createColumn(boardId, t).fold(
                onSuccess = { col ->
                    _uiState.update { st ->
                        val b = st.board ?: return@update st
                        st.copy(board = KanbanBoardReducer.upsertColumn(b, col))
                    }
                },
                onFailure = { _effects.tryEmit(it.message ?: "Column create failed") },
            )
        }
    }

    fun renameColumn(columnId: String, newTitle: String) {
        val t = newTitle.trim()
        if (t.isEmpty()) return
        if (!can(BoardPermission.UPDATE_COLUMN)) {
            _effects.tryEmit("No permission to edit columns")
            return
        }
        viewModelScope.launch {
            val prev = _uiState.value.board
            _uiState.update { st ->
                val b = st.board ?: return@update st
                st.copy(board = KanbanBoardReducer.renameColumn(b, columnId, t))
            }
            boardRepository.renameColumn(columnId, t).onFailure { e ->
                _uiState.update { it.copy(board = prev) }
                _effects.tryEmit(e.message ?: "Rename failed")
            }
        }
    }

    fun deleteColumn(columnId: String) {
        if (!can(BoardPermission.DELETE_COLUMN)) {
            _effects.tryEmit("No permission to delete columns")
            return
        }
        viewModelScope.launch {
            val prev = _uiState.value.board
            _uiState.update { st ->
                val b = st.board ?: return@update st
                st.copy(board = KanbanBoardReducer.removeColumn(b, columnId))
            }
            boardRepository.deleteColumn(columnId).onFailure { e ->
                _uiState.update { it.copy(board = prev) }
                _effects.tryEmit(e.message ?: "Delete column failed")
            }
        }
    }

    fun moveColumn(columnId: String, delta: Int) {
        if (!can(BoardPermission.REORDER_COLUMNS)) {
            _effects.tryEmit("No permission to reorder columns")
            return
        }
        val board = _uiState.value.board ?: return
        val sorted = board.columns.sortedBy { it.order }
        val index = sorted.indexOfFirst { it.id == columnId }
        if (index < 0) return
        val newIndex = (index + delta).coerceIn(0, sorted.lastIndex)
        if (newIndex == index) return
        val reordered = sorted.toMutableList().apply {
            val item = removeAt(index)
            add(newIndex, item)
        }
        val pairs = reordered.mapIndexed { i, col -> col.id to i }
        val snapshot = board
        viewModelScope.launch {
            _uiState.update { st ->
                val b = st.board ?: return@update st
                st.copy(board = KanbanBoardReducer.reorderColumns(b, reordered.map { it.id }))
            }
            boardRepository.reorderColumns(pairs).onFailure { e ->
                _uiState.update { it.copy(board = snapshot) }
                _effects.tryEmit(e.message ?: "Reorder failed")
            }
        }
    }

    fun moveCardWithinColumn(cardId: String, delta: Int) {
        if (!can(BoardPermission.MOVE_CARD)) {
            _effects.tryEmit("No permission to move cards")
            return
        }
        val board = _uiState.value.board ?: return
        val column = board.columns.firstOrNull { col -> col.cards.any { it.id == cardId } } ?: return
        val oldIndex = column.cards.indexOfFirst { it.id == cardId }
        val targetIndex = (oldIndex + delta).coerceIn(0, column.cards.lastIndex)
        if (oldIndex == targetIndex) return
        val newOrder = sameColumnInsertionIndex(oldIndex, targetIndex)
        applyMove(cardId, column.id, newOrder)
    }

    fun moveCardToColumn(cardId: String, targetColumnId: String) {
        if (!can(BoardPermission.MOVE_CARD)) {
            _effects.tryEmit("No permission to move cards")
            return
        }
        val board = _uiState.value.board ?: return
        val sourceCol = board.columns.firstOrNull { col -> col.cards.any { it.id == cardId } } ?: return
        val targetCol = board.columns.firstOrNull { it.id == targetColumnId } ?: return
        if (sourceCol.id == targetColumnId) return
        val newOrder = if (sourceCol.id == targetColumnId) {
            sourceCol.cards.indexOfFirst { it.id == cardId }
        } else {
            targetCol.cards.count { it.id != cardId }
        }
        applyMove(cardId, targetColumnId, newOrder.coerceAtLeast(0))
    }

    private fun sameColumnInsertionIndex(oldIndex: Int, targetIndex: Int): Int =
        if (targetIndex > oldIndex) targetIndex - 1 else targetIndex

    private fun applyMove(cardId: String, targetColumnId: String, newOrder: Int) {
        val snapshot = _uiState.value.board ?: return
        val optimistic = KanbanBoardReducer.applyCardMove(snapshot, cardId, targetColumnId, newOrder)
        _uiState.update { it.copy(board = optimistic) }
        viewModelScope.launch {
            boardRepository.moveCard(cardId, targetColumnId, newOrder).fold(
                onSuccess = { merged ->
                    _uiState.update { st ->
                        val b = st.board ?: return@update st
                        st.copy(board = KanbanBoardReducer.mergeCard(b, merged))
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(board = snapshot) }
                    _effects.tryEmit(e.message ?: "Move failed")
                },
            )
        }
    }

    fun submitComment() {
        val cardId = _uiState.value.selectedCardId ?: return
        val text = _uiState.value.newCommentText.trim()
        if (text.isEmpty()) return
        if (!can(BoardPermission.COMMENT_CREATE)) {
            _effects.tryEmit("No permission to comment")
            return
        }
        viewModelScope.launch {
            boardRepository.addComment(cardId, text).fold(
                onSuccess = { comment ->
                    _uiState.update { st ->
                        val b = st.board ?: return@update st
                        st.copy(
                            board = KanbanBoardReducer.addComment(b, cardId, comment),
                            newCommentText = "",
                        )
                    }
                },
                onFailure = { _effects.tryEmit(it.message ?: "Comment failed") },
            )
        }
    }

    fun deleteComment(commentId: String) {
        val cardId = _uiState.value.selectedCardId ?: return
        viewModelScope.launch {
            boardRepository.deleteComment(cardId, commentId).fold(
                onSuccess = {
                    _uiState.update { st ->
                        val b = st.board ?: return@update st
                        st.copy(board = KanbanBoardReducer.removeComment(b, cardId, commentId))
                    }
                },
                onFailure = { _effects.tryEmit(it.message ?: "Delete comment failed") },
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val session = sessionRepository.sessionState.first()
            val userId = (session as? SessionState.Authenticated)?.userId
            val boardResult = boardRepository.getBoard(boardId)
            boardResult.fold(
                onSuccess = { board ->
                    val teamAdmin = userId?.let { uid ->
                        teamsRepository.listMembers(board.teamId).getOrNull()
                            ?.any { it.userId == uid && it.role == TeamMemberRole.ADMIN }
                    } == true
                    val role = resolveEffectiveBoardRole(board, userId, teamAdmin)
                    _uiState.update {
                        it.copy(
                            loading = false,
                            board = board,
                            effectiveRole = role,
                            error = null,
                        )
                    }
                    if (initialCardId != null) {
                        selectCard(initialCardId)
                    }
                },
                onFailure = { e ->
                    val message = e.message ?: "Failed to load board"
                    _uiState.update {
                        it.copy(loading = false, board = null, error = message)
                    }
                    _effects.tryEmit(message)
                },
            )
        }
    }

    private fun BoardDetails.findCard(cardId: String): BoardCard? =
        columns.asSequence().flatMap { it.cards.asSequence() }.firstOrNull { it.id == cardId }

    private fun BoardCard.toDraft(): CardDraft =
        CardDraft(
            title = title,
            description = description.orEmpty(),
            priority = priority.orEmpty(),
            assigneeId = assigneeId.orEmpty(),
            projectIdsText = projectIds.joinToString(","),
            deadlineDueAt = deadlineDueAt.orEmpty(),
        )
}
