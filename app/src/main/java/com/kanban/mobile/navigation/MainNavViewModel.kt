package com.kanban.mobile.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanban.mobile.core.session.SessionRepository
import com.kanban.mobile.core.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainNavViewModel @Inject constructor(
    sessionRepository: SessionRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = sessionRepository.sessionState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SessionState.Unknown,
    )

    val sessionInvalidated = sessionRepository.sessionInvalidatedEvents
}
