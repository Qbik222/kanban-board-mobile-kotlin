package com.kanban.mobile.feature.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed interface SplashDestination {
    data object Main : SplashDestination
    data object Login : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    suspend fun bootstrapDestination(): SplashDestination {
        return authRepository.bootstrapSession().fold(
            onSuccess = { SplashDestination.Main },
            onFailure = { SplashDestination.Login },
        )
    }
}
