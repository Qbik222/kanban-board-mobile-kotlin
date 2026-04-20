package com.kanban.mobile.feature.teams

import androidx.lifecycle.SavedStateHandle
import com.kanban.mobile.core.session.SessionRepository
import com.kanban.mobile.core.session.SessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeamDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadSuccess_populatesTeamAndMembers() = runTest(dispatcher) {
        val repo = mockk<TeamsRepository>()
        coEvery { repo.getTeam("t1") } returns Result.success(Team("t1", "My team"))
        coEvery { repo.listMembers("t1") } returns Result.success(
            listOf(
                TeamMember("u1", "a@b.co", "Ann", TeamMemberRole.ADMIN),
            ),
        )
        coEvery { repo.inviteSearch(any(), any(), any()) } returns Result.success(emptyList())
        val session = mockSession()
        val vm = TeamDetailViewModel(
            SavedStateHandle(mapOf("teamId" to "t1")),
            repo,
            session,
        )
        advanceUntilIdle()
        assertEquals("My team", vm.uiState.value.team?.name)
        assertEquals(1, vm.uiState.value.members.size)
        assertEquals("Ann", vm.uiState.value.members.first().name)
    }

    @Test
    fun inviteSearchAfterDebounce_usesLastQueryOnly() = runTest(dispatcher) {
        val repo = mockk<TeamsRepository>()
        coEvery { repo.getTeam("t1") } returns Result.success(Team("t1", "N"))
        coEvery { repo.listMembers("t1") } returns Result.success(emptyList())
        coEvery { repo.inviteSearch("t1", any(), 20) } returns Result.success(emptyList())
        val session = mockSession()
        val vm = TeamDetailViewModel(
            SavedStateHandle(mapOf("teamId" to "t1")),
            repo,
            session,
        )
        advanceUntilIdle()
        vm.onInviteSearchQueryChange("jo")
        advanceTimeBy(150)
        vm.onInviteSearchQueryChange("joe")
        advanceTimeBy(450)
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.inviteSearch("t1", "joe", 20) }
    }

    private fun mockSession(): SessionRepository {
        val session = mockk<SessionRepository>()
        every { session.sessionState } returns MutableStateFlow(
            SessionState.Authenticated(userId = "u1", email = "me@x.co"),
        )
        every { session.accessTokenFlow } returns flowOf(null)
        return session
    }
}
