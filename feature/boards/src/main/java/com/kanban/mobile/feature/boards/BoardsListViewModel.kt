package com.kanban.mobile.feature.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanban.mobile.feature.teams.Team
import com.kanban.mobile.feature.teams.TeamsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardsListUiState(
    val loading: Boolean = false,
    val teamsLoading: Boolean = true,
    val teams: List<Team> = emptyList(),
    val boards: List<BoardSummary> = emptyList(),
    /** `null` means show boards from all teams (in-memory filter). */
    val filterTeamId: String? = null,
    val error: String? = null,
) {
    val visibleBoards: List<BoardSummary>
        get() =
            if (filterTeamId.isNullOrBlank()) {
                boards
            } else {
                boards.filter { it.teamId == filterTeamId }
            }
}

@HiltViewModel
class BoardsListViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val teamsRepository: TeamsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardsListUiState())
    val uiState: StateFlow<BoardsListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setFilterTeamId(teamId: String?) {
        _uiState.update { it.copy(filterTeamId = teamId) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = runCatching {
                coroutineScope {
                    val boardsD = async { boardRepository.listBoards() }
                    val teamsD = async { teamsRepository.listTeams() }
                    Pair(boardsD.await(), teamsD.await())
                }
            }
            result.fold(
                onSuccess = { (boardsRes, teamsRes) ->
                    val teams = teamsRes.getOrElse { emptyList() }
                    val teamsErr = teamsRes.exceptionOrNull()?.message
                    boardsRes.fold(
                        onSuccess = { list ->
                            _uiState.update {
                                it.copy(
                                    loading = false,
                                    teamsLoading = false,
                                    teams = teams,
                                    boards = list,
                                    error = teamsErr?.takeIf { list.isEmpty() },
                                )
                            }
                        },
                        onFailure = { e ->
                            _uiState.update {
                                it.copy(
                                    loading = false,
                                    teamsLoading = false,
                                    teams = teams,
                                    error = e.message ?: "Failed to load boards",
                                )
                            }
                        },
                    )
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            teamsLoading = false,
                            error = e.message ?: "Failed to load boards",
                        )
                    }
                },
            )
        }
    }
}
