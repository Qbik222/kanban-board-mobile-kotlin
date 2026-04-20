package com.kanban.mobile.feature.teams

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanban.mobile.core.session.SessionRepository
import com.kanban.mobile.core.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeamDetailUiState(
    val loading: Boolean = true,
    val team: Team? = null,
    val members: List<TeamMember> = emptyList(),
    val screenError: String? = null,
    val inviteSearchQuery: String = "",
    val inviteCandidates: List<InviteCandidate> = emptyList(),
    val addMemberUserId: String = "",
    val pendingMutation: Boolean = false,
)

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TeamDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val teamsRepository: TeamsRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val teamId: String = savedStateHandle.get<String>("teamId")!!

    private val _uiState = MutableStateFlow(TeamDetailUiState())
    val uiState: StateFlow<TeamDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val effects: SharedFlow<String> = _effects.asSharedFlow()

    private val inviteSearchInput = MutableStateFlow("")

    init {
        viewModelScope.launch {
            inviteSearchInput
                .debounce(400L)
                .distinctUntilChanged()
                .flatMapLatest { q ->
                    flow {
                        if (q.length < 2) {
                            emit(emptyList())
                        } else {
                            val r = teamsRepository.inviteSearch(teamId, q, limit = 20)
                            emit(r.getOrElse { emptyList() })
                        }
                    }
                }
                .collect { candidates ->
                    _uiState.update { it.copy(inviteCandidates = candidates) }
                }
        }
        load()
    }

    fun onInviteSearchQueryChange(query: String) {
        _uiState.update { it.copy(inviteSearchQuery = query) }
        inviteSearchInput.value = query
    }

    fun onAddMemberUserIdChange(value: String) {
        _uiState.update { it.copy(addMemberUserId = value) }
    }

    fun applyCandidateUserId(candidate: InviteCandidate) {
        _uiState.update { it.copy(addMemberUserId = candidate.userId) }
    }

    fun isCurrentUser(userId: String): Boolean {
        val auth = sessionRepository.sessionState.value as? SessionState.Authenticated
        return auth?.userId != null && auth.userId == userId
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true, screenError = null)
            }
            val (teamResult, membersResult) = coroutineScope {
                val t = async { teamsRepository.getTeam(teamId) }
                val m = async { teamsRepository.listMembers(teamId) }
                Pair(t.await(), m.await())
            }
            val team = teamResult.getOrNull()
            val members = membersResult.getOrNull()
            val err = when {
                teamResult.isFailure && membersResult.isFailure ->
                    teamResult.exceptionOrNull()?.message
                        ?: membersResult.exceptionOrNull()?.message
                teamResult.isFailure -> teamResult.exceptionOrNull()?.message
                membersResult.isFailure -> membersResult.exceptionOrNull()?.message
                else -> null
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    team = team,
                    members = members ?: emptyList(),
                    screenError = err,
                )
            }
        }
    }

    fun patchTeamName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _effects.tryEmit("Enter a name")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(pendingMutation = true) }
            teamsRepository.patchTeam(teamId, trimmed).fold(
                onSuccess = { team ->
                    _uiState.update { it.copy(team = team, pendingMutation = false) }
                    _effects.tryEmit("Team updated")
                },
                onFailure = { e ->
                    _uiState.update { it.copy(pendingMutation = false) }
                    _effects.tryEmit(e.message ?: "Could not update team")
                },
            )
        }
    }

    fun addMember() {
        val raw = _uiState.value.addMemberUserId.trim()
        if (raw.isBlank()) {
            _effects.tryEmit("Enter user id")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(pendingMutation = true) }
            teamsRepository.addMember(teamId, raw, role = TeamMemberRole.MEMBER).fold(
                onSuccess = {
                    _effects.tryEmit("Member added")
                    _uiState.update { it.copy(addMemberUserId = "", pendingMutation = false) }
                    reloadMembers()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(pendingMutation = false) }
                    _effects.tryEmit(e.message ?: "Could not add member")
                },
            )
        }
    }

    fun changeMemberRole(userId: String, role: TeamMemberRole) {
        viewModelScope.launch {
            _uiState.update { it.copy(pendingMutation = true) }
            teamsRepository.patchMemberRole(teamId, userId, role).fold(
                onSuccess = {
                    _effects.tryEmit("Role updated")
                    _uiState.update { it.copy(pendingMutation = false) }
                    reloadMembers()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(pendingMutation = false) }
                    _effects.tryEmit(e.message ?: "Could not change role")
                },
            )
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(pendingMutation = true) }
            teamsRepository.removeMember(teamId, userId).fold(
                onSuccess = {
                    _effects.tryEmit("Member removed")
                    _uiState.update { it.copy(pendingMutation = false) }
                    reloadMembers()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(pendingMutation = false) }
                    _effects.tryEmit(e.message ?: "Could not remove member")
                },
            )
        }
    }

    private suspend fun reloadMembers() {
        teamsRepository.listMembers(teamId).fold(
            onSuccess = { list -> _uiState.update { it.copy(members = list) } },
            onFailure = { e ->
                _effects.tryEmit(e.message ?: "Could not reload members")
            },
        )
    }
}
