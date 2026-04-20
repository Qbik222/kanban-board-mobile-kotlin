package com.kanban.mobile.feature.teams

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

data class TeamsListUiState(
    val loading: Boolean = false,
    val teams: List<Team> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TeamsListViewModel @Inject constructor(
    private val teamsRepository: TeamsRepository,
) : ViewModel() {

    companion object {
        const val TEAM_CREATED_EFFECT = "Team created"
    }

    private val _uiState = MutableStateFlow(TeamsListUiState())
    val uiState: StateFlow<TeamsListUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val effects: SharedFlow<String> = _effects.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            teamsRepository.listTeams().fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(loading = false, teams = list, error = null)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(loading = false, error = e.message ?: "Failed to load teams")
                    }
                },
            )
        }
    }

    fun createTeam(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _effects.tryEmit("Enter a team name")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            teamsRepository.createTeam(trimmed).fold(
                onSuccess = {
                    _effects.tryEmit(TEAM_CREATED_EFFECT)
                    teamsRepository.listTeams().fold(
                        onSuccess = { list ->
                            _uiState.update {
                                it.copy(loading = false, teams = list, error = null)
                            }
                        },
                        onFailure = { e ->
                            _uiState.update {
                                it.copy(
                                    loading = false,
                                    error = e.message ?: "Failed to refresh list",
                                )
                            }
                        },
                    )
                },
                onFailure = { e ->
                    _uiState.update { it.copy(loading = false) }
                    _effects.tryEmit(e.message ?: "Could not create team")
                },
            )
        }
    }
}
