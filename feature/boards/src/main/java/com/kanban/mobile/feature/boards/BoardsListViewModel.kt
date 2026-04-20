package com.kanban.mobile.feature.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardsListUiState(
    val loading: Boolean = false,
    val boards: List<BoardSummary> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class BoardsListViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardsListUiState())
    val uiState: StateFlow<BoardsListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            boardRepository.listBoards().fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(loading = false, boards = list, error = null)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: "Failed to load boards",
                        )
                    }
                },
            )
        }
    }
}
