package com.kanban.mobile.feature.boards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanban.mobile.feature.teams.Team
import com.kanban.mobile.feature.teams.TeamsRepository
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

data class CreateBoardUiState(
    val title: String = "",
    val projectIdsText: String = "",
    val teams: List<Team> = emptyList(),
    val selectedTeamId: String = "",
    val loadingTeams: Boolean = true,
    val loadingSubmit: Boolean = false,
    val teamsError: String? = null,
)

@HiltViewModel
class CreateBoardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val boardRepository: BoardRepository,
    private val teamsRepository: TeamsRepository,
) : ViewModel() {

    private val initialTeamId: String = savedStateHandle.get<String>("teamId").orEmpty()

    private val _uiState = MutableStateFlow(CreateBoardUiState())
    val uiState: StateFlow<CreateBoardUiState> = _uiState.asStateFlow()

    private val _navigateToBoardId = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val navigateToBoardId: SharedFlow<String> = _navigateToBoardId.asSharedFlow()

    private val _effects = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val effects: SharedFlow<String> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            teamsRepository.listTeams().fold(
                onSuccess = { teams ->
                    val selected = when {
                        initialTeamId.isNotBlank() && teams.any { it.id == initialTeamId } ->
                            initialTeamId
                        else -> teams.firstOrNull()?.id.orEmpty()
                    }
                    _uiState.update {
                        it.copy(
                            loadingTeams = false,
                            teams = teams,
                            selectedTeamId = selected,
                            teamsError = null,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            loadingTeams = false,
                            teamsError = e.message ?: "Failed to load teams",
                        )
                    }
                },
            )
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onProjectIdsTextChange(value: String) {
        _uiState.update { it.copy(projectIdsText = value) }
    }

    fun onTeamSelected(teamId: String) {
        _uiState.update { it.copy(selectedTeamId = teamId) }
    }

    fun submit() {
        val title = _uiState.value.title.trim()
        if (title.isBlank()) {
            _effects.tryEmit("Enter a board title")
            return
        }
        val teamId = _uiState.value.selectedTeamId
        if (teamId.isBlank()) {
            _effects.tryEmit("Select a team")
            return
        }
        val projectIds = _uiState.value.projectIdsText
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() }
        viewModelScope.launch {
            _uiState.update { it.copy(loadingSubmit = true) }
            boardRepository.createBoard(title, teamId, projectIds).fold(
                onSuccess = { summary ->
                    _uiState.update { it.copy(loadingSubmit = false) }
                    _navigateToBoardId.tryEmit(summary.id)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(loadingSubmit = false) }
                    _effects.tryEmit(e.message ?: "Could not create board")
                },
            )
        }
    }
}
