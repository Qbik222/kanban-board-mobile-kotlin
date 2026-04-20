package com.kanban.mobile.feature.boards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class AiBoardAction {
    CREATE_COLUMN,
    PATCH_CARD,
    MOVE_CARD,
}

data class AiPlaygroundUiState(
    val jsonText: String = "{}",
    val selectedAction: AiBoardAction = AiBoardAction.CREATE_COLUMN,
    val busy: Boolean = false,
)

sealed interface AiPlaygroundEffect {
    data class Toast(val message: String) : AiPlaygroundEffect
}

@HiltViewModel
class AiPlaygroundViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val boardRepository: BoardRepository,
) : ViewModel() {

    private val boardId: String = savedStateHandle.get<String>("boardId")!!

    private val _uiState = MutableStateFlow(AiPlaygroundUiState())
    val uiState: StateFlow<AiPlaygroundUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AiPlaygroundEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<AiPlaygroundEffect> = _effects.asSharedFlow()

    fun onJsonChange(value: String) {
        _uiState.update { it.copy(jsonText = value) }
    }

    fun onActionSelected(action: AiBoardAction) {
        _uiState.update { it.copy(selectedAction = action) }
    }

    /**
     * Parses JSON leniently; invalid JSON is a silent no-op (parity with web playground).
     */
    fun runSelectedAction() {
        val raw = _uiState.value.jsonText
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return
        when (_uiState.value.selectedAction) {
            AiBoardAction.CREATE_COLUMN -> runCreateColumn(root)
            AiBoardAction.PATCH_CARD -> runPatchCard(root)
            AiBoardAction.MOVE_CARD -> runMoveCard(root)
        }
    }

    private fun runCreateColumn(root: JSONObject) {
        val title = root.optString("title", "").trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            boardRepository.createColumn(boardId, title).fold(
                onSuccess = {
                    _uiState.update { it.copy(busy = false) }
                    _effects.tryEmit(AiPlaygroundEffect.Toast("Column created"))
                },
                onFailure = { e ->
                    _uiState.update { it.copy(busy = false) }
                    _effects.tryEmit(AiPlaygroundEffect.Toast(e.message ?: "Failed"))
                },
            )
        }
    }

    private fun runPatchCard(root: JSONObject) {
        val cardId = root.optString("cardId", "").trim()
        if (cardId.isEmpty()) return
        val title = root.optString("title", "").trim().takeIf { root.has("title") }
        val description = root.optString("description", "").takeIf { root.has("description") }
        val priority = root.optString("priority", "").takeIf { root.has("priority") }
        val deadlineObj = root.optJSONObject("deadline")
        val deadline =
            deadlineObj?.let { d ->
                CardDeadline(
                    startDate = d.optString("startDate", "").takeIf { it.isNotEmpty() },
                    endDate = d.optString("endDate", "").takeIf { it.isNotEmpty() },
                    dueAt = d.optString("dueAt", "").takeIf { it.isNotEmpty() },
                ).takeIf { dl ->
                    dl.startDate != null || dl.endDate != null || dl.dueAt != null
                }
            }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            boardRepository.patchCard(
                cardId = cardId,
                title = title,
                description = description?.takeIf { it.isNotEmpty() },
                priority = priority?.takeIf { it.isNotEmpty() },
                assigneeId = root.optString("assigneeId", "").takeIf { root.has("assigneeId") && it.isNotEmpty() },
                projectIds = root.optJSONArray("projectIds")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        arr.optString(i).takeIf { it.isNotBlank() }
                    }.takeIf { it.isNotEmpty() }
                },
                deadline = deadline,
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(busy = false) }
                    _effects.tryEmit(AiPlaygroundEffect.Toast("Card updated"))
                },
                onFailure = { e ->
                    _uiState.update { it.copy(busy = false) }
                    _effects.tryEmit(AiPlaygroundEffect.Toast(e.message ?: "Failed"))
                },
            )
        }
    }

    private fun runMoveCard(root: JSONObject) {
        val cardId = root.optString("cardId", "").trim()
        val targetColumnId = root.optString("targetColumnId", "").trim()
        if (cardId.isEmpty() || targetColumnId.isEmpty()) return
        val newOrder = root.optInt("newOrder", 0)
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            boardRepository.moveCard(cardId, targetColumnId, newOrder).fold(
                onSuccess = {
                    _uiState.update { it.copy(busy = false) }
                    _effects.tryEmit(AiPlaygroundEffect.Toast("Card moved"))
                },
                onFailure = { e ->
                    _uiState.update { it.copy(busy = false) }
                    _effects.tryEmit(AiPlaygroundEffect.Toast(e.message ?: "Failed"))
                },
            )
        }
    }
}
