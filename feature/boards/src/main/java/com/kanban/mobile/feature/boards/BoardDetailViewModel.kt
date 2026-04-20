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

data class BoardDetailUiState(
    val loading: Boolean = true,
    val board: BoardDetails? = null,
    val error: String? = null,
)

@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val boardRepository: BoardRepository,
) : ViewModel() {

    private val boardId: String = savedStateHandle.get<String>("boardId")!!

    private val _uiState = MutableStateFlow(BoardDetailUiState())
    val uiState: StateFlow<BoardDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val effects: SharedFlow<String> = _effects.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            boardRepository.getBoard(boardId).fold(
                onSuccess = { board ->
                    _uiState.update {
                        it.copy(loading = false, board = board, error = null)
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

    fun refresh() {
        load()
    }
}
